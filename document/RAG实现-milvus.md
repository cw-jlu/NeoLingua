# RAG实现总结

## 系统概述

SpeakMaster现已完成基于Milvus向量数据库的RAG知识库和三层记忆系统集成，支持外部embedding API调用，实现了智能的上下文感知对话。

## 架构设计

### 三层记忆架构
```
┌─────────────────┐    ┌─────────────────┐    ┌─────────────────┐
│   短期记忆       │    │   中期记忆       │    │   长期记忆       │
│   (Redis)       │    │ (Elasticsearch) │    │   (Milvus)      │
├─────────────────┤    ├─────────────────┤    ├─────────────────┤
│ • 最近N条对话    │    │ • 对话摘要       │    │ • 用户历史对话   │
│ • 会话上下文     │    │ • 定时生成       │    │ • 向量化存储     │
│ • 临时状态      │    │ • 时间范围检索   │    │ • 语义相似检索   │
│ • TTL过期       │    │ • 全文搜索       │    │ • 长期画像       │
└─────────────────┘    └─────────────────┘    └─────────────────┘
```

### RAG知识库架构
```
┌─────────────────┐    ┌─────────────────┐    ┌─────────────────┐
│   文件上         │    │   文本处理       │    │   向量存储       │
│ (管理端界面)     │    │ (AI Service)    │    │   (Milvus)      │
├─────────────────┤    ├─────────────────┤    ├─────────────────┤
│ • .txt/.md/.pdf │    │ • 文本提取       │    │ • 文档分块       │
│ • 拖拽上传       │    │ • 智能分块       │    │ • embedding向量  │
│ • 批量管理       │    │ • 重叠处理       │    │ • 相似度检索     │
│ • 元数据标记     │    │ • 去重优化       │    │ • 多文档融合     │
└─────────────────┘    └─────────────────┘    └─────────────────┘
```

## 🔧 技术实现

### 1. Milvus Collections

系统自动创建两个collection：

#### user_memories (用户记忆)
```python
fields = [
    FieldSchema(name="id", dtype=DataType.INT64, is_primary=True, auto_id=True),
    FieldSchema(name="user_id", dtype=DataType.VARCHAR, max_length=64),
    FieldSchema(name="session_id", dtype=DataType.VARCHAR, max_length=64),
    FieldSchema(name="session_type", dtype=DataType.VARCHAR, max_length=32),
    FieldSchema(name="content", dtype=DataType.VARCHAR, max_length=4096),
    FieldSchema(name="metadata", dtype=DataType.VARCHAR, max_length=1024),
    FieldSchema(name="embedding", dtype=DataType.FLOAT_VECTOR, dim=768),
]
```

#### rag_documents (知识库)
```python
fields = [
    FieldSchema(name="id", dtype=DataType.INT64, is_primary=True, auto_id=True),
    FieldSchema(name="doc_id", dtype=DataType.VARCHAR, max_length=64),
    FieldSchema(name="filename", dtype=DataType.VARCHAR, max_length=256),
    FieldSchema(name="chunk_index", dtype=DataType.INT64),
    FieldSchema(name="content", dtype=DataType.VARCHAR, max_length=4096),
    FieldSchema(name="metadata", dtype=DataType.VARCHAR, max_length=1024),
    FieldSchema(name="embedding", dtype=DataType.FLOAT_VECTOR, dim=768),
]
```

### 2. Embedding提供商支持

#### 多提供商架构
```python
def get_embedding(text: str) -> List[float]:
    provider = settings.EMBEDDING_PROVIDER.lower()
    
    try:
        if provider == "gemini":
            return _embed_gemini(text)      # 768维，推荐
        elif provider == "openai":
            return _embed_openai(text)      # 1536维
        else:
            return _embed_ollama(text)      # 768维，本地
    except Exception as e:
        return _embed_hash(text)            # Hash降级
```

#### 配置示例
```bash
# Gemini (推荐)
EMBEDDING_PROVIDER=gemini
GEMINI_API_KEY=your_gemini_api_key_here
GEMINI_EMBEDDING_MODEL=models/text-embedding-004

# OpenAI (备用)
EMBEDDING_PROVIDER=openai
OPENAI_API_KEY=your_openai_api_key_here
OPENAI_EMBEDDING_MODEL=text-embedding-3-small

# Ollama (本地降级)
EMBEDDING_PROVIDER=ollama
OLLAMA_BASE_URL=http://localhost:11434
OLLAMA_EMBEDDING_MODEL=nomic-embed-text
```

### 3. RAG检索流程

```python
async def chat(session_id: str, user_id: str, message: str, ...):
    # 1. 检索用户历史记忆(长期记忆)
    memories = memory_service.retrieve_memory(user_id, message, top_k=3)
    
    # 2. RAG检索知识库
    rag_docs = memory_service.search_documents(message, top_k=3)
    
    # 3. 获取短期上下文(Redis)
    context_messages = redis_service.get_context_messages(session_id)
    
    # 4. 构建系统提示词(整合三层记忆 + RAG知识)
    system_prompt = _build_system_prompt(
        role_prompt, theme, memories, rag_docs
    )
    
    # 5. 通过AI Gateway调用模型
    # 6. 保存新的对话到记忆系统
```

## 📁文件结构

```
backend/ai_service/
├── app/
│   ├── services/
│   │   ├── memory_service.py      # Milvus向量存储服务
│   │   ├── redis_service.py       # Redis短期记忆
│   │   └── agent_service.py       # Agent编排(集成三层记忆)
│   ├── routers/
│   │   └── rag_router.py          # RAG知识库API
│   ├── config.py                  # 配置管理
│   └── main.py                    # FastAPI应用
├── test_milvus_embedding.py       # 集成测试脚本
├── requirements.txt               # Python依赖
├── .env                          # 环境变量配置
└── README.md                     # 服务文档

frontend/admin_web/src/
├── views/ai/
│   └── KnowledgeBase.vue         # 知识库管理界面
├── api/
│   └── rag.js                    # RAG API调用
└── router/index.js               # 路由配置(已添加知识库页面)
```

## 🌐 API接口

### RAG知识库管理
```http
POST /ai/rag/documents/upload     # 上传文档
GET  /ai/rag/documents            # 列出文档
DELETE /ai/rag/documents/{docId}  # 删除文档
POST /ai/rag/search               # 测试检索
```
### 记忆管理
```http
POST /ai/memory/save              # 保存记忆
GET  /ai/memory/retrieve          # 检索记忆
DELETE /ai/memory/clear           # 清空记忆
```
### 对话接口(集成RAG+记忆)
```http
POST /ai/chat                     # 普通对话
POST /ai/chat/stream              # 流式对话
POST /ai/chat/stop                # 停止生成
```
## 使用场景
### 1. 管理端知识库管理
- 上传英语学习资料(.txt/.md/.pdf)
- 自动分块和向量化存储
- 文档列表管理和删除
- 搜索测试功能
### 2. 用户对话增强
- 自动检索相关知识库内容
- 注入用户历史学习记忆
- 提供个性化学习建议
- 上下文感知的智能回复
### 3. 英语学习优化
- 基于用户历史的个性化纠错
- 相关语法知识自动补充
- 学习进度和弱点分析
- 智能练习推荐
## 部署启动

### 1. 启动依赖服务
```bash
docker-compose up -d mysql redis milvus
```

### 2. 配置embedding API
```bash
# 编辑 backend/ai_service/.env
EMBEDDING_PROVIDER=gemini
GEMINI_API_KEY=your_api_key_here
```

### 3. 启动AI服务
```bash
cd backend/ai_service
python -m venv venv_ai_service
venv_ai_service\Scripts\activate
pip install -r requirements.txt
python test_milvus_embedding.py  # 测试
uvicorn app.main:app --host 0.0.0.0 --port 8089 --reload
```

### 4. 访问管理界面
- API文档: http://localhost:8089/ai/docs
- 知识库管理: http://localhost:5173/ai/knowledge

## 性能优化方向

### 1. 向量检索优化
- 使用COS相似度计算
- IVF_FLAT索引加速检索
- 合理设置top_k参数

### 2. 分块策略优化
- 默认500字符分块，50字符重叠
- 根据文档类型调整分块大小
- 保持语义完整性

### 3. 缓存策略
- Redis缓存频繁查询结果
- Embedding结果本地缓存
- 分层缓存减少API调用

## 安全考虑

### 1. API密钥管理
- 环境变量存储敏感信息
- 支持多提供商降级
- 本地hash作为最终降级

### 2. 数据隔离
- 用户记忆按user_id隔离
- 文档权限控制
- 敏感信息过滤

### 3. 访问控制
- 管理端权限验证
- API接口鉴权
- 文件上传安全检查

## 功能亮点

1. **智能记忆系统**: 三层记忆架构，短中长期记忆有机结合
2. **RAG知识库**: 管理端上传文档，用户对话自动检索相关知识
3. **多提供商支持**: Gemini/OpenAI/Ollama多种embedding API，自动降级
4. **无缝集成**: 与现有对话系统完美融合，透明增强用户体验
5. **可视化管理**: 管理端界面友好，支持拖拽上传和批量管理
6. **高性能检索**: Milvus向量数据库，毫秒级相似度检索
7. **容错设计**: 多级降级策略，确保服务稳定性

## 未来扩展

1. **多模态支持**: 图片、音频文档的向量化存储
2. **知识图谱**: 结合Neo4j构建概念关系网络
3. **个性化推荐**: 基于用户画像的学习内容推荐
4. **实时更新**: 支持知识库的增量更新和版本管理
5. **分布式部署**: Milvus集群部署，支持海量数据

---

## 问题
1. **Q**: 概述一下父子索引。有在RAG流程中引入父子索引吗（Parent-Document Retrieval）？为什么？怎么做？固定长度切分会造成什么问题，有什么解决方法。如果按照语义切分，一篇文章语义非常相近怎么切分。一如果按照段落切分，一个段落上下两部分意思相差很大怎么办
2. **Q**: 有引入BM25吗？为什么？向量检索和BM25的融合比例是怎么样的？为什么这个权重？
3. **Q**: 检索融合的具体流程是什么？召回后有没有做Rerank？
4. **Q**: Rerank后返回几个快（Chunk）？有没有针对这个数量做过验证？
5. **Q**: Rerank后的TopK截断是怎么做的？为什么是这个值？有没有其他截断方案？
6. **Q**: 上下文工程（Context Engineering），Agent的Memory是怎么做的？
7. **Q**:怎么处理内容非常乱的word,pdf?
8. **Q**:构建向量的维度是多少，为什么这个维度
9. **Q**:Cross Encoder是什么，怎Zrerank
10. **Q**:查询重写模块怎么做的
11. **Q**:怎么对rag评估，测试集怎么做的，如果召回相关度极差怎么做,
**文档最后修改时间**: 2026-03-05  
**技术栈**: Python + FastAPI + Milvus + Redis + Vue.js