"""
记忆服务 - Milvus 向量存储
两个 Collection:
  - user_memories: 用户对话历史记忆(长期记忆)
  - rag_documents: 管理端上传文件的知识库(RAG)
"""
import json
import hashlib
from typing import Optional, List, Dict, Any
from loguru import logger

from app.config import settings

# 延迟初始化
_milvus_ready = False
_user_memories_col = None
_rag_documents_col = None


# ==================== Milvus 初始化 ====================

def _init_milvus():
    global _milvus_ready, _user_memories_col, _rag_documents_col
    if _milvus_ready:
        return

    try:
        from pymilvus import connections, Collection, FieldSchema, CollectionSchema, DataType, utility

        connections.connect(alias="default", host=settings.MILVUS_HOST, port=settings.MILVUS_PORT)
        logger.info(f"Milvus连接成功: {settings.MILVUS_HOST}:{settings.MILVUS_PORT}")

        _user_memories_col = _ensure_collection(
            name="user_memories",
            description="用户对话历史记忆",
            extra_fields=[
                FieldSchema(name="user_id", dtype=DataType.VARCHAR, max_length=64),
                FieldSchema(name="session_id", dtype=DataType.VARCHAR, max_length=64),
                FieldSchema(name="session_type", dtype=DataType.VARCHAR, max_length=32),
                FieldSchema(name="content", dtype=DataType.VARCHAR, max_length=4096),
                FieldSchema(name="metadata", dtype=DataType.VARCHAR, max_length=1024),
            ]
        )

        _rag_documents_col = _ensure_collection(
            name="rag_documents",
            description="管理端上传的知识库文档",
            extra_fields=[
                FieldSchema(name="doc_id", dtype=DataType.VARCHAR, max_length=64),
                FieldSchema(name="filename", dtype=DataType.VARCHAR, max_length=256),
                FieldSchema(name="chunk_index", dtype=DataType.INT64),
                FieldSchema(name="content", dtype=DataType.VARCHAR, max_length=4096),
                FieldSchema(name="metadata", dtype=DataType.VARCHAR, max_length=1024),
            ]
        )

        _milvus_ready = True
        logger.info("Milvus collections 初始化完成: user_memories, rag_documents")

    except Exception as e:
        logger.warning(f"Milvus初始化失败(服务降级运行): {e}")
        _milvus_ready = False


def _ensure_collection(name: str, description: str, extra_fields: list):
    """确保 collection 存在,不存在则创建"""
    from pymilvus import Collection, FieldSchema, CollectionSchema, DataType, utility

    if utility.has_collection(name):
        col = Collection(name=name)
        col.load()
        return col

    fields = [
        FieldSchema(name="id", dtype=DataType.INT64, is_primary=True, auto_id=True),
        *extra_fields,
        FieldSchema(name="embedding", dtype=DataType.FLOAT_VECTOR, dim=settings.EMBEDDING_DIM),
    ]
    schema = CollectionSchema(fields=fields, description=description)
    col = Collection(name=name, schema=schema)
    col.create_index(
        field_name="embedding",
        index_params={"metric_type": "COSINE", "index_type": "IVF_FLAT", "params": {"nlist": 128}}
    )
    col.load()
    logger.info(f"Collection 创建成功: {name}")
    return col


# ==================== Embedding ====================

def get_embedding(text: str) -> List[float]:
    """
    调用 embedding API 获取向量
    根据 EMBEDDING_PROVIDER 选择: gemini | openai | ollama
    所有 provider 失败后 hash 降级
    """
    provider = settings.EMBEDDING_PROVIDER.lower()

    try:
        if provider == "gemini":
            return _embed_gemini(text)
        elif provider == "openai":
            return _embed_openai(text)
        else:
            return _embed_ollama(text)

    except Exception as e:
        logger.warning(f"Embedding 调用失败,使用 hash 降级: {e}")

    return _embed_hash(text)


def _embed_gemini(text: str) -> List[float]:
    """Gemini text-embedding-004, 输出 768 维"""
    import httpx
    
    if not settings.GEMINI_API_KEY:
        raise ValueError("GEMINI_API_KEY 未配置")
    
    url = (
        f"https://generativelanguage.googleapis.com/v1beta/"
        f"{settings.GEMINI_EMBEDDING_MODEL}:embedContent"
        f"?key={settings.GEMINI_API_KEY}"
    )
    
    with httpx.Client(timeout=30.0) as client:
        resp = client.post(
            url,
            json={"model": settings.GEMINI_EMBEDDING_MODEL, "content": {"parts": [{"text": text}]}},
        )
        resp.raise_for_status()
        embedding = resp.json()["embedding"]["values"]
        return _normalize_dim(embedding)


def _embed_openai(text: str) -> List[float]:
    """OpenAI 兼容接口 /v1/embeddings"""
    import httpx
    
    if not settings.OPENAI_API_KEY:
        raise ValueError("OPENAI_API_KEY 未配置")
    
    with httpx.Client(timeout=30.0) as client:
        resp = client.post(
            f"{settings.OPENAI_BASE_URL}/embeddings",
            headers={"Authorization": f"Bearer {settings.OPENAI_API_KEY}"},
            json={"model": settings.OPENAI_EMBEDDING_MODEL, "input": text},
        )
        resp.raise_for_status()
        embedding = resp.json()["data"][0]["embedding"]
        return _normalize_dim(embedding)


def _embed_ollama(text: str) -> List[float]:
    """Ollama 本地 embedding"""
    import httpx
    
    with httpx.Client(timeout=30.0) as client:
        resp = client.post(
            f"{settings.OLLAMA_BASE_URL}/api/embeddings",
            json={"model": settings.OLLAMA_EMBEDDING_MODEL, "prompt": text},
        )
        resp.raise_for_status()
        embedding = resp.json().get("embedding", [])
        return _normalize_dim(embedding)


def _normalize_dim(embedding: List[float]) -> List[float]:
    """截断或补零,确保维度与 EMBEDDING_DIM 一致"""
    dim = settings.EMBEDDING_DIM
    if len(embedding) >= dim:
        return embedding[:dim]
    return embedding + [0.0] * (dim - len(embedding))


def _embed_hash(text: str) -> List[float]:
    """hash 降级方案,无需外部依赖"""
    hash_bytes = hashlib.sha256(text.encode()).digest()
    vector = []
    for i in range(settings.EMBEDDING_DIM):
        byte_idx = i % len(hash_bytes)
        vector.append((hash_bytes[byte_idx] - 128) / 128.0)
    return vector


# ==================== 用户对话记忆 ====================

def save_memory(user_id: str, session_id: str, content: str,
                session_type: str = "chat",
                metadata: Optional[Dict[str, Any]] = None) -> bool:
    """保存用户对话记忆到 Milvus"""
    _init_milvus()
    if _user_memories_col is None:
        return False

    try:
        embedding = get_embedding(content)
        meta_str = json.dumps(metadata or {}, ensure_ascii=False)[:1024]

        _user_memories_col.insert([
            [user_id],
            [session_id],
            [session_type],
            [content[:4096]],
            [meta_str],
            [embedding],
        ])
        _user_memories_col.flush()
        logger.debug(f"记忆保存成功: user={user_id}, session={session_id}")
        return True
    except Exception as e:
        logger.error(f"保存记忆失败: {e}")
        return False


def retrieve_memory(user_id: str, query: str, top_k: int = 5,
                    session_type: Optional[str] = None) -> List[Dict[str, Any]]:
    """从 Milvus 检索与 query 最相关的用户历史记忆"""
    _init_milvus()
    if _user_memories_col is None:
        return []

    try:
        query_embedding = get_embedding(query)
        expr = f'user_id == "{user_id}"'
        if session_type:
            expr += f' && session_type == "{session_type}"'

        results = _user_memories_col.search(
            data=[query_embedding],
            anns_field="embedding",
            param={"metric_type": "COSINE", "params": {"nprobe": 10}},
            limit=top_k,
            expr=expr,
            output_fields=["content", "session_id", "session_type", "metadata"]
        )

        memories = []
        for hits in results:
            for hit in hits:
                memories.append({
                    "content": hit.entity.get("content"),
                    "session_id": hit.entity.get("session_id"),
                    "session_type": hit.entity.get("session_type"),
                    "metadata": json.loads(hit.entity.get("metadata", "{}")),
                    "score": hit.score,
                })
        return memories
    except Exception as e:
        logger.error(f"检索记忆失败: {e}")
        return []


def delete_user_memories(user_id: str) -> bool:
    """清空用户所有记忆"""
    _init_milvus()
    if _user_memories_col is None:
        return False
    try:
        _user_memories_col.delete(expr=f'user_id == "{user_id}"')
        logger.info(f"用户记忆清空: user_id={user_id}")
        return True
    except Exception as e:
        logger.error(f"清空记忆失败: {e}")
        return False


# ==================== RAG 知识库 ====================

def add_document_chunks(doc_id: str, filename: str,
                        chunks: List[str],
                        metadata: Optional[Dict[str, Any]] = None,
                        user_id: Optional[str] = None,
                        role_name: Optional[str] = None) -> int:
    """
    将文档分块后 embedding 并存入 Milvus
    返回成功存储的 chunk 数量
    
    Args:
        doc_id: 文档ID
        filename: 文件名
        chunks: 文档分块列表
        metadata: 元数据
        user_id: 用户ID (用户上传时必填，管理端上传时为None表示全局知识库)
        role_name: 角色名称 (用户可以为特定角色上传资料)
    """
    _init_milvus()
    if _rag_documents_col is None:
        return 0

    # 扩展元数据
    extended_metadata = metadata or {}
    if user_id:
        extended_metadata["user_id"] = user_id
        extended_metadata["scope"] = "user"  # 用户私有
    else:
        extended_metadata["scope"] = "global"  # 全局知识库
    
    if role_name:
        extended_metadata["role_name"] = role_name
    
    meta_str = json.dumps(extended_metadata, ensure_ascii=False)[:1024]
    success = 0

    try:
        # 批量 embedding
        embeddings = [get_embedding(chunk) for chunk in chunks]

        _rag_documents_col.insert([
            [doc_id] * len(chunks),
            [filename] * len(chunks),
            list(range(len(chunks))),          # chunk_index
            [c[:4096] for c in chunks],
            [meta_str] * len(chunks),
            embeddings,
        ])
        _rag_documents_col.flush()
        success = len(chunks)
        logger.info(f"文档入库成功: doc_id={doc_id}, filename={filename}, chunks={success}, user_id={user_id}, role={role_name}")
    except Exception as e:
        logger.error(f"文档入库失败: doc_id={doc_id}, error={e}")

    return success


def search_documents(query: str, top_k: int = 5,
                     doc_id: Optional[str] = None,
                     user_id: Optional[str] = None,
                     role_name: Optional[str] = None,
                     include_global: bool = True) -> List[Dict[str, Any]]:
    """
    RAG 检索: 根据 query 向量搜索最相关的文档片段
    
    Args:
        query: 查询文本
        top_k: 返回结果数量
        doc_id: 指定文档ID (可选)
        user_id: 用户ID (可选，用于检索用户私有知识库)
        role_name: 角色名称 (可选，用于检索特定角色的知识库)
        include_global: 是否包含全局知识库 (默认True)
    """
    _init_milvus()
    if _rag_documents_col is None:
        return []

    try:
        query_embedding = get_embedding(query)
        
        # 构建检索表达式
        expr_parts = []
        
        if doc_id:
            expr_parts.append(f'doc_id == "{doc_id}"')
        else:
            # 构建范围检索表达式
            scope_parts = []
            
            if include_global:
                scope_parts.append('metadata like "%\\"scope\\": \\"global\\"%"')
            
            if user_id:
                scope_parts.append(f'metadata like "%\\"user_id\\": \\"{user_id}\\"%"')
            
            if role_name:
                scope_parts.append(f'metadata like "%\\"role_name\\": \\"{role_name}\\"%"')
            
            if scope_parts:
                expr_parts.append(f"({' || '.join(scope_parts)})")
        
        expr = ' && '.join(expr_parts) if expr_parts else None

        results = _rag_documents_col.search(
            data=[query_embedding],
            anns_field="embedding",
            param={"metric_type": "COSINE", "params": {"nprobe": 10}},
            limit=top_k,
            expr=expr,
            output_fields=["doc_id", "filename", "chunk_index", "content", "metadata"]
        )

        docs = []
        for hits in results:
            for hit in hits:
                metadata = json.loads(hit.entity.get("metadata", "{}"))
                docs.append({
                    "doc_id": hit.entity.get("doc_id"),
                    "filename": hit.entity.get("filename"),
                    "chunk_index": hit.entity.get("chunk_index"),
                    "content": hit.entity.get("content"),
                    "metadata": metadata,
                    "score": hit.score,
                    "user_id": metadata.get("user_id"),
                    "role_name": metadata.get("role_name"),
                    "scope": metadata.get("scope", "global"),
                })
        return docs
    except Exception as e:
        logger.error(f"RAG 检索失败: {e}")
        return []


def delete_document(doc_id: str) -> bool:
    """删除知识库中某个文档的所有 chunk"""
    _init_milvus()
    if _rag_documents_col is None:
        return False
    try:
        _rag_documents_col.delete(expr=f'doc_id == "{doc_id}"')
        logger.info(f"文档删除成功: doc_id={doc_id}")
        return True
    except Exception as e:
        logger.error(f"删除文档失败: {e}")
        return False


def list_documents(user_id: Optional[str] = None, 
                   role_name: Optional[str] = None) -> List[Dict[str, Any]]:
    """
    列出知识库中的文档(按 doc_id 去重)
    
    Args:
        user_id: 用户ID (可选，用于列出用户私有文档)
        role_name: 角色名称 (可选，用于列出特定角色的文档)
    """
    _init_milvus()
    if _rag_documents_col is None:
        return []
    try:
        # 构建查询表达式
        expr_parts = ["chunk_index == 0"]  # 只取每个文档的第一个 chunk
        
        if user_id:
            expr_parts.append(f'metadata like "%\\"user_id\\": \\"{user_id}\\"%"')
        
        if role_name:
            expr_parts.append(f'metadata like "%\\"role_name\\": \\"{role_name}\\"%"')
        
        expr = ' && '.join(expr_parts)
        
        results = _rag_documents_col.query(
            expr=expr,
            output_fields=["doc_id", "filename", "metadata"]
        )
        
        docs = []
        for r in results:
            metadata = json.loads(r.get("metadata", "{}"))
            docs.append({
                "doc_id": r.get("doc_id"),
                "filename": r.get("filename"),
                "metadata": metadata,
                "user_id": metadata.get("user_id"),
                "role_name": metadata.get("role_name"),
                "scope": metadata.get("scope", "global"),
            })
        return docs
    except Exception as e:
        logger.error(f"列出文档失败: {e}")
        return []


# ==================== 文本分块工具 ====================

def split_text(text: str, chunk_size: int = 500, overlap: int = 50) -> List[str]:
    """
    简单的滑动窗口分块
    chunk_size: 每块字符数
    overlap: 相邻块重叠字符数
    """
    if len(text) <= chunk_size:
        return [text]

    chunks = []
    start = 0
    while start < len(text):
        end = start + chunk_size
        chunks.append(text[start:end])
        start += chunk_size - overlap

    return chunks
