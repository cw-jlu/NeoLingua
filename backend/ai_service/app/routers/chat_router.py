"""
聊天路由
处理对话、流式对话、停止生成、反馈、发音分析
所有模型推理通过AI Gateway调用，由用户选择模型或由Gateway路由决定
"""
from fastapi import APIRouter
from fastapi.responses import StreamingResponse
from loguru import logger

from app.models.schemas import (
    Result, ChatRequest, StopRequest,
    FeedbackRequest, PronunciationRequest
)
from app.services import agent_service

router = APIRouter(prefix="/ai", tags=["聊天"])


@router.post("/chat")
async def chat(request: ChatRequest):
    """
    普通对话
    model_id可选：指定则使用该模型，不指定则由AI Gateway路由规则选择
    """
    try:
        result = await agent_service.chat(
            session_id=request.session_id,
            user_id=request.user_id,
            message=request.message,
            model_id=request.model_id,
            audio_url=request.audio_url,
            role_prompt=request.role_prompt,
            theme=request.theme,
            history=[m.model_dump() for m in request.history] if request.history else None
        )
        return Result.success(data=result)
    except Exception as e:
        logger.error(f"聊天失败: {e}")
        return Result.error(code=50001, msg=f"AI服务异常: {str(e)}")


@router.post("/chat/stream")
async def chat_stream(request: ChatRequest):
    """
    流式对话 - SSE
    通过AI Gateway的流式接口转发
    """
    try:
        return StreamingResponse(
            agent_service.chat_stream(
                session_id=request.session_id,
                user_id=request.user_id,
                message=request.message,
                model_id=request.model_id,
                role_prompt=request.role_prompt,
                theme=request.theme
            ),
            media_type="text/event-stream",
            headers={
                "Cache-Control": "no-cache",
                "Connection": "keep-alive",
                "X-Accel-Buffering": "no"
            }
        )
    except Exception as e:
        logger.error(f"流式聊天失败: {e}")
        return Result.error(code=50001, msg=f"AI服务异常: {str(e)}")


@router.post("/chat/stop")
async def stop_generation(request: StopRequest):
    """停止生成 - 转发到AI Gateway"""
    try:
        await agent_service.stop_generation(request.session_id)
        return Result.success(msg="已停止生成")
    except Exception as e:
        logger.error(f"停止生成失败: {e}")
        return Result.error(code=50001, msg=str(e))


@router.post("/feedback")
async def generate_feedback(request: FeedbackRequest):
    """生成反馈 - 通过AI Gateway调用模型"""
    try:
        result = await agent_service.generate_feedback(
            user_id=request.user_id,
            session_id=request.session_id,
            message=request.message,
            context=request.context,
            model_id=request.model_id
        )
        return Result.success(data=result)
    except Exception as e:
        logger.error(f"生成反馈失败: {e}")
        return Result.error(code=50001, msg=str(e))


@router.post("/pronunciation")
async def analyze_pronunciation(request: PronunciationRequest):
    """发音分析 - 通过AI Gateway调用模型"""
    try:
        result = await agent_service.analyze_pronunciation(
            user_id=request.user_id,
            text=request.text,
            reference_text=request.reference_text,
            model_id=request.model_id
        )
        return Result.success(data=result)
    except Exception as e:
        logger.error(f"发音分析失败: {e}")
        return Result.error(code=50001, msg=str(e))


@router.get("/models")
async def get_models():
    """获取可用模型列表（从AI Gateway获取）"""
    try:
        models = await agent_service.get_models()
        return Result.success(data=models)
    except Exception as e:
        logger.error(f"获取模型列表失败: {e}")
        return Result.error(code=50001, msg=str(e))
