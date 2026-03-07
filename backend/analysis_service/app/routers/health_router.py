"""
健康检查API
"""
from fastapi import APIRouter
from app.models.schemas import Result
from app.services.redis_service import redis_service

router = APIRouter(tags=["健康检查"])


@router.get("/health")
async def health_check():
    """健康检查"""
    redis_ok = await redis_service.health_check()
    status = "UP" if redis_ok else "DEGRADED"
    return Result.success({
        "status": status,
        "components": {
            "redis": "UP" if redis_ok else "DOWN",
            "service": "UP"
        }
    })
