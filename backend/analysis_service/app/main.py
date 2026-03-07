"""
Analysis Service 入口
端口: 8085
提供学习数据分析、排名、报表导出等功能
Kafka Worker独立进程运行: python -m app.worker
"""
import logging
from contextlib import asynccontextmanager
from fastapi import FastAPI
from app.config import settings
from app.database import engine, Base
from app.services.redis_service import redis_service
from app.services.audio_storage import audio_storage
from app.routers import user_router, admin_router, health_router

logging.basicConfig(
    level=logging.DEBUG if settings.DEBUG else logging.INFO,
    format="%(asctime)s [%(levelname)s] %(name)s - %(message)s"
)
logger = logging.getLogger(__name__)


@asynccontextmanager
async def lifespan(app: FastAPI):
    """应用生命周期"""
    # 启动
    logger.info(f"{settings.APP_NAME} 启动中...")
    Base.metadata.create_all(bind=engine)
    await redis_service.connect()
    audio_storage.connect_minio()
    logger.info(f"{settings.APP_NAME} 启动完成，端口: {settings.APP_PORT}")
    yield
    # 关闭
    await redis_service.disconnect()
    logger.info(f"{settings.APP_NAME} 已关闭")


app = FastAPI(
    title=settings.APP_NAME,
    description="SpeakMaster 分析服务 - 学习数据分析、排名、报表",
    version="1.0.0",
    lifespan=lifespan
)

# 注册路由
app.include_router(user_router.router)
app.include_router(admin_router.router)
app.include_router(health_router.router)


@app.get("/")
async def root():
    return {"service": settings.APP_NAME, "port": settings.APP_PORT}


if __name__ == "__main__":
    import uvicorn
    uvicorn.run(
        "app.main:app",
        host=settings.APP_HOST,
        port=settings.APP_PORT,
        reload=settings.DEBUG
    )
