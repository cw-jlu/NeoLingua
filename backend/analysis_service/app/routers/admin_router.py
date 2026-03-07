"""
管理端分析API
前缀: /admin/analysis
"""
from fastapi import APIRouter, Depends, Query
from sqlalchemy.orm import Session
from app.database import get_db
from app.models.schemas import Result
from app.services.analysis_service import analysis_business_service
from app.services.redis_service import redis_service

router = APIRouter(prefix="/admin/analysis", tags=["管理端-分析"])


@router.get("/reports")
async def get_all_reports(
    keyword: str = Query(None, description="用户ID搜索"),
    page: int = Query(1, ge=1),
    size: int = Query(20, ge=1, le=100),
    db: Session = Depends(get_db)
):
    """获取所有分析报告"""
    data = analysis_business_service.get_all_reports(db, keyword, page, size)
    return Result.success(data)


@router.get("/reports/{report_id}")
async def get_report_detail(report_id: int, db: Session = Depends(get_db)):
    """获取报告详情"""
    from app.models.entities import AnalysisReport
    report = db.query(AnalysisReport).filter(AnalysisReport.id == report_id).first()
    if not report:
        return Result.error(code=404, msg="报告不存在")
    return Result.success(report)


@router.delete("/reports/{report_id}")
async def delete_report(report_id: int, db: Session = Depends(get_db)):
    """删除分析报告"""
    success = analysis_business_service.delete_report(db, report_id)
    if not success:
        return Result.error(code=404, msg="报告不存在")
    return Result.success(msg="删除成功")


@router.get("/overview")
async def get_overview(db: Session = Depends(get_db)):
    """获取分析概览统计"""
    data = analysis_business_service.get_overview(db)
    return Result.success(data)


@router.get("/user/{user_id}/statistics")
async def get_user_statistics(user_id: int, db: Session = Depends(get_db)):
    """获取指定用户的统计数据"""
    data = analysis_business_service.get_user_statistics(db, user_id)
    return Result.success(data)


@router.get("/ranking")
async def get_ranking(top: int = Query(50, ge=1, le=500)):
    """获取排名榜（管理端）"""
    rankings = await redis_service.get_top_rankings(top)
    return Result.success(rankings)


@router.get("/statistics")
async def get_statistics(db: Session = Depends(get_db)):
    """获取分析服务统计（管理端仪表盘用）"""
    overview = analysis_business_service.get_overview(db)
    ranking_count = await redis_service.get_ranking_count()
    overview["ranking_users"] = ranking_count
    return Result.success(overview)
