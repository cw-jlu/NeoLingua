# SpeakMaster AI Service

AI服务模块，基于Python FastAPI + LangGraph + Milvus + Redis构建。

## 架构定位

AI Service是Agent编排层，负责：
- 构建系统提示词（角色、主题、记忆上下文）
- 管理短期上下文（Redis）和长期记忆（Milvus）
- RAG知识库检索（管理端上传文件 → embedding → Milvus存储）
- 工具调用（时间查询、联网搜索等）
- 反馈生成和发音分析

**模型推理不在本服务执行**，而是通过AI Gateway调用。AI Gateway负责：
- 模型选择（用户指定model_id或路由规则自动选择）
- 模型路由（Ollama / 远程API / 本地千问模型）
- 熔断降级

调用链路：`practice_service → ai_service → ai_gateway → 模型`

## 技术栈

- Python 3.10+
- FastAPI
- LangGraph（Agent编排）
- Milvus 2.3（向量数据库/长期记忆/RAG知识库）
- Redis（短期上下文）
- AI Gateway（模型推理代理）
- 外部Embedding API（Gemini/OpenAI，Ollama降级）
- DuckDuckGo Search（联网搜索工具）

## 新功能：Milvus + RAG + 三层记忆系统

### 三层记忆架构
1. **短期记忆**：Redis存储最近N条对话上下文
2. **中期记忆**：Elasticsearch存储对话摘要（定时任务生成）
3. **长期记忆**：Milvus向量数据库存储用户历史对话embedding

### RAG知识库
- 管理端上传文件（.txt/.md/.pdf）
- 自动分块 → embedding → Milvus存储
- 用户对话时自动检索相关知识注入prompt

### Embedding配置
支持多种embedding提供商：
- **Gemini**：text-embedding-004（768维，推荐）
- **OpenAI**：text-embedding-3-small（1536维）
- **Ollama**：nomic-embed-text（768维，本地降级）
- **Hash降级**：所有API失败时使用SHA256哈希

## API接口

### 对话接口
- `POST /ai/chat` - 普通对话（可指定model_id）
- `POST /ai/chat/stream` - 流式对话（SSE，通过AI Gateway转发）
- `POST /ai/chat/stop` - 停止生成（转发到AI Gateway）
- `POST /ai/feedback` - 生成反馈
- `POST /ai/pronunciation` - 发音分析
- `GET /ai/models` - 获取可用模型列表（从AI Gateway获取）

### 记忆管理
- `POST /ai/memory/save` - 保存记忆
- `GET /ai/memory/retrieve` - 检索记忆
- `GET /ai/memory/{sessionId}` - 获取会话记忆
- `DELETE /ai/memory/{sessionId}` - 删除会话记忆
- `POST /ai/memory/clear` - 清空记忆

### RAG知识库管理
- `POST /ai/rag/documents/upload` - 上传文档到知识库
- `GET /ai/rag/documents` - 列出知识库文档
- `DELETE /ai/rag/documents/{docId}` - 删除文档
- `POST /ai/rag/search` - 测试检索功能

### 工具管理
- `GET /ai/tools` - 获取工具列表
- `POST /ai/tools/register` - 注册新工具
- `DELETE /ai/tools/{toolId}` - 删除工具
- `POST /ai/tools/{toolId}/execute` - 执行工具

### 会话管理
- `POST /ai/sessions` - 创建会话
- `GET /ai/sessions/{id}` - 获取会话
- `PUT /ai/sessions/{id}` - 更新会话
- `DELETE /ai/sessions/{id}` - 删除会话

### 健康检查
- `GET /ai/health` - 健康检查（Redis/Milvus/AI Gateway）
- `GET /ai/models/status` - 模型状态（从AI Gateway获取）

## 配置说明

### 环境变量配置（.env）

```bash
# Embedding 配置
EMBEDDING_PROVIDER=gemini  # gemini | openai | ollama
EMBEDDING_DIM=768

# Gemini embedding (推荐)
GEMINI_API_KEY=your_gemini_api_key_here
GEMINI_EMBEDDING_MODEL=models/text-embedding-004

# OpenAI 兼容 embedding (备用)
OPENAI_API_KEY=your_openai_api_key_here
OPENAI_EMBEDDING_MODEL=text-embedding-3-small
OPENAI_BASE_URL=https://api.openai.com/v1

# Ollama embedding (本地降级)
OLLAMA_BASE_URL=http://localhost:11434
OLLAMA_EMBEDDING_MODEL=nomic-embed-text

# Milvus配置
MILVUS_HOST=localhost
MILVUS_PORT=19530
```

### Milvus Collections

系统自动创建两个collection：
1. `user_memories`：用户对话历史记忆
2. `rag_documents`：管理端上传的知识库文档

## 启动方式

```bash
# 1. 确保依赖服务运行
docker-compose up -d mysql redis milvus

# 2. 配置embedding API密钥
# 编辑 .env 文件，设置 GEMINI_API_KEY 或 OPENAI_API_KEY

# 3. 安装依赖
pip install -r requirements.txt

# 4. 测试Milvus和embedding集成
python test_milvus_embedding.py

# 5. 启动服务（需要先启动AI Gateway）
python -m app.main
# 或
uvicorn app.main:app --host 0.0.0.0 --port 8089 --reload
```

## 端口

- 8089

## 环境依赖

- AI Gateway (端口8088，必须先启动)
- Redis (端口6379)
- Milvus (端口19530，包含etcd和minio)
- 外部Embedding API（Gemini/OpenAI）

## 测试

```bash
# 测试Milvus连接和embedding功能
python test_milvus_embedding.py

# 测试结果示例：
# ✅ Embedding 成功
# ✅ Milvus 连接成功  
# ✅ 用户记忆功能正常
# ✅ RAG文档功能正常
```

## API文档

启动后访问: http://localhost:8089/ai/docs
