"""
Analysis Service 配置模块
从环境变量加载配置
"""
from pydantic_settings import BaseSettings


class Settings(BaseSettings):
    """应用配置"""

    # 服务配置
    APP_NAME: str = "SpeakMaster Analysis Service"
    APP_HOST: str = "0.0.0.0"
    APP_PORT: int = 8085
    DEBUG: bool = True

    # 数据库配置
    DB_HOST: str = "localhost"
    DB_PORT: int = 3307
    DB_NAME: str = "speakmaster_analysis"
    DB_USER: str = "root"
    DB_PASSWORD: str = "root123"

    # Redis配置
    REDIS_HOST: str = "localhost"
    REDIS_PORT: int = 6379
    REDIS_PASSWORD: str = ""
    REDIS_DB: int = 5

    # Kafka配置
    KAFKA_BOOTSTRAP_SERVERS: str = "localhost:9092"
    KAFKA_CONSUMER_GROUP: str = "analysis-worker-group"
    KAFKA_TOPIC_DIALOGUE: str = "stream.dialogue.text"
    KAFKA_TOPIC_RESULT: str = "stream.analysis.result"

    # MinIO配置
    MINIO_ENDPOINT: str = "localhost:9000"
    MINIO_ACCESS_KEY: str = "minioadmin"
    MINIO_SECRET_KEY: str = "minioadmin"
    MINIO_BUCKET: str = "speakmaster-audio"
    MINIO_SECURE: bool = False

    # MFA配置（独立conda环境，避免依赖冲突）
    MFA_PYTHON_PATH: str = ""
    MFA_DICTIONARY_PATH: str = ""
    MFA_ACOUSTIC_MODEL_PATH: str = ""

    # 音频本地存储路径
    AUDIO_STORAGE_PATH: str = "./audio_files"

    @property
    def database_url(self) -> str:
        return f"mysql+pymysql://{self.DB_USER}:{self.DB_PASSWORD}@{self.DB_HOST}:{self.DB_PORT}/{self.DB_NAME}?charset=utf8mb4"

    class Config:
        env_file = ".env"
        case_sensitive = True


settings = Settings()
