"""
工具管理路由
处理工具的注册、查询、删除、执行
"""
from fastapi import APIRouter
from loguru import logger

from app.models.schemas import Result, ToolRegisterRequest, ToolExecuteRequest
from app.tools.base import tool_registry

router = APIRouter(prefix="/ai/tools", tags=["工具管理"])


@router.get("")
async def list_tools():
    """获取工具列表"""
    try:
        tools = tool_registry.list_tools()
        return Result.success(data=tools)
    except Exception as e:
        logger.error(f"获取工具列表失败: {e}")
        return Result.error(code=50003, msg=str(e))


@router.post("/register")
async def register_tool(request: ToolRegisterRequest):
    """注册新工具（动态注册，仅注册元信息，执行器需代码实现）"""
    try:
        tool_id = request.name.lower().replace(" ", "_")
        # 动态注册的工具使用通用执行器
        def placeholder_executor(**kwargs):
            return f"工具 {request.name} 已注册但未实现执行逻辑"

        tool_registry.register(
            tool_id=tool_id,
            name=request.name,
            description=request.description,
            executor=placeholder_executor,
            parameters=request.parameters
        )
        return Result.success(data={"tool_id": tool_id}, msg="工具注册成功")
    except Exception as e:
        logger.error(f"注册工具失败: {e}")
        return Result.error(code=50003, msg=str(e))


@router.delete("/{tool_id}")
async def delete_tool(tool_id: str):
    """删除工具"""
    try:
        success = tool_registry.unregister(tool_id)
        if success:
            return Result.success(msg="工具删除成功")
        return Result.error(code=50003, msg="工具不存在")
    except Exception as e:
        logger.error(f"删除工具失败: {e}")
        return Result.error(code=50003, msg=str(e))


@router.post("/{tool_id}/execute")
async def execute_tool(tool_id: str, request: ToolExecuteRequest):
    """执行工具"""
    try:
        result = tool_registry.execute(tool_id, request.parameters)
        return Result.success(data={"result": result})
    except ValueError as e:
        return Result.error(code=50003, msg=str(e))
    except Exception as e:
        logger.error(f"执行工具失败: {e}")
        return Result.error(code=50003, msg=str(e))
