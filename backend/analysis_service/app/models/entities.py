"""
数据库实体模型
"""
from sqlalchemy import Column, Integer, BigInteger, String, Float, Text, DateTime, JSON, Boolean
from sqlalchemy.sql import func
from app.database import Base


class AnalysisReport(Base):
    """分析报告表"""
    __tablename__ = "analysis_report"

    id = Column(BigInteger, primary_key=True, autoincrement=True)
    session_id = Column(BigInteger, nullable=False, index=True, comment="会话ID")
    message_id = Column(BigInteger, nullable=True, comment="消息ID")
    user_id = Column(BigInteger, nullable=False, index=True, comment="用户ID")
    grammar_score = Column(Float, default=0, comment="语法评分")
    pronunciation_score = Column(Float, default=0, comment="发音评分")
    fluency_score = Column(Float, default=0, comment="流利度评分")
    overall_score = Column(Float, default=0, comment="综合评分")
    native_expression_suggestion = Column(Text, nullable=True, comment="地道表达建议")
    detailed_feedback = Column(JSON, nullable=True, comment="详细反馈JSON")
    created_at = Column(DateTime, server_default=func.now(), comment="创建时间")
    deleted = Column(Boolean, default=False, comment="逻辑删除")


class LearningRecord(Base):
    """学习记录表（每日汇总）"""
    __tablename__ = "learning_record"

    id = Column(BigInteger, primary_key=True, autoincrement=True)
    user_id = Column(BigInteger, nullable=False, index=True, comment="用户ID")
    record_date = Column(String(10), nullable=False, comment="记录日期 yyyy-MM-dd")
    practice_count = Column(Integer, default=0, comment="练习次数")
    practice_duration = Column(Integer, default=0, comment="练习时长(秒)")
    avg_grammar_score = Column(Float, default=0, comment="平均语法分")
    avg_pronunciation_score = Column(Float, default=0, comment="平均发音分")
    avg_fluency_score = Column(Float, default=0, comment="平均流利度分")
    avg_overall_score = Column(Float, default=0, comment="平均综合分")
    top_errors = Column(JSON, nullable=True, comment="常见错误Top5")
    created_at = Column(DateTime, server_default=func.now())
    updated_at = Column(DateTime, server_default=func.now(), onupdate=func.now())

    class Meta:
        unique_together = ("user_id", "record_date")


class UserRanking(Base):
    """用户排名表（缓存快照）"""
    __tablename__ = "user_ranking"

    id = Column(BigInteger, primary_key=True, autoincrement=True)
    user_id = Column(BigInteger, nullable=False, unique=True, index=True, comment="用户ID")
    total_practice_count = Column(Integer, default=0, comment="总练习次数")
    total_practice_duration = Column(Integer, default=0, comment="总练习时长(秒)")
    avg_overall_score = Column(Float, default=0, comment="平均综合分")
    ranking_score = Column(Float, default=0, comment="排名分数(加权计算)")
    current_rank = Column(Integer, default=0, comment="当前排名")
    updated_at = Column(DateTime, server_default=func.now(), onupdate=func.now())
