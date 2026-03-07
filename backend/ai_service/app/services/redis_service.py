"""
Redis服务 - 短期记忆存储
用于存储会话上下文、短期对话历史
"""
import json
from typing import Optional, List, Dict, Any
from loguru import logger

from app.config import settings

# Redis客户端（延迟初始化）
_redis_client = None


def get_redis_client():
    """获取Redis客户端（单例）"""
    global _redis_client
    if _redis_client is None:
        import redis
        _redis_client = redis.Redis(
            host=settings.REDIS_HOST,
            port=settings.REDIS_PORT,
            password=settings.REDIS_PASSWORD or None,
            db=settings.REDIS_DB,
            decode_responses=True
        )
        logger.info(f"Redis连接成功: {settings.REDIS_HOST}:{settings.REDIS_PORT}/{settings.REDIS_DB}")
    return _redis_client


def _session_key(session_id: str) -> str:
    """会话消息列表key"""
    return f"ai:session:{session_id}:messages"


def _session_info_key(session_id: str) -> str:
    """会话信息key"""
    return f"ai:session:{session_id}:info"


# ==================== 短期上下文操作 ====================

def save_message(session_id: str, role: str, content: str):
    """保存消息到短期上下文"""
    try:
        client = get_redis_client()
        key = _session_key(session_id)
        message = json.dumps({"role": role, "content": content}, ensure_ascii=False)
        client.rpush(key, message)
        # 保持最大消息数限制
        client.ltrim(key, -settings.MAX_CONTEXT_MESSAGES, -1)
        # 设置过期时间
        client.expire(key, settings.SHORT_TERM_TTL)
    except Exception as e:
        logger.error(f"保存消息到Redis失败: {e}")


def get_context_messages(session_id: str) -> List[Dict[str, str]]:
    """获取会话上下文消息"""
    try:
        client = get_redis_client()
        key = _session_key(session_id)
        messages = client.lrange(key, 0, -1)
        return [json.loads(m) for m in messages]
    except Exception as e:
        logger.error(f"获取上下文消息失败: {e}")
        return []


def clear_context(session_id: str):
    """清空会话上下文"""
    try:
        client = get_redis_client()
        client.delete(_session_key(session_id))
        client.delete(_session_info_key(session_id))
    except Exception as e:
        logger.error(f"清空上下文失败: {e}")


# ==================== 会话信息操作 ====================

def save_session_info(session_id: str, info: Dict[str, Any]):
    """保存会话信息"""
    try:
        client = get_redis_client()
        key = _session_info_key(session_id)
        client.set(key, json.dumps(info, ensure_ascii=False))
        client.expire(key, settings.SHORT_TERM_TTL)
    except Exception as e:
        logger.error(f"保存会话信息失败: {e}")


def get_session_info(session_id: str) -> Optional[Dict[str, Any]]:
    """获取会话信息"""
    try:
        client = get_redis_client()
        key = _session_info_key(session_id)
        data = client.get(key)
        return json.loads(data) if data else None
    except Exception as e:
        logger.error(f"获取会话信息失败: {e}")
        return None


def delete_session(session_id: str):
    """删除会话"""
    clear_context(session_id)


def get_all_sessions_by_user(user_id: str) -> List[Dict[str, Any]]:
    """获取用户所有会话（通过扫描key）"""
    try:
        client = get_redis_client()
        sessions = []
        cursor = 0
        while True:
            cursor, keys = client.scan(cursor, match="ai:session:*:info", count=100)
            for key in keys:
                data = client.get(key)
                if data:
                    info = json.loads(data)
                    if info.get("user_id") == user_id:
                        sessions.append(info)
            if cursor == 0:
                break
        return sessions
    except Exception as e:
        logger.error(f"获取用户会话列表失败: {e}")
        return []


# ==================== 通用数据操作 ====================

def get_data(key: str) -> Optional[str]:
    """获取数据"""
    try:
        client = get_redis_client()
        return client.get(key)
    except Exception as e:
        logger.error(f"获取数据失败 key={key}: {e}")
        return None


def set_data(key: str, value: str, expire: Optional[int] = None):
    """设置数据"""
    try:
        client = get_redis_client()
        client.set(key, value)
        if expire:
            client.expire(key, expire)
    except Exception as e:
        logger.error(f"设置数据失败 key={key}: {e}")
