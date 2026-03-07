"""
高级功能API路由
提供纠错反馈、实时交互、评估评分等高级功能的REST API
"""
from fastapi import APIRouter, HTTPException, UploadFile, File, Form
from pydantic import BaseModel, Field
from typing import Optional, Dict, Any, List
import tempfile
import os
from pathlib import Path

from app.services import agent_service

router = APIRouter(prefix="/advanced", tags=["高级功能"])


# ==================== 请求模型 ====================

class PronunciationAssessmentRequest(BaseModel):
    """发音评估请求"""
    text: str = Field(..., description="要评估的文本")
    user_profile: Optional[Dict[str, Any]] = Field(None, description="用户画像")


class RealtimeSessionRequest(BaseModel):
    """实时会话请求"""
    mode: str = Field("half_duplex", description="交互模式: full_duplex, half_duplex, guided, free_talk")
    user_profile: Optional[Dict[str, Any]] = Field(None, description="用户画像")


class AudioFeaturesRequest(BaseModel):
    """音频特征请求"""
    energy_level: float = Field(0.5, ge=0.0, le=1.0, description="能量水平")
    pitch_variance: float = Field(0.5, ge=0.0, le=1.0, description="音调变化")
    speech_rate: float = Field(150, ge=50, le=300, description="语速(词/分钟)")
    pause_duration: float = Field(0.0, ge=0.0, description="停顿时长(秒)")
    voice_activity: bool = Field(True, description="是否有语音活动")
    confidence_score: float = Field(0.8, ge=0.0, le=1.0, description="识别置信度")
    transcribed_text: Optional[str] = Field(None, description="转录文本")


class SpeakingAssessmentRequest(BaseModel):
    """口语评估请求"""
    user_message: str = Field(..., description="用户消息")
    audio_analysis: Optional[Dict[str, Any]] = Field(None, description="音频分析结果")
    assessment_type: str = Field("ielts", description="评估类型: ielts, toefl")


class ChinglishDetectionRequest(BaseModel):
    """中式英语检测请求"""
    text: str = Field(..., description="要检测的文本")


class CompletionHelpRequest(BaseModel):
    """补全帮助请求"""
    incomplete_text: str = Field(..., description="不完整的文本")
    user_profile: Optional[Dict[str, Any]] = Field(None, description="用户画像")


# ==================== API端点 ====================

@router.post("/pronunciation/assess")
async def assess_pronunciation(
    user_id: str,
    session_id: str,
    audio_file: UploadFile = File(..., description="音频文件"),
    text: str = Form(..., description="要评估的文本"),
    user_profile: Optional[str] = Form(None, description="用户画像JSON字符串")
):
    """
    高级发音评估
    上传音频文件进行发音分析，返回详细的反馈和建议
    """
    try:
        # 解析用户画像
        import json
        profile = json.loads(user_profile) if user_profile else None
        
        # 保存临时音频文件
        with tempfile.NamedTemporaryFile(delete=False, suffix=".wav") as temp_file:
            content = await audio_file.read()
            temp_file.write(content)
            temp_audio_path = temp_file.name
        
        try:
            # 调用发音评估服务
            result = await agent_service.assess_pronunciation_advanced(
                user_id, session_id, temp_audio_path, text, profile
            )
            return result
        finally:
            # 清理临时文件
            if os.path.exists(temp_audio_path):
                os.unlink(temp_audio_path)
                
    except Exception as e:
        raise HTTPException(status_code=500, detail=f"发音评估失败: {str(e)}")


@router.post("/realtime/start")
async def start_realtime_session(
    user_id: str,
    session_id: str,
    request: RealtimeSessionRequest
):
    """
    启动实时对话会话
    支持多种交互模式：全双工、半双工、引导式、自由对话
    """
    try:
        result = await agent_service.start_realtime_conversation(
            user_id, session_id, request.mode, request.user_profile
        )
        return result
    except Exception as e:
        raise HTTPException(status_code=500, detail=f"启动实时会话失败: {str(e)}")


@router.post("/realtime/audio")
async def process_realtime_audio(
    session_id: str,
    request: AudioFeaturesRequest
):
    """
    处理实时音频流
    分析音频特征，判断轮流说话信号，提供实时反馈
    """
    try:
        audio_features = {
            "energy_level": request.energy_level,
            "pitch_variance": request.pitch_variance,
            "speech_rate": request.speech_rate,
            "pause_duration": request.pause_duration,
            "voice_activity": request.voice_activity,
            "confidence_score": request.confidence_score
        }
        
        result = await agent_service.process_realtime_audio(
            session_id, audio_features, request.transcribed_text
        )
        return result
    except Exception as e:
        raise HTTPException(status_code=500, detail=f"处理实时音频失败: {str(e)}")


@router.post("/realtime/end")
async def end_realtime_session(session_id: str):
    """
    结束实时对话会话
    生成会话总结和最终评估
    """
    try:
        result = await agent_service.end_realtime_session(session_id)
        return result
    except Exception as e:
        raise HTTPException(status_code=500, detail=f"结束实时会话失败: {str(e)}")


@router.post("/assessment/speaking")
async def assess_speaking_performance(
    user_id: str,
    session_id: str,
    request: SpeakingAssessmentRequest
):
    """
    口语表现评估
    基于IELTS/TOEFL标准进行多维度评分
    """
    try:
        result = await agent_service.get_speaking_assessment(
            user_id, session_id, request.user_message,
            request.audio_analysis, request.assessment_type
        )
        
        # agent_service已经返回字典格式，直接返回即可
        return result
    except Exception as e:
        raise HTTPException(status_code=500, detail=f"口语评估失败: {str(e)}")


@router.get("/progress/{user_id}")
async def get_progress_tracking(user_id: str, days: int = 30):
    """
    获取学习进度追踪
    分析用户在指定天数内的学习进展
    """
    try:
        result = await agent_service.get_progress_tracking(user_id, days)
        return result
    except Exception as e:
        raise HTTPException(status_code=500, detail=f"获取进度追踪失败: {str(e)}")


@router.post("/chinglish/detect")
async def detect_chinglish(request: ChinglishDetectionRequest):
    """
    中式英语检测
    识别常见的中式英语表达模式并提供纠正建议
    """
    try:
        result = await agent_service.detect_chinglish(request.text)
        return result
    except Exception as e:
        raise HTTPException(status_code=500, detail=f"中式英语检测失败: {str(e)}")


@router.post("/completion/help")
async def generate_completion_help(
    user_id: str,
    session_id: str,
    request: CompletionHelpRequest
):
    """
    生成引导式补全帮助
    当用户表达不完整时提供智能补全建议
    """
    try:
        result = await agent_service.generate_completion_help(
            user_id, session_id, request.incomplete_text, request.user_profile
        )
        return result
    except Exception as e:
        raise HTTPException(status_code=500, detail=f"生成补全帮助失败: {str(e)}")


@router.get("/session/status/{session_id}")
async def get_session_status(session_id: str):
    """
    获取会话状态
    返回当前会话的实时状态信息
    """
    try:
        from app.services.realtime_interaction_service import realtime_interaction_service
        result = await realtime_interaction_service.get_session_status(session_id)
        return result
    except Exception as e:
        raise HTTPException(status_code=500, detail=f"获取会话状态失败: {str(e)}")


@router.put("/session/mode/{session_id}")
async def update_interaction_mode(session_id: str, new_mode: str):
    """
    动态更新交互模式
    在会话进行中切换交互模式
    """
    try:
        from app.services.realtime_interaction_service import realtime_interaction_service, InteractionMode
        mode = InteractionMode(new_mode)
        result = await realtime_interaction_service.update_interaction_mode(session_id, mode)
        return result
    except Exception as e:
        raise HTTPException(status_code=500, detail=f"更新交互模式失败: {str(e)}")


# ==================== 批量操作端点 ====================

@router.post("/batch/assess")
async def batch_assessment(
    user_id: str,
    sessions: List[str],
    assessment_type: str = "ielts"
):
    """
    批量评估多个会话
    对用户的多个会话进行批量评估分析
    """
    try:
        results = []
        for session_id in sessions:
            # 获取会话消息
            from app.services import redis_service
            messages = redis_service.get_context_messages(session_id)
            
            if messages:
                # 合并用户消息
                user_messages = [msg["content"] for msg in messages if msg.get("role") == "user"]
                combined_text = " ".join(user_messages)
                
                if combined_text.strip():
                    result = await agent_service.get_speaking_assessment(
                        user_id, session_id, combined_text, None, assessment_type
                    )
                    results.append({
                        "session_id": session_id,
                        "assessment": result
                    })
        
        return {
            "total_sessions": len(sessions),
            "assessed_sessions": len(results),
            "results": results
        }
    except Exception as e:
        raise HTTPException(status_code=500, detail=f"批量评估失败: {str(e)}")


@router.get("/analytics/overview/{user_id}")
async def get_analytics_overview(user_id: str, days: int = 30):
    """
    获取分析概览
    提供用户学习分析的综合概览
    """
    try:
        # 获取进度追踪
        progress = await agent_service.get_progress_tracking(user_id, days)
        
        # 获取最近的评估历史
        from app.services import redis_service
        import json
        
        history_key = f"assessment_history:{user_id}"
        history_data = redis_service.get_data(history_key)
        history = json.loads(history_data) if history_data else []
        
        # 计算统计信息
        recent_history = history[-10:] if history else []
        avg_score = sum(h.get("overall_score", 0) for h in recent_history) / len(recent_history) if recent_history else 0
        
        return {
            "user_id": user_id,
            "analysis_period_days": days,
            "progress_tracking": progress,
            "recent_assessments": len(recent_history),
            "average_score": round(avg_score, 2),
            "assessment_history": recent_history,
            "recommendations": [
                "Continue regular practice sessions",
                "Focus on identified weak areas",
                "Try different interaction modes",
                "Set specific improvement goals"
            ]
        }
    except Exception as e:
        raise HTTPException(status_code=500, detail=f"获取分析概览失败: {str(e)}")