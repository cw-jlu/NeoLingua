"""
记忆管理路由
处理长期记忆的保存、检索、删除
"""
from fastapi import APIRouter
from loguru import logger

from app.models.schemas import Result, MemorySaveRequest, MemoryRetrieveRequest
from app.services import memory_service

router = APIRouter(prefix="/ai/memory", tags=["记忆管理"])


@router.post("/save")
async def save_memory(request: MemorySaveRequest):
    """保存记忆"""
    try:
        success = memory_service.save_memory(
            user_id=request.user_id,
            session_id=request.session_id,
            content=request.content,
            metadata=request.metadata
        )
        if success:
            return Result.success(msg="记忆保存成功")
        return Result.error(code=50002, msg="记忆保存失败（Milvus不可用）")
    except Exception as e:
        logger.error(f"保存记忆失败: {e}")
        return Result.error(code=50002, msg=str(e))


@router.get("/retrieve")
async def retrieve_memory(user_id: str, query: str, top_k: int = 5):
    """检索记忆"""
    try:
        memories = memory_service.retrieve_memory(user_id, query, top_k)
        return Result.success(data=memories)
    except Exception as e:
        logger.error(f"检索记忆失败: {e}")
        return Result.error(code=50002, msg=str(e))


@router.get("/{session_id}")
async def get_session_memories(session_id: str):
    """获取会话记忆"""
    try:
        memories = memory_service.get_session_memories(session_id)
        return Result.success(data=memories)
    except Exception as e:
        logger.error(f"获取会话记忆失败: {e}")
        return Result.error(code=50002, msg=str(e))


@router.delete("/{session_id}")
async def delete_session_memories(session_id: str):
    """删除会话记忆"""
    try:
        success = memory_service.delete_session_memories(session_id)
        if success:
            return Result.success(msg="会话记忆删除成功")
        return Result.error(code=50002, msg="删除失败")
    except Exception as e:
        logger.error(f"删除会话记忆失败: {e}")
        return Result.error(code=50002, msg=str(e))


@router.post("/clear")
async def clear_memories(user_id: str):
    """清空用户所有记忆"""
    try:
        success = memory_service.clear_user_memories(user_id)
        if success:
            return Result.success(msg="记忆清空成功")
        return Result.error(code=50002, msg="清空失败")
    except Exception as e:
        logger.error(f"清空记忆失败: {e}")
        return Result.error(code=50002, msg=str(e))
