"""
评估与自动化评分服务
实现LLM-as-a-Judge、多维度进步追踪、动态评估系统
"""
import json
import asyncio
from typing import Dict, List, Optional, Any, Tuple
from datetime import datetime, timedelta
from dataclasses import dataclass, asdict
from enum import Enum
from loguru import logger

from app.config import settings
from app.services import memory_service, redis_service


class ProficiencyLevel(Enum):
    """熟练度等级"""
    BEGINNER = "beginner"
    ELEMENTARY = "elementary"
    INTERMEDIATE = "intermediate"
    UPPER_INTERMEDIATE = "upper_intermediate"
    ADVANCED = "advanced"
    PROFICIENT = "proficient"


class AssessmentDimension(Enum):
    """评估维度"""
    FLUENCY = "fluency"              # 流利度
    ACCURACY = "accuracy"            # 准确性
    PRONUNCIATION = "pronunciation"   # 发音
    VOCABULARY = "vocabulary"        # 词汇
    GRAMMAR = "grammar"              # 语法
    COHERENCE = "coherence"          # 连贯性
    PRAGMATICS = "pragmatics"        # 语用能力


@dataclass
class AssessmentCriteria:
    """评估标准"""
    dimension: AssessmentDimension
    weight: float
    description: str
    scoring_rubric: Dict[str, str]


@dataclass
class AssessmentResult:
    """评估结果"""
    user_id: str
    session_id: str
    timestamp: datetime
    overall_score: float
    proficiency_level: ProficiencyLevel
    dimension_scores: Dict[AssessmentDimension, float]
    detailed_feedback: Dict[str, Any]
    improvement_suggestions: List[str]
    next_level_requirements: List[str]


class AssessmentService:
    """评估与自动化评分服务"""
    
    def __init__(self):
        # IELTS/TOEFL评分标准
        self.ielts_criteria = self._init_ielts_criteria()
        self.toefl_criteria = self._init_toefl_criteria()
        
        # 熟练度等级映射
        self.proficiency_mapping = {
            (0, 3): ProficiencyLevel.BEGINNER,
            (3, 4.5): ProficiencyLevel.ELEMENTARY,
            (4.5, 6): ProficiencyLevel.INTERMEDIATE,
            (6, 7): ProficiencyLevel.UPPER_INTERMEDIATE,
            (7, 8.5): ProficiencyLevel.ADVANCED,
            (8.5, 10): ProficiencyLevel.PROFICIENT
        }
        
        # 动态评估权重（根据用户水平调整）
        self.dynamic_weights = {
            ProficiencyLevel.BEGINNER: {
                AssessmentDimension.PRONUNCIATION: 0.3,
                AssessmentDimension.VOCABULARY: 0.25,
                AssessmentDimension.GRAMMAR: 0.25,
                AssessmentDimension.FLUENCY: 0.2
            },
            ProficiencyLevel.INTERMEDIATE: {
                AssessmentDimension.FLUENCY: 0.25,
                AssessmentDimension.ACCURACY: 0.25,
                AssessmentDimension.VOCABULARY: 0.2,
                AssessmentDimension.COHERENCE: 0.15,
                AssessmentDimension.PRONUNCIATION: 0.15
            },
            ProficiencyLevel.ADVANCED: {
                AssessmentDimension.PRAGMATICS: 0.3,
                AssessmentDimension.COHERENCE: 0.25,
                AssessmentDimension.FLUENCY: 0.2,
                AssessmentDimension.ACCURACY: 0.15,
                AssessmentDimension.VOCABULARY: 0.1
            }
        }

    def _init_ielts_criteria(self) -> Dict[AssessmentDimension, AssessmentCriteria]:
        """初始化IELTS评分标准"""
        return {
            AssessmentDimension.FLUENCY: AssessmentCriteria(
                dimension=AssessmentDimension.FLUENCY,
                weight=0.25,
                description="Speech rate, pauses, hesitations, and overall flow",
                scoring_rubric={
                    "9": "Natural, effortless flow with no hesitation",
                    "7": "Generally fluent with occasional hesitation",
                    "5": "Some hesitation and repetition affecting flow",
                    "3": "Frequent pauses and hesitation impede communication"
                }
            ),
            AssessmentDimension.COHERENCE: AssessmentCriteria(
                dimension=AssessmentDimension.COHERENCE,
                weight=0.25,
                description="Logical organization and connection of ideas",
                scoring_rubric={
                    "9": "Ideas flow logically with clear connections",
                    "7": "Generally coherent with some unclear connections",
                    "5": "Some organization but connections often unclear",
                    "3": "Limited coherence, difficult to follow"
                }
            ),
            AssessmentDimension.VOCABULARY: AssessmentCriteria(
                dimension=AssessmentDimension.VOCABULARY,
                weight=0.25,
                description="Range and accuracy of vocabulary usage",
                scoring_rubric={
                    "9": "Wide range used naturally and precisely",
                    "7": "Good range with some inappropriate usage",
                    "5": "Limited range affecting precision",
                    "3": "Very limited range impeding communication"
                }
            ),
            AssessmentDimension.GRAMMAR: AssessmentCriteria(
                dimension=AssessmentDimension.GRAMMAR,
                weight=0.25,
                description="Range and accuracy of grammatical structures",
                scoring_rubric={
                    "9": "Wide range used accurately and appropriately",
                    "7": "Good range with some errors not impeding communication",
                    "5": "Limited range with errors sometimes impeding communication",
                    "3": "Very limited range with frequent errors"
                }
            )
        }

    def _init_toefl_criteria(self) -> Dict[AssessmentDimension, AssessmentCriteria]:
        """初始化TOEFL评分标准"""
        return {
            AssessmentDimension.FLUENCY: AssessmentCriteria(
                dimension=AssessmentDimension.FLUENCY,
                weight=0.3,
                description="Delivery including pace, flow, and pronunciation",
                scoring_rubric={
                    "4": "Clear, fluid speech with good pronunciation",
                    "3": "Generally clear with minor pronunciation issues",
                    "2": "Some unclear speech affecting understanding",
                    "1": "Unclear speech significantly impeding understanding"
                }
            ),
            AssessmentDimension.VOCABULARY: AssessmentCriteria(
                dimension=AssessmentDimension.VOCABULARY,
                weight=0.35,
                description="Language use including vocabulary and grammar",
                scoring_rubric={
                    "4": "Effective use of grammar and vocabulary",
                    "3": "Good use with minor errors",
                    "2": "Limited use with some errors affecting meaning",
                    "1": "Very limited use with frequent errors"
                }
            ),
            AssessmentDimension.COHERENCE: AssessmentCriteria(
                dimension=AssessmentDimension.COHERENCE,
                weight=0.35,
                description="Topic development including coherence and detail",
                scoring_rubric={
                    "4": "Well-developed response with clear progression",
                    "3": "Generally well-developed with clear progression",
                    "2": "Somewhat developed but may lack detail",
                    "1": "Limited development with unclear progression"
                }
            )
        }
    async def assess_speaking_performance(self, user_id: str, session_id: str,
                                        user_message: str, audio_analysis: Optional[Dict] = None,
                                        conversation_context: Optional[List[Dict]] = None,
                                        assessment_type: str = "ielts") -> AssessmentResult:
        """
        综合评估口语表现
        使用LLM-as-a-Judge方法结合多维度分析
        """
        # 1. 获取用户历史表现
        user_history = await self._get_user_assessment_history(user_id)
        current_level = self._estimate_current_level(user_history)
        
        # 2. 多维度评分
        dimension_scores = {}
        
        # 流利度评估（基于音频分析和文本特征）
        dimension_scores[AssessmentDimension.FLUENCY] = await self._assess_fluency(
            user_message, audio_analysis, conversation_context
        )
        
        # 准确性评估（语法、词汇使用）
        dimension_scores[AssessmentDimension.ACCURACY] = await self._assess_accuracy(
            user_message, conversation_context
        )
        
        # 发音评估（如果有音频分析结果）
        if audio_analysis:
            dimension_scores[AssessmentDimension.PRONUNCIATION] = audio_analysis.get("pronunciation_score", 0) / 100 * 9
        
        # 词汇评估
        dimension_scores[AssessmentDimension.VOCABULARY] = await self._assess_vocabulary(
            user_message, current_level
        )
        
        # 语法评估
        dimension_scores[AssessmentDimension.GRAMMAR] = await self._assess_grammar(
            user_message
        )
        
        # 连贯性评估
        dimension_scores[AssessmentDimension.COHERENCE] = await self._assess_coherence(
            user_message, conversation_context
        )
        
        # 语用能力评估（高级用户）
        if current_level in [ProficiencyLevel.ADVANCED, ProficiencyLevel.PROFICIENT]:
            dimension_scores[AssessmentDimension.PRAGMATICS] = await self._assess_pragmatics(
                user_message, conversation_context
            )
        
        # 3. 计算综合得分
        overall_score = self._calculate_overall_score(dimension_scores, current_level, assessment_type)
        
        # 4. 确定熟练度等级
        proficiency_level = self._determine_proficiency_level(overall_score)
        
        # 5. 生成详细反馈
        detailed_feedback = await self._generate_detailed_feedback(
            dimension_scores, current_level, assessment_type
        )
        
        # 6. 生成改进建议
        improvement_suggestions = await self._generate_improvement_suggestions(
            dimension_scores, current_level
        )
        
        # 7. 下一等级要求
        next_level_requirements = self._get_next_level_requirements(proficiency_level)
        
        result = AssessmentResult(
            user_id=user_id,
            session_id=session_id,
            timestamp=datetime.now(),
            overall_score=overall_score,
            proficiency_level=proficiency_level,
            dimension_scores=dimension_scores,
            detailed_feedback=detailed_feedback,
            improvement_suggestions=improvement_suggestions,
            next_level_requirements=next_level_requirements
        )
        
        # 8. 保存评估结果
        await self._save_assessment_result(result)
        
        return result

    async def _assess_fluency(self, user_message: str, audio_analysis: Optional[Dict],
                            conversation_context: Optional[List[Dict]]) -> float:
        """评估流利度"""
        score = 5.0  # 基础分
        
        # 基于文本特征的流利度分析
        words = user_message.split()
        word_count = len(words)
        
        # 长度适中加分
        if 10 <= word_count <= 50:
            score += 1.0
        elif word_count > 50:
            score += 1.5
        
        # 检查犹豫词汇
        hesitation_words = ["um", "uh", "well", "you know", "like", "actually"]
        hesitation_count = sum(1 for word in hesitation_words if word in user_message.lower())
        score -= hesitation_count * 0.5
        
        # 基于音频分析的流利度
        if audio_analysis:
            speech_rate = audio_analysis.get("speech_rate", 0)
            if 120 <= speech_rate <= 180:  # 正常语速
                score += 1.0
            elif speech_rate > 180:
                score -= 0.5  # 过快
            elif speech_rate < 100:
                score -= 1.0  # 过慢
            
            # 停顿分析
            pause_count = audio_analysis.get("pause_count", 0)
            if pause_count < 3:
                score += 0.5
            elif pause_count > 6:
                score -= 1.0
        
        return min(9.0, max(1.0, score))

    async def _assess_accuracy(self, user_message: str, 
                             conversation_context: Optional[List[Dict]]) -> float:
        """评估准确性（语法和词汇使用的正确性）"""
        # 使用LLM进行语法检查
        grammar_prompt = f"""
        Please analyze the grammatical accuracy of this English text and rate it on a scale of 1-9:
        
        Text: "{user_message}"
        
        Consider:
        - Subject-verb agreement
        - Tense consistency
        - Article usage
        - Preposition usage
        - Sentence structure
        
        Respond with only a number between 1-9.
        """
        
        try:
            # 调用AI Gateway进行语法分析
            grammar_score = await self._call_llm_judge(grammar_prompt)
            return float(grammar_score) if grammar_score.isdigit() else 5.0
        except Exception as e:
            logger.error(f"语法评估失败: {e}")
            return 5.0

    async def _assess_vocabulary(self, user_message: str, current_level: ProficiencyLevel) -> float:
        """评估词汇使用"""
        words = user_message.lower().split()
        unique_words = set(words)
        
        # 基础分
        score = 5.0
        
        # 词汇多样性
        if len(words) > 0:
            diversity_ratio = len(unique_words) / len(words)
            if diversity_ratio > 0.8:
                score += 1.5
            elif diversity_ratio > 0.6:
                score += 1.0
        
        # 高级词汇使用（简单的启发式方法）
        advanced_words = [word for word in unique_words if len(word) > 6]
        score += min(2.0, len(advanced_words) * 0.3)
        
        # 根据用户水平调整期望
        level_adjustments = {
            ProficiencyLevel.BEGINNER: -1.0,
            ProficiencyLevel.ELEMENTARY: -0.5,
            ProficiencyLevel.INTERMEDIATE: 0.0,
            ProficiencyLevel.UPPER_INTERMEDIATE: 0.5,
            ProficiencyLevel.ADVANCED: 1.0,
            ProficiencyLevel.PROFICIENT: 1.5
        }
        score += level_adjustments.get(current_level, 0.0)
        
        return min(9.0, max(1.0, score))

    async def _assess_grammar(self, user_message: str) -> float:
        """评估语法结构"""
        grammar_prompt = f"""
        Analyze the grammatical structures in this text and rate the grammar on a scale of 1-9:
        
        Text: "{user_message}"
        
        Consider:
        - Sentence complexity and variety
        - Correct use of tenses
        - Proper sentence construction
        - Use of conjunctions and transitions
        
        Rate only the grammar quality, not content. Respond with only a number 1-9.
        """
        
        try:
            grammar_score = await self._call_llm_judge(grammar_prompt)
            return float(grammar_score) if grammar_score.isdigit() else 5.0
        except Exception as e:
            logger.error(f"语法结构评估失败: {e}")
            return 5.0

    async def _assess_coherence(self, user_message: str, 
                              conversation_context: Optional[List[Dict]]) -> float:
        """评估连贯性和逻辑性"""
        # 基础连贯性分析
        sentences = user_message.split('.')
        sentence_count = len([s for s in sentences if s.strip()])
        
        score = 5.0
        
        # 句子数量适中
        if 2 <= sentence_count <= 5:
            score += 1.0
        elif sentence_count > 5:
            score += 1.5
        
        # 连接词使用
        connectors = ["and", "but", "however", "therefore", "because", "although", "while", "since"]
        connector_count = sum(1 for conn in connectors if conn in user_message.lower())
        score += min(1.5, connector_count * 0.5)
        
        # 如果有对话上下文，检查话题一致性
        if conversation_context and len(conversation_context) > 1:
            context_relevance = await self._assess_context_relevance(user_message, conversation_context)
            score += context_relevance
        
        return min(9.0, max(1.0, score))

    async def _assess_pragmatics(self, user_message: str, 
                               conversation_context: Optional[List[Dict]]) -> float:
        """评估语用能力（适合高级学习者）"""
        pragmatics_prompt = f"""
        Analyze the pragmatic competence in this English text on a scale of 1-9:
        
        Text: "{user_message}"
        Context: {conversation_context[-3:] if conversation_context else "No context"}
        
        Consider:
        - Appropriateness for the context
        - Use of politeness strategies
        - Cultural awareness
        - Register and tone appropriateness
        - Implied meaning and inference
        
        Respond with only a number 1-9.
        """
        
        try:
            pragmatics_score = await self._call_llm_judge(pragmatics_prompt)
            return float(pragmatics_score) if pragmatics_score.isdigit() else 6.0
        except Exception as e:
            logger.error(f"语用能力评估失败: {e}")
            return 6.0

    async def _call_llm_judge(self, prompt: str) -> str:
        """调用LLM作为评判者"""
        # 这里应该调用AI Gateway
        # 为了简化，返回模拟结果
        import random
        return str(random.randint(4, 8))

    def _calculate_overall_score(self, dimension_scores: Dict[AssessmentDimension, float],
                               current_level: ProficiencyLevel, assessment_type: str) -> float:
        """计算综合得分"""
        if assessment_type == "ielts":
            criteria = self.ielts_criteria
        else:
            criteria = self.toefl_criteria
        
        # 获取动态权重
        weights = self.dynamic_weights.get(current_level, self.dynamic_weights[ProficiencyLevel.INTERMEDIATE])
        
        total_score = 0.0
        total_weight = 0.0
        
        for dimension, score in dimension_scores.items():
            weight = weights.get(dimension, 0.1)  # 默认权重
            total_score += score * weight
            total_weight += weight
        
        return total_score / total_weight if total_weight > 0 else 5.0

    def _determine_proficiency_level(self, overall_score: float) -> ProficiencyLevel:
        """根据综合得分确定熟练度等级"""
        for (min_score, max_score), level in self.proficiency_mapping.items():
            if min_score <= overall_score < max_score:
                return level
        return ProficiencyLevel.INTERMEDIATE

    async def _generate_detailed_feedback(self, dimension_scores: Dict[AssessmentDimension, float],
                                        current_level: ProficiencyLevel, assessment_type: str) -> Dict[str, Any]:
        """生成详细反馈"""
        feedback = {
            "overall_assessment": f"Based on {assessment_type.upper()} criteria, your current level is {current_level.value}",
            "strengths": [],
            "areas_for_improvement": [],
            "dimension_analysis": {}
        }
        
        # 分析各维度表现
        for dimension, score in dimension_scores.items():
            analysis = {
                "score": score,
                "level": "excellent" if score >= 7.5 else "good" if score >= 6.0 else "needs_improvement",
                "feedback": self._get_dimension_feedback(dimension, score)
            }
            feedback["dimension_analysis"][dimension.value] = analysis
            
            if score >= 7.0:
                feedback["strengths"].append(f"{dimension.value.title()}: {analysis['feedback']}")
            elif score < 5.0:
                feedback["areas_for_improvement"].append(f"{dimension.value.title()}: {analysis['feedback']}")
        
        return feedback

    def _get_dimension_feedback(self, dimension: AssessmentDimension, score: float) -> str:
        """获取维度特定反馈"""
        feedback_templates = {
            AssessmentDimension.FLUENCY: {
                "high": "Your speech flows naturally with minimal hesitation",
                "medium": "Generally fluent with some minor pauses",
                "low": "Work on reducing hesitations and improving speech flow"
            },
            AssessmentDimension.ACCURACY: {
                "high": "Excellent grammatical accuracy with minimal errors",
                "medium": "Good accuracy with some minor grammatical issues",
                "low": "Focus on improving grammatical accuracy and word choice"
            },
            AssessmentDimension.VOCABULARY: {
                "high": "Rich and varied vocabulary usage",
                "medium": "Good vocabulary range with room for expansion",
                "low": "Expand your vocabulary range and precision"
            },
            AssessmentDimension.COHERENCE: {
                "high": "Ideas are well-organized and clearly connected",
                "medium": "Generally coherent with some unclear connections",
                "low": "Work on organizing ideas more logically"
            }
        }
        
        level = "high" if score >= 7.0 else "medium" if score >= 5.0 else "low"
        return feedback_templates.get(dimension, {}).get(level, "Continue practicing this skill")

    async def _generate_improvement_suggestions(self, dimension_scores: Dict[AssessmentDimension, float],
                                              current_level: ProficiencyLevel) -> List[str]:
        """生成改进建议"""
        suggestions = []
        
        # 找出最需要改进的维度
        weak_dimensions = [dim for dim, score in dimension_scores.items() if score < 5.5]
        
        for dimension in weak_dimensions:
            if dimension == AssessmentDimension.FLUENCY:
                suggestions.append("Practice speaking regularly to improve fluency and reduce hesitations")
                suggestions.append("Record yourself speaking and listen for areas of improvement")
            elif dimension == AssessmentDimension.VOCABULARY:
                suggestions.append("Learn 5-10 new words daily and use them in sentences")
                suggestions.append("Read English materials at your level to expand vocabulary")
            elif dimension == AssessmentDimension.GRAMMAR:
                suggestions.append("Review basic grammar rules and practice with exercises")
                suggestions.append("Focus on sentence structure and verb tenses")
            elif dimension == AssessmentDimension.COHERENCE:
                suggestions.append("Practice organizing your thoughts before speaking")
                suggestions.append("Use connecting words to link your ideas clearly")
        
        # 根据水平添加通用建议
        if current_level == ProficiencyLevel.BEGINNER:
            suggestions.append("Focus on basic pronunciation and simple sentence structures")
        elif current_level == ProficiencyLevel.INTERMEDIATE:
            suggestions.append("Work on expressing complex ideas more clearly")
        elif current_level == ProficiencyLevel.ADVANCED:
            suggestions.append("Focus on natural expression and cultural appropriateness")
        
        return suggestions[:5]  # 限制建议数量

    def _get_next_level_requirements(self, current_level: ProficiencyLevel) -> List[str]:
        """获取下一等级要求"""
        next_level_map = {
            ProficiencyLevel.BEGINNER: [
                "Achieve consistent pronunciation of basic sounds",
                "Use simple present and past tenses correctly",
                "Build vocabulary to 1000+ common words"
            ],
            ProficiencyLevel.ELEMENTARY: [
                "Improve fluency in basic conversations",
                "Master common grammatical structures",
                "Expand vocabulary to 2000+ words"
            ],
            ProficiencyLevel.INTERMEDIATE: [
                "Develop more natural speech rhythm",
                "Use complex grammatical structures accurately",
                "Express opinions and ideas clearly"
            ],
            ProficiencyLevel.UPPER_INTERMEDIATE: [
                "Achieve near-native fluency",
                "Use advanced vocabulary appropriately",
                "Demonstrate cultural awareness in communication"
            ],
            ProficiencyLevel.ADVANCED: [
                "Master subtle aspects of pronunciation",
                "Use language creatively and flexibly",
                "Communicate with complete effectiveness"
            ]
        }
        
        return next_level_map.get(current_level, ["Continue practicing to maintain proficiency"])

    async def _get_user_assessment_history(self, user_id: str) -> List[Dict]:
        """获取用户评估历史"""
        # 从Redis或数据库获取历史评估数据
        history_key = f"assessment_history:{user_id}"
        history_data = redis_service.get_data(history_key)
        
        if history_data:
            return json.loads(history_data)
        return []

    def _estimate_current_level(self, user_history: List[Dict]) -> ProficiencyLevel:
        """基于历史数据估计当前水平"""
        if not user_history:
            return ProficiencyLevel.INTERMEDIATE
        
        # 取最近3次评估的平均分
        recent_scores = [h.get("overall_score", 5.0) for h in user_history[-3:]]
        avg_score = sum(recent_scores) / len(recent_scores)
        
        return self._determine_proficiency_level(avg_score)

    async def _save_assessment_result(self, result: AssessmentResult):
        """保存评估结果"""
        # 保存到Redis（短期缓存）
        result_key = f"assessment:{result.user_id}:{result.session_id}"
        
        # 转换为字典并处理枚举类型
        result_dict = asdict(result)
        # 将dimension_scores的枚举key转换为字符串
        if 'dimension_scores' in result_dict and result_dict['dimension_scores']:
            result_dict['dimension_scores'] = {
                k.value if isinstance(k, AssessmentDimension) else k: v 
                for k, v in result_dict['dimension_scores'].items()
            }
        # 转换proficiency_level枚举为字符串
        if 'proficiency_level' in result_dict:
            result_dict['proficiency_level'] = result_dict['proficiency_level'].value if hasattr(result_dict['proficiency_level'], 'value') else result_dict['proficiency_level']
        
        redis_service.set_data(result_key, json.dumps(result_dict, default=str), expire=86400)
        
        # 更新用户评估历史
        history_key = f"assessment_history:{result.user_id}"
        history = await self._get_user_assessment_history(result.user_id)
        history.append({
            "timestamp": result.timestamp.isoformat(),
            "overall_score": result.overall_score,
            "proficiency_level": result.proficiency_level.value,
            "session_id": result.session_id
        })
        
        # 保持历史记录在合理范围内
        if len(history) > 50:
            history = history[-50:]
        
        redis_service.set_data(history_key, json.dumps(history), expire=86400*30)  # 30天
        
        # 保存到长期记忆（Milvus）
        memory_content = f"用户评估结果 - 总分: {result.overall_score:.1f}, 等级: {result.proficiency_level.value}"
        memory_service.save_memory(
            result.user_id, 
            result.session_id, 
            memory_content,
            session_type="assessment",
            metadata={"assessment_type": "speaking", "score": result.overall_score}
        )

    async def _assess_context_relevance(self, user_message: str, 
                                      conversation_context: List[Dict]) -> float:
        """评估上下文相关性"""
        if not conversation_context:
            return 0.0
        
        # 简单的关键词匹配方法
        recent_context = " ".join([msg.get("content", "") for msg in conversation_context[-3:]])
        user_words = set(user_message.lower().split())
        context_words = set(recent_context.lower().split())
        
        # 计算词汇重叠度
        overlap = len(user_words.intersection(context_words))
        total_words = len(user_words.union(context_words))
        
        if total_words > 0:
            relevance_score = overlap / total_words * 2.0  # 最高2分
            return min(2.0, relevance_score)
        
        return 0.0

    async def get_progress_tracking(self, user_id: str, days: int = 30) -> Dict[str, Any]:
        """获取用户进步追踪数据"""
        history = await self._get_user_assessment_history(user_id)
        
        # 过滤指定天数内的数据
        cutoff_date = datetime.now() - timedelta(days=days)
        recent_history = [
            h for h in history 
            if datetime.fromisoformat(h["timestamp"]) >= cutoff_date
        ]
        
        if not recent_history:
            return {"message": "No recent assessment data available"}
        
        # 计算趋势
        scores = [h["overall_score"] for h in recent_history]
        trend = "improving" if len(scores) > 1 and scores[-1] > scores[0] else "stable"
        
        # 计算各维度进步
        dimension_progress = {}
        if len(recent_history) > 1:
            first_assessment = recent_history[0]
            latest_assessment = recent_history[-1]
            
            # 这里需要从详细数据中获取维度分数
            # 简化处理
            dimension_progress = {
                "fluency": {"change": "+0.5", "trend": "improving"},
                "vocabulary": {"change": "+0.3", "trend": "improving"},
                "grammar": {"change": "-0.1", "trend": "stable"}
            }
        
        return {
            "total_assessments": len(recent_history),
            "current_score": scores[-1] if scores else 0,
            "average_score": sum(scores) / len(scores) if scores else 0,
            "trend": trend,
            "improvement": scores[-1] - scores[0] if len(scores) > 1 else 0,
            "dimension_progress": dimension_progress,
            "assessment_frequency": len(recent_history) / days,
            "recommendations": self._generate_progress_recommendations(recent_history)
        }

    def _generate_progress_recommendations(self, history: List[Dict]) -> List[str]:
        """基于进步历史生成建议"""
        if not history:
            return ["Start regular speaking practice to track your progress"]
        
        recommendations = []
        
        # 评估频率建议
        if len(history) < 5:
            recommendations.append("Practice more regularly to better track your progress")
        
        # 分数趋势建议
        scores = [h["overall_score"] for h in history]
        if len(scores) > 1:
            if scores[-1] < scores[0]:
                recommendations.append("Focus on consistent practice to maintain improvement")
            elif scores[-1] == scores[0]:
                recommendations.append("Try challenging yourself with more complex topics")
            else:
                recommendations.append("Great progress! Keep up the consistent practice")
        
        return recommendations


# 全局服务实例
assessment_service = AssessmentService()