"""
分析服务 - 核心业务逻辑
处理对话消息分析、学习记录汇总、排名计算
"""
import logging
from datetime import datetime, date
from typing import Optional, List, Dict, Any
from sqlalchemy.orm import Session
from sqlalchemy import func, desc
from app.models.entities import AnalysisReport, LearningRecord, UserRanking
from app.services.pronunciation_service import pronunciation_service
from app.services.audio_storage import audio_storage
from app.services.redis_service import redis_service

logger = logging.getLogger(__name__)


class AnalysisBusinessService:
    """分析业务服务"""

    async def analyze_message(self, db: Session, session_id: int, user_id: int,
                              text: str, role: str, message_id: int = None,
                              audio_url: str = None, metadata: dict = None) -> Optional[AnalysisReport]:
        """
        分析单条消息
        - 语法评分（基于规则）
        - 发音评分（通过MFA）
        - 流利度评分
        """
        try:
            grammar_score = self._analyze_grammar(text)
            pronunciation_score = 0.0
            pronunciation_feedback = None

            # 发音分析（仅用户消息且有音频）
            target_audio = audio_url or (metadata.get("audio_url") if metadata else None)
            if role == "user" and target_audio:
                audio_path = audio_storage.resolve_audio_path(target_audio)
                if audio_path:
                    p_result = pronunciation_service.analyze(audio_path, text)
                    pronunciation_score = p_result.get("pronunciation_score", 0)
                    pronunciation_feedback = p_result.get("feedback", "")

            fluency_score = self._analyze_fluency(text)
            overall = 0.3 * grammar_score + 0.4 * pronunciation_score + 0.3 * fluency_score

            # 生成地道表达建议
            suggestion = self._generate_suggestion(text)

            report = AnalysisReport(
                session_id=session_id,
                message_id=message_id,
                user_id=user_id,
                grammar_score=round(grammar_score, 1),
                pronunciation_score=round(pronunciation_score, 1),
                fluency_score=round(fluency_score, 1),
                overall_score=round(overall, 1),
                native_expression_suggestion=suggestion,
                detailed_feedback={
                    "pronunciation_feedback": pronunciation_feedback,
                    "grammar_notes": [],
                    "fluency_notes": []
                }
            )
            db.add(report)
            db.commit()
            db.refresh(report)

            # 更新排名
            await self._update_user_ranking(db, user_id)

            return report
        except Exception as e:
            logger.error(f"消息分析失败: {e}", exc_info=True)
            db.rollback()
            return None

    def _analyze_grammar(self, text: str) -> float:
        """语法分析（基于规则的简单评分）"""
        score = 85.0
        text_lower = text.lower()
        # 常见语法错误检测
        errors = [
            ("i is", -5), ("he have", -5), ("she have", -5),
            ("they is", -5), ("we is", -5), ("i am go", -3),
            ("more better", -3), ("more faster", -3),
        ]
        for pattern, penalty in errors:
            if pattern in text_lower:
                score += penalty
        # 句子长度奖励
        words = text.split()
        if len(words) >= 8:
            score = min(100, score + 5)
        return max(0, min(100, score))

    def _analyze_fluency(self, text: str) -> float:
        """流利度分析"""
        score = 82.0
        words = text.split()
        if len(words) < 3:
            score -= 10
        # 检测重复词（犹豫）
        for i in range(len(words) - 1):
            if words[i].lower() == words[i + 1].lower():
                score -= 3
        # 填充词检测
        fillers = {"um", "uh", "er", "ah", "like", "you know"}
        filler_count = sum(1 for w in words if w.lower() in fillers)
        score -= filler_count * 2
        return max(0, min(100, score))

    def _generate_suggestion(self, text: str) -> Optional[str]:
        """生成地道表达建议"""
        suggestions = []
        text_lower = text.lower()
        patterns = {
            "i want to": "口语中可以用 'I wanna' 更自然",
            "going to": "口语中可以用 'gonna' 更地道",
            "i think that": "可以简化为 'I think' 或用 'I reckon'",
            "very good": "试试 'awesome', 'fantastic', 'brilliant'",
            "very big": "试试 'huge', 'enormous', 'massive'",
        }
        for pattern, tip in patterns.items():
            if pattern in text_lower:
                suggestions.append(tip)
        return "; ".join(suggestions) if suggestions else None

    # ===== 查询功能 =====

    def get_reports_by_session(self, db: Session, session_id: int, page: int = 1, size: int = 20) -> Dict:
        """获取会话的分析报告"""
        query = db.query(AnalysisReport).filter(
            AnalysisReport.session_id == session_id,
            AnalysisReport.deleted == False
        )
        total = query.count()
        records = query.order_by(desc(AnalysisReport.created_at)).offset((page - 1) * size).limit(size).all()
        return {"content": records, "total": total}

    def get_reports_by_user(self, db: Session, user_id: int, page: int = 1, size: int = 20) -> Dict:
        """获取用户的分析报告"""
        query = db.query(AnalysisReport).filter(
            AnalysisReport.user_id == user_id,
            AnalysisReport.deleted == False
        )
        total = query.count()
        records = query.order_by(desc(AnalysisReport.created_at)).offset((page - 1) * size).limit(size).all()
        return {"content": records, "total": total}

    def get_user_statistics(self, db: Session, user_id: int) -> Dict:
        """获取用户统计数据"""
        result = db.query(
            func.count(AnalysisReport.id).label("total"),
            func.avg(AnalysisReport.grammar_score).label("avg_grammar"),
            func.avg(AnalysisReport.pronunciation_score).label("avg_pronunciation"),
            func.avg(AnalysisReport.fluency_score).label("avg_fluency"),
            func.avg(AnalysisReport.overall_score).label("avg_overall"),
        ).filter(
            AnalysisReport.user_id == user_id,
            AnalysisReport.deleted == False
        ).first()

        return {
            "user_id": user_id,
            "total_reports": result.total or 0,
            "avg_grammar_score": round(float(result.avg_grammar or 0), 1),
            "avg_pronunciation_score": round(float(result.avg_pronunciation or 0), 1),
            "avg_fluency_score": round(float(result.avg_fluency or 0), 1),
            "avg_overall_score": round(float(result.avg_overall or 0), 1),
        }

    def get_overview(self, db: Session) -> Dict:
        """获取全局统计概览（管理端）"""
        result = db.query(
            func.count(AnalysisReport.id).label("total_reports"),
            func.count(func.distinct(AnalysisReport.user_id)).label("total_users"),
            func.avg(AnalysisReport.grammar_score).label("avg_grammar"),
            func.avg(AnalysisReport.pronunciation_score).label("avg_pronunciation"),
            func.avg(AnalysisReport.fluency_score).label("avg_fluency"),
            func.avg(AnalysisReport.overall_score).label("avg_overall"),
        ).filter(AnalysisReport.deleted == False).first()

        return {
            "total_reports": result.total_reports or 0,
            "total_users": result.total_users or 0,
            "avg_grammar": round(float(result.avg_grammar or 0), 1),
            "avg_pronunciation": round(float(result.avg_pronunciation or 0), 1),
            "avg_fluency": round(float(result.avg_fluency or 0), 1),
            "avg_overall": round(float(result.avg_overall or 0), 1),
        }

    async def _update_user_ranking(self, db: Session, user_id: int):
        """更新用户排名"""
        stats = self.get_user_statistics(db, user_id)
        # 排名分数 = 综合分 * 0.6 + 练习次数权重 * 0.4
        count_weight = min(100, stats["total_reports"] * 2)
        ranking_score = stats["avg_overall_score"] * 0.6 + count_weight * 0.4
        await redis_service.update_ranking(user_id, ranking_score)

    def delete_report(self, db: Session, report_id: int) -> bool:
        """逻辑删除报告"""
        report = db.query(AnalysisReport).filter(AnalysisReport.id == report_id).first()
        if not report:
            return False
        report.deleted = True
        db.commit()
        return True

    def get_all_reports(self, db: Session, keyword: str = None, page: int = 1, size: int = 20) -> Dict:
        """管理端：获取所有报告"""
        query = db.query(AnalysisReport).filter(AnalysisReport.deleted == False)
        if keyword:
            query = query.filter(AnalysisReport.user_id == int(keyword))
        total = query.count()
        records = query.order_by(desc(AnalysisReport.created_at)).offset((page - 1) * size).limit(size).all()
        return {"content": records, "total": total}


analysis_business_service = AnalysisBusinessService()
