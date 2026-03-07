"""
会话管理路由
处理会话的创建、查询、更新、删除
"""
import uuid
from datetime import datetime
from fastapi import APIRouter
from loguru import logger

from app.models.schemas import Result, SessionCreateRequest
from app.services import redis_service

router = APIRouter(prefix="/ai/sessions", tags=["会话管理"])


@router.post("")
async def create_session(request: SessionCreateRequest):
    """创建会话"""
    try:
        session_id = str(uuid.uuid4())
        session_info = {
            "session_id": session_id,
            "user_id": request.user_id,
            "theme": request.theme,
            "role_prompt": request.role_prompt,
            "created_at": datetime.now().isoformat(),
            "message_count": 0
        }
        redis_service.save_session_info(session_id, session_info)
        return Result.success(data=session_info)
    except Exception as e:
        logger.error(f"创建会话失败: {e}")
        return Result.error(code=50004, msg=str(e))


@router.get("/{session_id}")
async def get_session(session_id: str):
    """获取会话信息"""
    try:
        info = redis_service.get_session_info(session_id)
        if info:
            # 补充消息数量
            messages = redis_service.get_context_messages(session_id)
            info["message_count"] = len(messages)
            return Result.success(data=info)
        return Result.error(code=50004, msg="会话不存在")
    except Exception as e:
        logger.error(f"获取会话失败: {e}")
        return Result.error(code=50004, msg=str(e))


@router.put("/{session_id}")
async def update_session(session_id: str, theme: str = None, role_prompt: str = None):
    """更新会话"""
    try:
        info = redis_service.get_session_info(session_id)
        if not info:
            return Result.error(code=50004, msg="会话不存在")

        if theme is not None:
            info["theme"] = theme
        if role_prompt is not None:
            info["role_prompt"] = role_prompt

        redis_service.save_session_info(session_id, info)
        return Result.success(data=info)
    except Exception as e:
        logger.error(f"更新会话失败: {e}")
        return Result.error(code=50004, msg=str(e))


@router.delete("/{session_id}")
async def delete_session(session_id: str):
    """删除会话"""
    try:
        redis_service.delete_session(session_id)
        return Result.success(msg="会话删除成功")
    except Exception as e:
        logger.error(f"删除会话失败: {e}")
        return Result.error(code=50004, msg=str(e))
