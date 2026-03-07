"""
实时语音交互策略服务
实现全双工交互、实时反馈判断、语音切换策略
"""
import asyncio
import json
import time
from typing import Dict, List, Optional, Any, AsyncGenerator
from dataclasses import dataclass, asdict
from enum import Enum
from loguru import logger

from app.services.conversation_strategy_service import (
    ConversationContext, ConversationState, TurnTakingSignal, 
    conversation_strategy_service
)
from app.services.assessment_service import assessment_service
from app.services import redis_service


class InteractionMode(Enum):
    """交互模式"""
    FULL_DUPLEX = "full_duplex"      # 全双工：可以同时说话
    HALF_DUPLEX = "half_duplex"      # 半双工：轮流说话
    GUIDED = "guided"                # 引导模式：系统主导
    FREE_TALK = "free_talk"          # 自由对话：用户主导


class FeedbackTiming(Enum):
    """反馈时机"""
    IMMEDIATE = "immediate"          # 立即反馈
    TURN_END = "turn_end"           # 轮次结束后
    SESSION_END = "session_end"      # 会话结束后
    ON_REQUEST = "on_request"        # 用户请求时


@dataclass
class AudioFeatures:
    """音频特征"""
    energy_level: float              # 能量水平 (0-1)
    pitch_variance: float            # 音调变化 (0-1)
    speech_rate: float              # 语速 (words per minute)
    pause_duration: float           # 停顿时长 (seconds)
    voice_activity: bool            # 是否有语音活动
    confidence_score: float         # 识别置信度 (0-1)


@dataclass
class InteractionState:
    """交互状态"""
    session_id: str
    user_id: str
    mode: InteractionMode
    current_speaker: str            # "user" or "system"
    last_activity_time: float
    audio_features: Optional[AudioFeatures]
    pending_feedback: List[Dict]
    conversation_context: ConversationContext


class RealtimeInteractionService:
    """实时语音交互策略服务"""
    
    def __init__(self):
        # 交互策略配置
        self.interaction_config = {
            "voice_activity_threshold": 0.3,    # 语音活动检测阈值
            "silence_timeout": 3.0,             # 静默超时时间
            "interruption_threshold": 0.7,      # 打断阈值
            "feedback_delay": 0.5,              # 反馈延迟时间
            "turn_taking_sensitivity": 0.8      # 轮流说话敏感度
        }
        
        # 不同模式的策略参数
        self.mode_strategies = {
            InteractionMode.FULL_DUPLEX: {
                "allow_interruption": True,
                "immediate_feedback": True,
                "overlap_tolerance": 0.8
            },
            InteractionMode.HALF_DUPLEX: {
                "allow_interruption": False,
                "immediate_feedback": False,
                "overlap_tolerance": 0.2
            },
            InteractionMode.GUIDED: {
                "system_initiative": True,
                "structured_feedback": True,
                "error_correction": "immediate"
            },
            InteractionMode.FREE_TALK: {
                "user_initiative": True,
                "minimal_interruption": True,
                "error_correction": "delayed"
            }
        }
        
        # 活跃的交互会话
        self.active_sessions: Dict[str, InteractionState] = {}

    async def start_realtime_session(self, session_id: str, user_id: str,
                                   mode: InteractionMode = InteractionMode.HALF_DUPLEX,
                                   user_profile: Optional[Dict] = None) -> Dict[str, Any]:
        """启动实时交互会话"""
        # 创建对话上下文
        conversation_context = ConversationContext(
            session_id=session_id,
            user_id=user_id,
            current_state=ConversationState.LISTENING,
            user_level=user_profile.get("level", "intermediate") if user_profile else "intermediate",
            emotional_state="confident",
            silence_duration=0.0,
            last_user_input_time=time.time()
        )
        
        # 创建交互状态
        interaction_state = InteractionState(
            session_id=session_id,
            user_id=user_id,
            mode=mode,
            current_speaker="system",  # 系统先开始
            last_activity_time=time.time(),
            audio_features=None,
            pending_feedback=[],
            conversation_context=conversation_context
        )
        
        self.active_sessions[session_id] = interaction_state
        
        # 生成开场白
        opening_message = await self._generate_opening_message(mode, user_profile)
        
        return {
            "session_id": session_id,
            "mode": mode.value,
            "status": "started",
            "opening_message": opening_message,
            "interaction_config": self.mode_strategies[mode]
        }

    async def process_audio_stream(self, session_id: str, 
                                 audio_features: AudioFeatures,
                                 transcribed_text: Optional[str] = None) -> Dict[str, Any]:
        """处理实时音频流"""
        if session_id not in self.active_sessions:
            return {"error": "Session not found"}
        
        state = self.active_sessions[session_id]
        state.audio_features = audio_features
        state.last_activity_time = time.time()
        
        # 更新对话上下文
        if transcribed_text:
            state.conversation_context.last_user_input_time = time.time()
        
        # 分析轮流说话信号
        turn_signal = await conversation_strategy_service.analyze_turn_taking(
            state.conversation_context, 
            asdict(audio_features) if audio_features else None
        )
        
        # 根据信号决定交互策略
        response = await self._handle_turn_taking_signal(state, turn_signal, transcribed_text)
        
        return response

    async def _handle_turn_taking_signal(self, state: InteractionState, 
                                       signal: TurnTakingSignal,
                                       transcribed_text: Optional[str]) -> Dict[str, Any]:
        """处理轮流说话信号"""
        response = {
            "session_id": state.session_id,
            "signal": signal.value,
            "action": "continue",
            "feedback": None,
            "system_response": None
        }
        
        if signal == TurnTakingSignal.USER_FINISHED:
            # 用户说完了，系统可以回应
            if transcribed_text:
                response["action"] = "system_turn"
                response["system_response"] = await self._generate_system_response(
                    state, transcribed_text
                )
                state.current_speaker = "system"
        
        elif signal == TurnTakingSignal.USER_HESITATING:
            # 用户犹豫，提供帮助
            response["action"] = "provide_help"
            response["system_response"] = await self._generate_help_response(state)
        
        elif signal == TurnTakingSignal.USER_STUCK:
            # 用户卡壳，提供引导
            response["action"] = "provide_guidance"
            guidance = await conversation_strategy_service.generate_guided_completion(
                state.conversation_context, transcribed_text or ""
            )
            response["guidance"] = guidance
        
        elif signal == TurnTakingSignal.SYSTEM_SHOULD_SPEAK:
            # 系统应该说话
            if state.mode == InteractionMode.FULL_DUPLEX:
                response["action"] = "system_speak"
                response["system_response"] = await self._generate_concurrent_response(state)
        
        # 检查是否需要即时反馈
        if await self._should_provide_immediate_feedback(state, transcribed_text):
            feedback = await self._generate_immediate_feedback(state, transcribed_text)
            response["feedback"] = feedback
        
        return response

    async def _should_provide_immediate_feedback(self, state: InteractionState, 
                                               text: Optional[str]) -> bool:
        """判断是否应该提供即时反馈"""
        if not text:
            return False
        
        # 根据交互模式决定
        mode_strategy = self.mode_strategies[state.mode]
        
        if not mode_strategy.get("immediate_feedback", False):
            return False
        
        # 检查是否有明显错误需要立即纠正
        should_correct = await conversation_strategy_service.should_provide_immediate_feedback(
            state.conversation_context, text
        )
        
        return should_correct

    async def _generate_immediate_feedback(self, state: InteractionState, 
                                         text: str) -> Dict[str, Any]:
        """生成即时反馈"""
        # 检测中式英语
        chinglish_analysis = await conversation_strategy_service.detect_chinglish_patterns(text)
        
        feedback = {
            "type": "immediate",
            "timing": "during_speech",
            "content": [],
            "severity": "low"
        }
        
        if chinglish_analysis["has_chinglish"]:
            feedback["severity"] = chinglish_analysis["severity"]
            for pattern in chinglish_analysis["patterns"][:2]:  # 最多2个
                feedback["content"].append({
                    "type": "correction",
                    "original": pattern["pattern"],
                    "suggestion": pattern["suggestion"],
                    "explanation": "Try this instead"
                })
        
        # 添加鼓励性反馈
        if state.conversation_context.emotional_state == "anxious":
            feedback["content"].append({
                "type": "encouragement",
                "message": "You're doing great! Keep going."
            })
        
        return feedback

    async def _generate_system_response(self, state: InteractionState, 
                                      user_text: str) -> Dict[str, Any]:
        """生成系统回应"""
        # 更新情感状态
        emotional_state = await conversation_strategy_service.detect_emotional_state(
            user_text, []
        )
        state.conversation_context.emotional_state = emotional_state
        
        # 生成情感共鸣回应
        empathetic_response = await conversation_strategy_service.generate_empathetic_response(
            state.conversation_context, user_text
        )
        
        # 根据交互模式调整回应
        if state.mode == InteractionMode.GUIDED:
            response_type = "structured"
            content = await self._generate_guided_response(state, user_text)
        elif state.mode == InteractionMode.FREE_TALK:
            response_type = "conversational"
            content = await self._generate_conversational_response(state, user_text)
        else:
            response_type = "balanced"
            content = await self._generate_balanced_response(state, user_text)
        
        return {
            "type": response_type,
            "content": content,
            "empathetic_elements": empathetic_response,
            "suggested_tone": empathetic_response.get("suggested_tone", "friendly"),
            "interaction_adjustment": empathetic_response.get("interaction_adjustment", {})
        }

    async def _generate_help_response(self, state: InteractionState) -> Dict[str, Any]:
        """生成帮助回应"""
        help_messages = [
            "Take your time. What would you like to say?",
            "No rush! I'm here to help.",
            "Would you like me to give you a hint?",
            "It's okay to pause and think."
        ]
        
        # 根据用户水平选择合适的帮助
        if state.conversation_context.user_level == "beginner":
            message = "Don't worry! Take your time to think about what you want to say."
        elif state.conversation_context.user_level == "advanced":
            message = "Feel free to take a moment to organize your thoughts."
        else:
            message = help_messages[0]
        
        return {
            "type": "help",
            "message": message,
            "suggestions": [
                "Try starting with 'I think...'",
                "You could say 'In my opinion...'",
                "How about 'Let me explain...'"
            ]
        }

    async def _generate_concurrent_response(self, state: InteractionState) -> Dict[str, Any]:
        """生成并发回应（全双工模式）"""
        return {
            "type": "concurrent",
            "message": "I see what you mean...",
            "backchanneling": True,  # 表示这是背景回应
            "volume": 0.7  # 降低音量避免干扰
        }

    async def _generate_guided_response(self, state: InteractionState, 
                                      user_text: str) -> str:
        """生成引导式回应"""
        # 结构化的教学回应
        return f"Good! You said '{user_text}'. Now, can you tell me more about that topic?"

    async def _generate_conversational_response(self, state: InteractionState, 
                                              user_text: str) -> str:
        """生成对话式回应"""
        # 自然的对话回应
        return "That's interesting! I'd love to hear more about your thoughts on this."

    async def _generate_balanced_response(self, state: InteractionState, 
                                        user_text: str) -> str:
        """生成平衡式回应"""
        # 教学和对话的平衡
        return "Great expression! Your English is improving. What else would you like to discuss?"

    async def _generate_opening_message(self, mode: InteractionMode, 
                                      user_profile: Optional[Dict]) -> str:
        """生成开场白"""
        user_level = user_profile.get("level", "intermediate") if user_profile else "intermediate"
        
        openings = {
            InteractionMode.FULL_DUPLEX: f"Hi! I'm ready for a natural conversation. Feel free to speak naturally, and I'll respond in real-time. What would you like to talk about?",
            InteractionMode.HALF_DUPLEX: f"Hello! Let's have a structured conversation. I'll wait for you to finish speaking before I respond. What topic interests you today?",
            InteractionMode.GUIDED: f"Welcome to guided practice! I'll help you step by step to improve your English. Let's start with introducing yourself.",
            InteractionMode.FREE_TALK: f"Hey there! This is free talk time. Share anything on your mind, and I'll listen and respond naturally."
        }
        
        base_message = openings.get(mode, openings[InteractionMode.HALF_DUPLEX])
        
        # 根据用户水平调整
        if user_level == "beginner":
            base_message += " Don't worry about making mistakes - that's how we learn!"
        elif user_level == "advanced":
            base_message += " Feel free to use complex expressions and ideas."
        
        return base_message

    async def end_realtime_session(self, session_id: str) -> Dict[str, Any]:
        """结束实时交互会话"""
        if session_id not in self.active_sessions:
            return {"error": "Session not found"}
        
        state = self.active_sessions[session_id]
        
        # 生成会话总结
        session_summary = await self._generate_session_summary(state)
        
        # 进行最终评估
        if hasattr(state, 'conversation_history'):
            final_assessment = await assessment_service.assess_speaking_performance(
                state.user_id, session_id, "", None, state.conversation_history
            )
        else:
            final_assessment = None
        
        # 清理会话状态
        del self.active_sessions[session_id]
        
        return {
            "session_id": session_id,
            "status": "ended",
            "summary": session_summary,
            "final_assessment": final_assessment,
            "recommendations": await self._generate_session_recommendations(state)
        }

    async def _generate_session_summary(self, state: InteractionState) -> Dict[str, Any]:
        """生成会话总结"""
        duration = time.time() - (state.last_activity_time - 1800)  # 假设会话时长
        
        return {
            "duration_minutes": duration / 60,
            "interaction_mode": state.mode.value,
            "total_turns": len(state.conversation_context.turn_taking_history),
            "emotional_journey": self._analyze_emotional_journey(state),
            "key_achievements": [
                "Maintained conversation flow",
                "Showed improvement in fluency",
                "Demonstrated good vocabulary usage"
            ]
        }

    def _analyze_emotional_journey(self, state: InteractionState) -> List[str]:
        """分析情感变化历程"""
        # 简化的情感历程分析
        return [
            "Started confidently",
            "Showed some hesitation mid-conversation",
            "Ended on a positive note"
        ]

    async def _generate_session_recommendations(self, state: InteractionState) -> List[str]:
        """生成会话建议"""
        recommendations = []
        
        if state.mode == InteractionMode.FULL_DUPLEX:
            recommendations.append("Practice more full-duplex conversations to improve natural interaction")
        
        if state.conversation_context.emotional_state == "anxious":
            recommendations.append("Try relaxation techniques before speaking practice")
        
        recommendations.extend([
            "Continue regular speaking practice",
            "Focus on areas identified in the assessment",
            "Try different interaction modes to challenge yourself"
        ])
        
        return recommendations

    async def get_session_status(self, session_id: str) -> Dict[str, Any]:
        """获取会话状态"""
        if session_id not in self.active_sessions:
            return {"error": "Session not found"}
        
        state = self.active_sessions[session_id]
        
        return {
            "session_id": session_id,
            "status": "active",
            "mode": state.mode.value,
            "current_speaker": state.current_speaker,
            "last_activity": time.time() - state.last_activity_time,
            "emotional_state": state.conversation_context.emotional_state,
            "pending_feedback_count": len(state.pending_feedback)
        }

    async def update_interaction_mode(self, session_id: str, 
                                    new_mode: InteractionMode) -> Dict[str, Any]:
        """动态更新交互模式"""
        if session_id not in self.active_sessions:
            return {"error": "Session not found"}
        
        state = self.active_sessions[session_id]
        old_mode = state.mode
        state.mode = new_mode
        
        return {
            "session_id": session_id,
            "old_mode": old_mode.value,
            "new_mode": new_mode.value,
            "message": f"Interaction mode changed from {old_mode.value} to {new_mode.value}",
            "new_config": self.mode_strategies[new_mode]
        }


# 全局服务实例
realtime_interaction_service = RealtimeInteractionService()