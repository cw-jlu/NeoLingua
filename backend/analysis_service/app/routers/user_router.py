"""
用户端分析API
前缀: /user/analysis
"""
from fastapi import APIRouter, Depends, Query, UploadFile, File, Form
from fastapi.responses import StreamingResponse
from sqlalchemy.orm import Session
from app.database import get_db
from app.models.schemas import Result, AnalysisReportVO
from app.services.analysis_service import analysis_business_service
from app.services.export_service import export_service
from app.services.redis_service import redis_service
from app.services.audio_storage import audio_storage
from app.services import stt_service
import uuid
import logging

logger = logging.getLogger(__name__)

router = APIRouter(prefix="/user/analysis", tags=["用户端-分析"])


@router.get("/reports")
async def get_my_reports(
    user_id: int = Query(..., description="用户ID"),
    page: int = Query(1, ge=1),
    size: int = Query(20, ge=1, le=100),
    db: Session = Depends(get_db)
):
    """获取我的分析报告列表"""
    data = analysis_business_service.get_reports_by_user(db, user_id, page, size)
    return Result.success(data)


@router.get("/reports/session/{session_id}")
async def get_session_reports(
    session_id: int,
    page: int = Query(1, ge=1),
    size: int = Query(20, ge=1, le=100),
    db: Session = Depends(get_db)
):
    """获取会话的分析报告"""
    data = analysis_business_service.get_reports_by_session(db, session_id, page, size)
    return Result.success(data)


@router.get("/statistics")
async def get_my_statistics(
    user_id: int = Query(..., description="用户ID"),
    db: Session = Depends(get_db)
):
    """获取我的统计数据"""
    data = analysis_business_service.get_user_statistics(db, user_id)
    return Result.success(data)


@router.get("/ranking")
async def get_ranking(top: int = Query(50, ge=1, le=200)):
    """获取排名榜"""
    rankings = await redis_service.get_top_rankings(top)
    return Result.success(rankings)


@router.get("/ranking/me")
async def get_my_rank(user_id: int = Query(...)):
    """获取我的排名"""
    rank = await redis_service.get_user_rank(user_id)
    total = await redis_service.get_ranking_count()
    return Result.success({"rank": rank, "total": total})


@router.get("/export")
async def export_report(
    user_id: int = Query(...),
    start_date: str = Query(None),
    end_date: str = Query(None),
    format: str = Query("xlsx"),
    db: Session = Depends(get_db)
):
    """导出分析报表"""
    buffer = export_service.export_user_report(db, user_id, start_date, end_date, format)
    if not buffer:
        return Result.error(code=404, msg="无数据可导出")
    media_type = "text/csv" if format == "csv" else "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
    ext = "csv" if format == "csv" else "xlsx"
    return StreamingResponse(
        buffer,
        media_type=media_type,
        headers={"Content-Disposition": f"attachment; filename=report_{user_id}.{ext}"}
    )


@router.post("/audio/upload")
async def upload_audio(
    audio: UploadFile = File(..., description="音频文件 (WAV/WebM/MP3)"),
    session_id: str = Form(..., description="练习会话ID"),
    user_id: str = Form(..., description="用户ID"),
    language: str = Form(default="en", description="语言代码")
):
    """
    上传音频：
    1. 保存到 MinIO
    2. 做 STT 识别
    3. 返回 { audio_url, text, language }
    前端拿到结果后把 text 显示为用户消息，把 audio_url + text 一起发给 practice_service
    """
    try:
        audio_bytes = await audio.read()
        if len(audio_bytes) < 500:
            return Result.error(code=400, msg="音频文件太短或为空")

        # 确定文件后缀
        content_type = audio.content_type or ""
        if "webm" in content_type or (audio.filename or "").endswith(".webm"):
            suffix = ".webm"
        elif "mp3" in content_type or (audio.filename or "").endswith(".mp3"):
            suffix = ".mp3"
        else:
            suffix = ".wav"

        # 生成唯一文件名并保存到本地临时目录
        file_name = f"{user_id}/{session_id}/{uuid.uuid4().hex}{suffix}"
        local_path = audio_storage.local_storage_path / file_name
        local_path.parent.mkdir(parents=True, exist_ok=True)
        local_path.write_bytes(audio_bytes)

        # 归档到 MinIO
        minio_url = audio_storage.archive_to_minio(local_path, file_name)
        # 对外暴露的 URL：优先用 MinIO，否则用本地相对路径
        audio_url = minio_url if minio_url else f"/audio/{file_name}"

        # STT 识别
        try:
            text, detected_lang = stt_service.transcribe_bytes(audio_bytes, suffix=suffix, language=language)
        except Exception as e:
            logger.warning(f"STT 识别失败，返回空文本: {e}")
            text = ""
            detected_lang = language

        logger.info(f"音频上传完成: user={user_id}, session={session_id}, url={audio_url}, text={text[:50]}")
        return Result.success(data={
            "audio_url": audio_url,
            "text": text,
            "language": detected_lang
        })

    except Exception as e:
        logger.error(f"音频上传失败: {e}", exc_info=True)
        return Result.error(code=500, msg=f"上传失败: {str(e)}")
