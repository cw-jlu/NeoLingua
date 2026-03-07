"""
SpeakMaster AI Service 主入口
FastAPI应用，集成LangGraph Agent、Milvus记忆、Redis上下文、工具系统
"""
from fastapi import FastAPI
from fastapi.middleware.cors import CORSMiddleware
from loguru import logger

from app.config import settings
from app.routers import chat_router, memory_router, tool_router, session_router, health_router
from app.routers import rag_router, tts_router, local_inference_router, advanced_features_router
from app.tools.builtin_tools import register_builtin_tools

# 配置日志
logger.add(
    "logs/ai_service_{time}.log",
    rotation="10 MB",
    retention="7 days",
    level="DEBUG" if settings.DEBUG else "INFO",
    encoding="utf-8"
)

# 创建FastAPI应用
app = FastAPI(
    title=settings.APP_NAME,
    description="SpeakMaster AI服务 - 提供AI对话、记忆管理、工具调用等功能",
    version="1.0.0",
    docs_url="/ai/docs",
    redoc_url="/ai/redoc"
)

# CORS中间件
app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

# 注册路由
app.include_router(chat_router.router)
app.include_router(memory_router.router)
app.include_router(tool_router.router)
app.include_router(session_router.router)
app.include_router(health_router.router)
app.include_router(rag_router.router)
app.include_router(tts_router.router)
app.include_router(local_inference_router.router)
app.include_router(advanced_features_router.router)


@app.on_event("startup")
async def startup():
    """应用启动事件"""
    logger.info(f"AI Service 启动中... 端口: {settings.APP_PORT}")

    # 注册内置工具
    register_builtin_tools()

    # 尝试初始化Redis连接
    try:
        from app.services.redis_service import get_redis_client
        client = get_redis_client()
        client.ping()
        logger.info("Redis连接成功")
    except Exception as e:
        logger.warning(f"Redis连接失败（服务仍可启动）: {e}")

    logger.info("AI Service 启动完成")


@app.on_event("shutdown")
async def shutdown():
    """应用关闭事件"""
    logger.info("AI Service 关闭中...")


# 根路径
@app.get("/")
async def root():
    return {"service": "SpeakMaster AI Service", "version": "1.0.0", "status": "running"}


if __name__ == "__main__":
    import uvicorn
    uvicorn.run(
        "app.main:app",
        host=settings.APP_HOST,
        port=settings.APP_PORT,
        reload=settings.DEBUG
    )
