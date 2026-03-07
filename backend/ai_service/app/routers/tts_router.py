"""
TTS路由 - 文字转语音
使用 edge-tts (微软 Edge TTS，免费，无需API Key)
支持多种英语语音，返回 MP3 音频流
"""
from fastapi import APIRouter, Query
from fastapi.responses import StreamingResponse, Response
from pydantic import BaseModel
from loguru import logger
import edge_tts
import io

router = APIRouter(prefix="/ai/tts", tags=["TTS"])

# 可用的英语语音
VOICES = {
    "female_us": "en-US-JennyNeural",      # 美式英语女声（默认）
    "male_us": "en-US-GuyNeural",           # 美式英语男声
    "female_uk": "en-GB-SoniaNeural",       # 英式英语女声
    "male_uk": "en-GB-RyanNeural",          # 英式英语男声
    "female_au": "en-AU-NatashaNeural",     # 澳式英语女声
}

DEFAULT_VOICE = "en-US-JennyNeural"


class TtsRequest(BaseModel):
    text: str
    voice: str = "female_us"   # 对应 VOICES 的 key
    rate: str = "+0%"          # 语速，如 "+20%" 加快，"-20%" 减慢
    volume: str = "+0%"        # 音量


@router.post("/speak")
async def text_to_speech(request: TtsRequest):
    """
    文字转语音，返回 MP3 音频流
    前端可直接用 Audio 对象播放
    """
    try:
        voice = VOICES.get(request.voice, DEFAULT_VOICE)
        communicate = edge_tts.Communicate(
            text=request.text,
            voice=voice,
            rate=request.rate,
            volume=request.volume
        )

        # 收集所有音频数据
        audio_data = io.BytesIO()
        async for chunk in communicate.stream():
            if chunk["type"] == "audio":
                audio_data.write(chunk["data"])

        audio_data.seek(0)
        return Response(
            content=audio_data.read(),
            media_type="audio/mpeg",
            headers={"Content-Disposition": "inline; filename=tts.mp3"}
        )
    except Exception as e:
        logger.error(f"TTS失败: {e}")
        return Response(status_code=500, content=str(e))


@router.get("/voices")
async def get_voices():
    """获取可用语音列表"""
    return {
        "voices": [
            {"key": k, "name": v, "label": {
                "female_us": "美式英语女声",
                "male_us": "美式英语男声",
                "female_uk": "英式英语女声",
                "male_uk": "英式英语男声",
                "female_au": "澳式英语女声",
            }.get(k, k)}
            for k, v in VOICES.items()
        ],
        "default": "female_us"
    }
