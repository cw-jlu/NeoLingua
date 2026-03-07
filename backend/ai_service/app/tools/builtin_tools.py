"""
内置工具 - 时间工具和联网搜索工具
"""
from datetime import datetime
from typing import Optional
from loguru import logger


def get_current_time(timezone: str = "Asia/Shanghai") -> str:
    """
    获取当前时间
    返回格式化的当前日期时间字符串
    """
    try:
        from zoneinfo import ZoneInfo
        now = datetime.now(ZoneInfo(timezone))
    except Exception:
        now = datetime.now()
    return now.strftime("%Y-%m-%d %H:%M:%S %A")


def web_search(query: str, max_results: int = 5) -> str:
    """
    联网搜索工具
    使用DuckDuckGo搜索引擎获取信息
    """
    try:
        from duckduckgo_search import DDGS
        with DDGS() as ddgs:
            results = list(ddgs.text(query, max_results=max_results))
            if not results:
                return "未找到相关搜索结果"
            formatted = []
            for i, r in enumerate(results, 1):
                formatted.append(f"{i}. {r.get('title', '')}\n   {r.get('body', '')}\n   来源: {r.get('href', '')}")
            return "\n\n".join(formatted)
    except Exception as e:
        logger.error(f"联网搜索失败: {e}")
        return f"搜索失败: {str(e)}"


def register_builtin_tools():
    """注册所有内置工具到全局注册表"""
    from app.tools.base import tool_registry

    tool_registry.register(
        tool_id="get_time",
        name="获取当前时间",
        description="获取当前的日期和时间，可指定时区。用于回答关于时间的问题。",
        executor=get_current_time,
        parameters={
            "timezone": {
                "type": "string",
                "description": "时区，默认Asia/Shanghai",
                "default": "Asia/Shanghai"
            }
        }
    )

    tool_registry.register(
        tool_id="web_search",
        name="联网搜索",
        description="在互联网上搜索信息。用于回答需要最新信息或知识库中没有的问题。",
        executor=web_search,
        parameters={
            "query": {
                "type": "string",
                "description": "搜索关键词",
                "required": True
            },
            "max_results": {
                "type": "integer",
                "description": "最大结果数，默认5",
                "default": 5
            }
        }
    )

    logger.info("内置工具注册完成: 获取当前时间, 联网搜索")
