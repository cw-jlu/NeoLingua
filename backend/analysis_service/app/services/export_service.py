"""
报表导出服务 - 生成Excel/CSV报表
"""
import io
import logging
from datetime import datetime
from typing import Optional
from sqlalchemy.orm import Session
from sqlalchemy import desc
from app.models.entities import AnalysisReport

logger = logging.getLogger(__name__)


class ExportService:
    """报表导出服务"""

    def export_user_report(self, db: Session, user_id: int,
                           start_date: str = None, end_date: str = None,
                           fmt: str = "xlsx") -> Optional[io.BytesIO]:
        """导出用户分析报表"""
        try:
            import pandas as pd

            query = db.query(AnalysisReport).filter(
                AnalysisReport.user_id == user_id,
                AnalysisReport.deleted == False
            )
            if start_date:
                query = query.filter(AnalysisReport.created_at >= start_date)
            if end_date:
                query = query.filter(AnalysisReport.created_at <= end_date)

            records = query.order_by(desc(AnalysisReport.created_at)).all()
            if not records:
                return None

            data = [{
                "报告ID": r.id,
                "会话ID": r.session_id,
                "语法分": r.grammar_score,
                "发音分": r.pronunciation_score,
                "流利度分": r.fluency_score,
                "综合分": r.overall_score,
                "建议": r.native_expression_suggestion or "",
                "时间": str(r.created_at)
            } for r in records]

            df = pd.DataFrame(data)
            buffer = io.BytesIO()

            if fmt == "csv":
                df.to_csv(buffer, index=False, encoding="utf-8-sig")
            else:
                df.to_excel(buffer, index=False, engine="openpyxl")

            buffer.seek(0)
            return buffer
        except Exception as e:
            logger.error(f"报表导出失败: {e}")
            return None


export_service = ExportService()
