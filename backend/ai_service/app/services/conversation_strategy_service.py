"""
智能对话策略服务
实现全双工交互、Turn-taking预测、情感感知等高级功能
"""
import asyncio
import json
import time
from typing import Dict, List, Optional, Any, AsyncGenerator
from loguru import logger
from dataclasses import dataclass
from enum import Enum

from app.config import settings
from app.services import redis_service, memory_service


class ConversationState(Enum):
    """对话状态枚举"""
    LISTENING = "listening"
    THINKING = "thinking"
    SPEAKING = "speaking"
    WAITING_FOR_TURN = "waiting_for_turn"
    ENCOURAGING = "encouraging"


class TurnTakingSignal(Enum):
    """轮流说话信号"""
    USER_CONTINUE = "user_continue"
    USER_FINISHED = "user_finished"
    USER_HESITATING = "user_hesitating"
    USER_STUCK = "user_stuck"
    SYSTEM_SHOULD_SPEAK = "system_should_speak"
    SYSTEM_SHOULD_WAIT = "system_should_wait"


@dataclass
class ConversationContext:
    """对话上下文"""
    session_id: str
    user_id: str
    current_state: ConversationState
    user_level: str  # beginner, intermediate, advanced
    emotional_state: str  # confident, anxious, frustrated, excited
    silence_duration: float
    last_user_input_time: float
    conversation_topic: Optional[str] = None
    role_name: Optional[str] = None
    turn_taking_history: List[Dict] = None
    
    def __post_init__(self):
        if self.turn_taking_history is None:
            self.turn_taking_history = []


class ConversationStrategyService:
    """智能对话策略服务"""
    
    def __init__(self):
        # Turn-taking 预测模型参数
        self.turn_taking_thresholds = {
            "silence_threshold": 2.0,  # 静默超过2秒认为用户可能结束
            "hesitation_threshold": 1.5,  # 停顿1.5秒认为用户犹豫
            "stuck_threshold": 3.0,  # 停顿3秒认为用户卡壳
            "encouragement_threshold": 5.0  # 停顿5秒需要鼓励
        }
        
        # 情感识别关键词
        self.emotional_indicators = {
            "anxious": ["nervous", "worried", "scared", "difficult", "hard", "can't"],
            "frustrated": ["annoying", "stupid", "hate", "wrong", "bad", "terrible"],
            "confident": ["easy", "good", "great", "perfect", "love", "excellent"],
            "excited": ["amazing", "wonderful", "fantastic", "awesome", "cool"]
        }
        
        # 中式英语识别模式
        self.chinglish_patterns = {
            "word_order": [
                r"very like",  # 很喜欢 -> really like
                r"so so",      # 一般般 -> not bad / okay
                r"how to say", # 怎么说 -> what's the word / how do you say
            ],
            "literal_translation": [
                r"open the light",  # 开灯 -> turn on the light
                r"close the light", # 关灯 -> turn off the light
                r"eat medicine",    # 吃药 -> take medicine
            ],
            "grammar_errors": [
                r"I very",          # 我很 -> I am very
                r"have been to",    # 去过 -> have been to (correct usage check)
                r"more and more",   # 越来越 -> increasingly
            ]
        }
        
        # 引导式补全模板
        self.completion_templates = {
            "vocabulary_help": [
                "The word you're looking for might be '{suggestion}'",
                "Are you trying to say '{suggestion}'?",
                "Perhaps you mean '{suggestion}'?"
            ],
            "grammar_help": [
                "You could say: '{corrected_sentence}'",
                "A better way might be: '{corrected_sentence}'",
                "Try this: '{corrected_sentence}'"
            ],
            "encouragement": [
                "Take your time, you're doing great!",
                "Don't worry, everyone makes mistakes when learning.",
                "You're improving! Keep going!"
            ]
        }

    async def analyze_turn_taking(self, context: ConversationContext, 
                                audio_features: Optional[Dict] = None) -> TurnTakingSignal:
        """
        分析轮流说话信号
        基于静默时长、语音特征、对话历史等判断
        """
        current_time = time.time()
        silence_duration = current_time - context.last_user_input_time
        
        # 基于静默时长的基础判断
        if silence_duration > self.turn_taking_thresholds["encouragement_threshold"]:
            return TurnTakingSignal.USER_STUCK
        elif silence_duration > self.turn_taking_thresholds["stuck_threshold"]:
            return TurnTakingSignal.USER_HESITATING
        elif silence_duration > self.turn_taking_thresholds["hesitation_threshold"]:
            # 需要更细致的判断
            return await self._analyze_hesitation_context(context, audio_features)
        elif silence_duration > self.turn_taking_thresholds["silence_threshold"]:
            return TurnTakingSignal.USER_FINISHED
        else:
            return TurnTakingSignal.USER_CONTINUE

    async def _analyze_hesitation_context(self, context: ConversationContext, 
                                        audio_features: Optional[Dict]) -> TurnTakingSignal:
        """分析犹豫情况的具体原因"""
        # 检查最近的对话历史
        recent_messages = redis_service.get_context_messages(context.session_id, limit=3)
        
        # 如果用户最后一句话很短或包含犹豫词汇
        if recent_messages:
            last_user_msg = None
            for msg in reversed(recent_messages):
                if msg.get("role") == "user":
                    last_user_msg = msg.get("content", "")
                    break
            
            if last_user_msg:
                hesitation_words = ["um", "uh", "well", "let me think", "how to say"]
                if any(word in last_user_msg.lower() for word in hesitation_words):
                    return TurnTakingSignal.USER_HESITATING
                
                # 如果句子很短且不完整
                if len(last_user_msg.split()) < 3:
                    return TurnTakingSignal.USER_HESITATING
        
        # 基于音频特征判断（如果有）
        if audio_features:
            # 检查语音能量、音调变化等
            if audio_features.get("energy_level", 0) < 0.3:
                return TurnTakingSignal.USER_HESITATING
            if audio_features.get("pitch_variance", 0) > 0.8:
                return TurnTakingSignal.USER_HESITATING
        
        return TurnTakingSignal.SYSTEM_SHOULD_WAIT

    async def detect_emotional_state(self, user_message: str, 
                                   conversation_history: List[Dict]) -> str:
        """检测用户情感状态"""
        message_lower = user_message.lower()
        
        # 基于关键词的情感识别
        emotion_scores = {}
        for emotion, keywords in self.emotional_indicators.items():
            score = sum(1 for keyword in keywords if keyword in message_lower)
            if score > 0:
                emotion_scores[emotion] = score
        
        # 基于对话历史的情感趋势分析
        if conversation_history:
            recent_messages = conversation_history[-3:]  # 最近3条消息
            for msg in recent_messages:
                if msg.get("role") == "user":
                    content = msg.get("content", "").lower()
                    # 检查重复的错误或困难表达
                    if "sorry" in content or "mistake" in content:
                        emotion_scores["anxious"] = emotion_scores.get("anxious", 0) + 1
                    elif "again" in content or "repeat" in content:
                        emotion_scores["frustrated"] = emotion_scores.get("frustrated", 0) + 1
        
        # 返回得分最高的情感，默认为confident
        if emotion_scores:
            return max(emotion_scores.items(), key=lambda x: x[1])[0]
        return "confident"

    async def detect_chinglish_patterns(self, text: str) -> Dict[str, Any]:
        """检测中式英语表达模式"""
        import re
        detected_patterns = []
        
        for category, patterns in self.chinglish_patterns.items():
            for pattern in patterns:
                matches = re.findall(pattern, text.lower())
                if matches:
                    detected_patterns.append({
                        "category": category,
                        "pattern": pattern,
                        "matches": matches,
                        "suggestion": await self._get_chinglish_correction(pattern)
                    })
        
        return {
            "has_chinglish": len(detected_patterns) > 0,
            "patterns": detected_patterns,
            "severity": "high" if len(detected_patterns) > 2 else "medium" if len(detected_patterns) > 0 else "low"
        }

    async def _get_chinglish_correction(self, pattern: str) -> str:
        """获取中式英语的纠正建议"""
        corrections = {
            r"very like": "really like / love",
            r"so so": "not bad / okay / it's alright",
            r"how to say": "what's the word / how do you say",
            r"open the light": "turn on the light",
            r"close the light": "turn off the light",
            r"eat medicine": "take medicine",
            r"I very": "I am very",
            r"more and more": "increasingly / more and more (when used correctly)"
        }
        return corrections.get(pattern, "Please check the grammar and word usage")

    async def generate_guided_completion(self, context: ConversationContext, 
                                       incomplete_text: str) -> Dict[str, Any]:
        """生成引导式补全建议"""
        # 分析用户可能想表达的内容
        suggestions = await self._predict_user_intent(incomplete_text, context)
        
        # 根据用户水平调整建议复杂度
        adjusted_suggestions = self._adjust_difficulty(suggestions, context.user_level)
        
        return {
            "suggestions": adjusted_suggestions,
            "completion_type": self._determine_completion_type(incomplete_text),
            "encouragement": self._get_contextual_encouragement(context)
        }

    async def _predict_user_intent(self, incomplete_text: str, 
                                 context: ConversationContext) -> List[str]:
        """预测用户意图并生成补全建议"""
        # 简单的基于规则的意图预测
        text_lower = incomplete_text.lower().strip()
        
        # 常见的开头模式
        if text_lower.startswith("i want to"):
            return ["I want to go", "I want to learn", "I want to try"]
        elif text_lower.startswith("i like"):
            return ["I like reading", "I like music", "I like traveling"]
        elif text_lower.startswith("how"):
            return ["How are you?", "How do you say", "How can I"]
        elif text_lower.startswith("what"):
            return ["What do you think?", "What's your opinion?", "What about"]
        elif text_lower.startswith("where"):
            return ["Where are you from?", "Where do you live?", "Where can I find"]
        
        # 基于对话主题的建议
        if context.conversation_topic:
            topic_suggestions = await self._get_topic_based_suggestions(
                incomplete_text, context.conversation_topic
            )
            if topic_suggestions:
                return topic_suggestions
        
        # 通用建议
        return [
            "Could you help me?",
            "I think that...",
            "In my opinion...",
            "Let me explain..."
        ]

    async def _get_topic_based_suggestions(self, incomplete_text: str, topic: str) -> List[str]:
        """基于对话主题生成建议"""
        topic_templates = {
            "travel": [
                "I've been to...",
                "I'd like to visit...",
                "My favorite place is...",
                "I enjoy traveling because..."
            ],
            "food": [
                "I love eating...",
                "My favorite dish is...",
                "I can cook...",
                "The taste is..."
            ],
            "hobbies": [
                "I enjoy...",
                "My hobby is...",
                "I spend time...",
                "I'm interested in..."
            ],
            "work": [
                "I work as...",
                "My job involves...",
                "I'm responsible for...",
                "At work, I..."
            ]
        }
        return topic_templates.get(topic.lower(), [])

    def _adjust_difficulty(self, suggestions: List[str], user_level: str) -> List[str]:
        """根据用户水平调整建议难度"""
        if user_level == "beginner":
            # 简化建议，使用基础词汇
            return [s for s in suggestions if len(s.split()) <= 5]
        elif user_level == "advanced":
            # 提供更复杂的表达
            advanced_suggestions = []
            for s in suggestions:
                if len(s.split()) > 3:
                    advanced_suggestions.append(s)
            return advanced_suggestions if advanced_suggestions else suggestions
        return suggestions  # intermediate level

    def _determine_completion_type(self, incomplete_text: str) -> str:
        """确定补全类型"""
        if len(incomplete_text.strip()) == 0:
            return "conversation_starter"
        elif incomplete_text.strip().endswith(("how", "what", "where", "when", "why")):
            return "question_completion"
        elif len(incomplete_text.split()) < 3:
            return "phrase_completion"
        else:
            return "sentence_completion"

    def _get_contextual_encouragement(self, context: ConversationContext) -> str:
        """获取上下文相关的鼓励语"""
        if context.emotional_state == "anxious":
            return "Don't worry, take your time. You're doing better than you think!"
        elif context.emotional_state == "frustrated":
            return "I understand it can be challenging. Let's try a different approach."
        elif context.emotional_state == "confident":
            return "Great! You're expressing yourself very well."
        else:
            return "Keep going! You're making good progress."

    async def generate_empathetic_response(self, context: ConversationContext, 
                                         user_message: str) -> Dict[str, Any]:
        """生成情感共鸣响应"""
        emotional_state = await self.detect_emotional_state(user_message, [])
        
        empathy_responses = {
            "anxious": {
                "acknowledgment": "I can sense you might be feeling a bit nervous about this.",
                "support": "That's completely normal when learning a new language.",
                "encouragement": "Remember, every expert was once a beginner. You're doing great!",
                "action": "Let's take it step by step. Would you like to try something easier first?"
            },
            "frustrated": {
                "acknowledgment": "I understand this can be frustrating sometimes.",
                "support": "Learning a language has its ups and downs for everyone.",
                "encouragement": "Your persistence shows real dedication. That's admirable!",
                "action": "Let's try a different approach that might work better for you."
            },
            "confident": {
                "acknowledgment": "I can hear the confidence in your expression!",
                "support": "You're really getting the hang of this.",
                "encouragement": "Your progress is impressive. Keep up the excellent work!",
                "action": "Ready for a slightly more challenging topic?"
            }
        }
        
        response_template = empathy_responses.get(emotional_state, empathy_responses["confident"])
        
        return {
            "emotional_state": emotional_state,
            "empathetic_response": response_template,
            "suggested_tone": "warm" if emotional_state in ["anxious", "frustrated"] else "encouraging",
            "interaction_adjustment": self._get_interaction_adjustment(emotional_state)
        }

    def _get_interaction_adjustment(self, emotional_state: str) -> Dict[str, Any]:
        """根据情感状态调整交互方式"""
        adjustments = {
            "anxious": {
                "speaking_speed": "slower",
                "complexity": "reduced",
                "patience_level": "high",
                "encouragement_frequency": "high"
            },
            "frustrated": {
                "speaking_speed": "normal",
                "complexity": "simplified",
                "patience_level": "very_high",
                "alternative_approach": True
            },
            "confident": {
                "speaking_speed": "normal",
                "complexity": "maintained",
                "challenge_level": "increased",
                "praise_frequency": "high"
            }
        }
        return adjustments.get(emotional_state, adjustments["confident"])

    async def update_conversation_context(self, context: ConversationContext, 
                                        user_message: str, system_response: str) -> ConversationContext:
        """更新对话上下文"""
        # 更新情感状态
        context.emotional_state = await self.detect_emotional_state(user_message, [])
        
        # 更新时间戳
        context.last_user_input_time = time.time()
        
        # 记录turn-taking历史
        turn_record = {
            "timestamp": time.time(),
            "user_message_length": len(user_message.split()),
            "silence_before": context.silence_duration,
            "emotional_state": context.emotional_state
        }
        context.turn_taking_history.append(turn_record)
        
        # 保持历史记录在合理范围内
        if len(context.turn_taking_history) > 20:
            context.turn_taking_history = context.turn_taking_history[-20:]
        
        return context

    async def should_provide_immediate_feedback(self, context: ConversationContext, 
                                              user_message: str) -> bool:
        """判断是否应该提供即时反馈"""
        # 检测明显错误
        chinglish_result = await self.detect_chinglish_patterns(user_message)
        
        # 如果有严重的中式英语问题
        if chinglish_result["severity"] == "high":
            return True
        
        # 如果用户表现出焦虑情绪
        if context.emotional_state == "anxious":
            return False  # 避免增加压力
        
        # 如果用户水平较高，可以提供更多即时反馈
        if context.user_level == "advanced":
            return chinglish_result["has_chinglish"]
        
        return False


# 全局服务实例
conversation_strategy_service = ConversationStrategyService()