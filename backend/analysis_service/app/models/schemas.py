"""
Pydantic数据模型（请求/响应）
"""
from pydantic import BaseModel
from typing import Optional, List, Any
from datetime import datetime


# ===== 统一响应 =====
class Result(BaseModel):
    """统一响应格式"""
    code: int = 200
    msg: str = "success"
    data: Any = None

    @staticmethod
    def success(data=None, msg="success"):
        return Result(code=200, msg=msg, data=data)

    @staticmethod
    def error(code=500, msg="error"):
        return Result(code=code, msg=msg, data=None)


# ===== 分析报告 =====
class AnalysisReportVO(BaseModel):
    id: int
    session_id: int
    message_id: Optional[int] = None
    user_id: int
    grammar_score: float
    pronunciation_score: float
    fluency_score: float
    overall_score: float
    native_expression_suggestion: Optional[str] = None
    detailed_feedback: Optional[dict] = None
    created_at: Optional[datetime] = None

    class Config:
        from_attributes = True


# ===== 学习记录 =====
class LearningRecordVO(BaseModel):
    id: int
    user_id: int
    record_date: str
    practice_count: int
    practice_duration: int
    avg_grammar_score: float
    avg_pronunciation_score: float
    avg_fluency_score: float
    avg_overall_score: float
    top_errors: Optional[list] = None

    class Config:
        from_attributes = True


# ===== 排名 =====
class RankingVO(BaseModel):
    user_id: int
    total_practice_count: int = 0
    total_practice_duration: int = 0
    avg_overall_score: float = 0
    ranking_score: float = 0
    current_rank: int = 0


# ===== 统计概览 =====
class AnalysisOverview(BaseModel):
    total_reports: int = 0
    total_users: int = 0
    avg_grammar: float = 0
    avg_pronunciation: float = 0
    avg_fluency: float = 0
    avg_overall: float = 0


# ===== 用户统计 =====
class UserStatistics(BaseModel):
    user_id: int
    total_sessions: int = 0
    total_messages: int = 0
    total_duration: int = 0
    avg_grammar_score: float = 0
    avg_pronunciation_score: float = 0
    avg_fluency_score: float = 0
    avg_overall_score: float = 0
    score_trend: Optional[List[dict]] = None
    top_errors: Optional[List[dict]] = None


# ===== 报表导出请求 =====
class ExportRequest(BaseModel):
    user_id: Optional[int] = None
    start_date: Optional[str] = None
    end_date: Optional[str] = None
    format: str = "xlsx"  # xlsx 或 csv
