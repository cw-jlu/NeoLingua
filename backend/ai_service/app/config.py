"""
应用配置模块
从环境变量加载配置
"""
from pydantic_settings import BaseSettings
from typing import Optional


class Settings(BaseSettings):
    """应用配置"""

    # 服务配置
    APP_NAME: str = "SpeakMaster AI Service"
    APP_HOST: str = "0.0.0.0"
    APP_PORT: int = 8089
    DEBUG: bool = True

    # AI Gateway配置（模型推理通过AI Gateway调用）
    AI_GATEWAY_URL: str = "http://localhost:8088"
    AI_GATEWAY_CHAT_PATH: str = "/api/v1/user/ai/chat"
    AI_GATEWAY_STREAM_PATH: str = "/api/v1/user/ai/chat/stream"
    AI_GATEWAY_STOP_PATH: str = "/api/v1/user/ai/chat/stop"
    AI_GATEWAY_MODELS_PATH: str = "/api/v1/user/ai/models"

    # Redis配置
    REDIS_HOST: str = "localhost"
    REDIS_PORT: int = 6379
    REDIS_PASSWORD: str = ""
    REDIS_DB: int = 8  # AI服务使用database 8

    # Milvus配置
    MILVUS_HOST: str = "localhost"
    MILVUS_PORT: int = 19530
    MILVUS_COLLECTION: str = "ai_memory"

    # Embedding 配置
    # provider: gemini | openai | ollama
    EMBEDDING_PROVIDER: str = "gemini"

    # Gemini embedding
    GEMINI_API_KEY: str = ""
    GEMINI_EMBEDDING_MODEL: str = "models/text-embedding-004"  # 输出768维

    # OpenAI 兼容 embedding (也可接 Azure / 其他兼容接口)
    OPENAI_API_KEY: str = ""
    OPENAI_EMBEDDING_MODEL: str = "text-embedding-3-small"  # 输出1536维
    OPENAI_BASE_URL: str = "https://api.openai.com/v1"

    # Ollama embedding (本地降级)
    OLLAMA_BASE_URL: str = "http://localhost:11434"
    OLLAMA_EMBEDDING_MODEL: str = "nomic-embed-text"  # 输出768维

    # 记忆配置
    SHORT_TERM_TTL: int = 3600  # 短期记忆过期时间(秒)
    MAX_CONTEXT_MESSAGES: int = 20  # 最大上下文消息数
    EMBEDDING_DIM: int = 768  # 向量维度,与所选模型保持一致

    class Config:
        env_file = ".env"
        case_sensitive = True


settings = Settings()
