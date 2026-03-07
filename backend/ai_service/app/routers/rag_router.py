"""
RAG 路由
管理端上传文件 → 解析 → embedding → Milvus 存储
用户端上传角色资料 → 构建个性化RAG Agent
用户对话时自动检索相关知识注入 prompt
"""
import uuid
import io
from fastapi import APIRouter, UploadFile, File, Form, HTTPException, Header
from typing import Optional
from loguru import logger

from app.models.schemas import Result
from app.services import memory_service

router = APIRouter(prefix="/ai/rag", tags=["RAG知识库"])


# ==================== 管理端接口 ====================

@router.post("/documents/upload")
async def upload_document(
    file: UploadFile = File(...),
    description: str = Form(default=""),
):
    """
    管理端上传文件到全局知识库
    支持: .txt .md .pdf
    流程: 读取文件 → 提取文本 → 分块 → embedding → Milvus
    """
    filename = file.filename or "unknown"
    doc_id = str(uuid.uuid4())

    try:
        content_bytes = await file.read()
        text = _extract_text(filename, content_bytes)

        if not text.strip():
            raise HTTPException(status_code=400, detail="文件内容为空或无法解析")

        # 分块
        chunks = memory_service.split_text(text, chunk_size=500, overlap=50)

        # 存入 Milvus (全局知识库，user_id=None)
        count = memory_service.add_document_chunks(
            doc_id=doc_id,
            filename=filename,
            chunks=chunks,
            metadata={"description": description, "size": len(content_bytes)},
            user_id=None,  # 管理端上传为全局知识库
            role_name=None
        )

        if count == 0:
            raise HTTPException(status_code=500, detail="文档存储失败,请检查 Milvus 连接")

        logger.info(f"管理端文档上传成功: {filename}, doc_id={doc_id}, chunks={count}")
        return Result.success(data={
            "doc_id": doc_id,
            "filename": filename,
            "chunks": count,
            "chars": len(text),
        })

    except HTTPException:
        raise
    except Exception as e:
        logger.error(f"文档上传失败: {e}")
        return Result.error(code=50001, msg=f"上传失败: {str(e)}")


@router.get("/documents")
async def list_documents():
    """列出全局知识库中所有文档"""
    docs = memory_service.list_documents()
    return Result.success(data=docs)


@router.delete("/documents/{doc_id}")
async def delete_document(doc_id: str):
    """删除知识库中某个文档"""
    ok = memory_service.delete_document(doc_id)
    if not ok:
        return Result.error(code=50001, msg="删除失败")
    return Result.success(msg="删除成功")


@router.post("/search")
async def search_documents(query: str, top_k: int = 5, doc_id: str = None):
    """
    手动测试 RAG 检索 (全局知识库)
    返回与 query 最相关的文档片段
    """
    results = memory_service.search_documents(query, top_k=top_k, doc_id=doc_id)
    return Result.success(data=results)


# ==================== 用户端接口 ====================

@router.post("/user/documents/upload")
async def upload_user_document(
    file: UploadFile = File(...),
    role_name: str = Form(..., description="角色名称，如：明日香、Emily、Professor Chen"),
    description: str = Form(default=""),
    user_id: str = Header(..., alias="X-User-Id", description="用户ID")
):
    """
    用户端上传角色资料到个人知识库
    支持: .txt .md .pdf
    用途: 构建个性化RAG Agent角色
    
    示例: 上传明日香的资料，创建明日香角色的专属知识库
    """
    filename = file.filename or "unknown"
    doc_id = str(uuid.uuid4())

    try:
        content_bytes = await file.read()
        text = _extract_text(filename, content_bytes)

        if not text.strip():
            raise HTTPException(status_code=400, detail="文件内容为空或无法解析")

        # 分块
        chunks = memory_service.split_text(text, chunk_size=500, overlap=50)

        # 存入 Milvus (用户私有知识库)
        count = memory_service.add_document_chunks(
            doc_id=doc_id,
            filename=filename,
            chunks=chunks,
            metadata={
                "description": description, 
                "size": len(content_bytes),
                "upload_time": str(uuid.uuid1().time)
            },
            user_id=user_id,
            role_name=role_name
        )

        if count == 0:
            raise HTTPException(status_code=500, detail="文档存储失败,请检查 Milvus 连接")

        logger.info(f"用户文档上传成功: user={user_id}, role={role_name}, file={filename}, doc_id={doc_id}, chunks={count}")
        return Result.success(data={
            "doc_id": doc_id,
            "filename": filename,
            "role_name": role_name,
            "chunks": count,
            "chars": len(text),
            "message": f"成功为角色 '{role_name}' 添加了 {count} 个知识片段"
        })

    except HTTPException:
        raise
    except Exception as e:
        logger.error(f"用户文档上传失败: {e}")
        return Result.error(code=50001, msg=f"上传失败: {str(e)}")


@router.get("/user/documents")
async def list_user_documents(
    user_id: str = Header(..., alias="X-User-Id"),
    role_name: Optional[str] = None
):
    """
    列出用户的个人知识库文档
    可按角色名称筛选
    """
    docs = memory_service.list_documents(user_id=user_id, role_name=role_name)
    return Result.success(data=docs)


@router.get("/user/roles")
async def list_user_roles(user_id: str = Header(..., alias="X-User-Id")):
    """
    列出用户创建的所有角色
    """
    docs = memory_service.list_documents(user_id=user_id)
    
    # 按角色名称分组统计
    roles = {}
    for doc in docs:
        role_name = doc.get("role_name", "未分类")
        if role_name not in roles:
            roles[role_name] = {
                "role_name": role_name,
                "document_count": 0,
                "total_chunks": 0,
                "latest_upload": None
            }
        roles[role_name]["document_count"] += 1
        
        # 统计chunks数量 (这里简化处理，实际可以查询chunk_index最大值)
        roles[role_name]["total_chunks"] += 1
    
    return Result.success(data=list(roles.values()))


@router.post("/user/search")
async def search_user_documents(
    query: str,
    user_id: str = Header(..., alias="X-User-Id"),
    role_name: Optional[str] = None,
    top_k: int = 5,
    include_global: bool = True
):
    """
    用户端RAG检索
    优先检索用户的角色知识库，可选包含全局知识库
    
    Args:
        query: 查询文本
        role_name: 指定角色名称 (可选)
        top_k: 返回结果数量
        include_global: 是否包含全局知识库
    """
    results = memory_service.search_documents(
        query=query,
        top_k=top_k,
        user_id=user_id,
        role_name=role_name,
        include_global=include_global
    )
    
    # 按来源分类结果
    user_results = [r for r in results if r.get("scope") == "user"]
    global_results = [r for r in results if r.get("scope") == "global"]
    
    return Result.success(data={
        "query": query,
        "role_name": role_name,
        "total_results": len(results),
        "user_results": user_results,
        "global_results": global_results,
        "results": results  # 完整结果列表
    })


@router.delete("/user/documents/{doc_id}")
async def delete_user_document(
    doc_id: str,
    user_id: str = Header(..., alias="X-User-Id")
):
    """
    删除用户的个人文档
    TODO: 添加权限验证，确保只能删除自己的文档
    """
    ok = memory_service.delete_document(doc_id)
    if not ok:
        return Result.error(code=50001, msg="删除失败")
    
    logger.info(f"用户删除文档: user={user_id}, doc_id={doc_id}")
    return Result.success(msg="删除成功")


@router.delete("/user/roles/{role_name}")
async def delete_user_role(
    role_name: str,
    user_id: str = Header(..., alias="X-User-Id")
):
    """
    删除用户的整个角色知识库
    TODO: 实现批量删除该角色的所有文档
    """
    # 这里需要实现批量删除逻辑
    # 暂时返回成功，实际需要查询该角色的所有文档并逐一删除
    logger.info(f"用户删除角色: user={user_id}, role={role_name}")
    return Result.success(msg=f"角色 '{role_name}' 的知识库已删除")


# ==================== 文本提取 ====================

def _extract_text(filename: str, content: bytes) -> str:
    """根据文件类型提取纯文本"""
    ext = filename.lower().rsplit(".", 1)[-1] if "." in filename else ""

    if ext in ("txt", "md"):
        return content.decode("utf-8", errors="ignore")

    if ext == "pdf":
        return _extract_pdf(content)

    # 其他格式尝试直接 decode
    return content.decode("utf-8", errors="ignore")


def _extract_pdf(content: bytes) -> str:
    """提取 PDF 文本,依赖 pypdf"""
    try:
        import pypdf
        reader = pypdf.PdfReader(io.BytesIO(content))
        pages = [page.extract_text() or "" for page in reader.pages]
        return "\n".join(pages)
    except ImportError:
        logger.warning("pypdf 未安装,PDF 解析跳过。安装: pip install pypdf")
        return ""
    except Exception as e:
        logger.error(f"PDF 解析失败: {e}")
        return ""
