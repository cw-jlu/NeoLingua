"""
工具基类和工具注册表
支持动态注册和扩展工具
"""
from typing import Dict, Any, Optional, List, Callable
from loguru import logger


class ToolRegistry:
    """
    工具注册表 - 管理所有可用工具
    支持动态注册、删除、查询工具
    """

    def __init__(self):
        self._tools: Dict[str, Dict[str, Any]] = {}
        self._executors: Dict[str, Callable] = {}

    def register(self, tool_id: str, name: str, description: str,
                 executor: Callable, parameters: Optional[Dict[str, Any]] = None):
        """注册工具"""
        self._tools[tool_id] = {
            "tool_id": tool_id,
            "name": name,
            "description": description,
            "parameters": parameters or {}
        }
        self._executors[tool_id] = executor
        logger.info(f"工具注册成功: {name} ({tool_id})")

    def unregister(self, tool_id: str) -> bool:
        """注销工具"""
        if tool_id in self._tools:
            del self._tools[tool_id]
            del self._executors[tool_id]
            logger.info(f"工具注销成功: {tool_id}")
            return True
        return False

    def get_tool_info(self, tool_id: str) -> Optional[Dict[str, Any]]:
        """获取工具信息"""
        return self._tools.get(tool_id)

    def list_tools(self) -> List[Dict[str, Any]]:
        """列出所有工具"""
        return list(self._tools.values())

    def execute(self, tool_id: str, parameters: Optional[Dict[str, Any]] = None) -> Any:
        """执行工具"""
        executor = self._executors.get(tool_id)
        if executor is None:
            raise ValueError(f"工具不存在: {tool_id}")
        try:
            return executor(**(parameters or {}))
        except Exception as e:
            logger.error(f"工具执行失败: {tool_id}, 错误: {e}")
            raise

    def get_langchain_tools(self):
        """获取LangChain格式的工具列表，用于Agent"""
        from langchain.tools import Tool
        lc_tools = []
        for tool_id, info in self._tools.items():
            executor = self._executors[tool_id]
            lc_tools.append(Tool(
                name=info["name"],
                description=info["description"],
                func=lambda params=None, _exec=executor: _exec(**(params if isinstance(params, dict) else {}))
            ))
        return lc_tools


# 全局工具注册表
tool_registry = ToolRegistry()
