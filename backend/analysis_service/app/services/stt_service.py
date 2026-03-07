"""
STT 服务 - 语音转文字
使用 faster-whisper (tiny 模型，CPU 友好，~75MB)
懒加载，首次调用时下载模型
"""
import logging
import os
import tempfile
from pathlib import Path
from typing import Optional, Tuple

logger = logging.getLogger(__name__)

_whisper_model = None


def _get_model():
    global _whisper_model
    if _whisper_model is None:
        try:
            from faster_whisper import WhisperModel
            _whisper_model = WhisperModel("tiny", device="cpu", compute_type="int8")
            logger.info("Whisper tiny 模型加载成功")
        except ImportError:
            logger.error("faster-whisper 未安装，请执行: pip install faster-whisper")
        except Exception as e:
            logger.error(f"Whisper 模型加载失败: {e}")
    return _whisper_model


def transcribe(audio_path: str, language: str = "en") -> Tuple[str, str]:
    """
    语音转文字
    返回 (识别文字, 检测到的语言)
    """
    model = _get_model()
    if model is None:
        raise RuntimeError("STT 模型不可用，请安装 faster-whisper")

    segments, info = model.transcribe(
        audio_path,
        language=language,
        beam_size=3,
        vad_filter=True,
        vad_parameters={"min_silence_duration_ms": 500}
    )
    text = " ".join(seg.text.strip() for seg in segments).strip()
    return text, info.language


def transcribe_bytes(audio_bytes: bytes, suffix: str = ".wav", language: str = "en") -> Tuple[str, str]:
    """
    从字节流转文字，写临时文件后识别
    """
    with tempfile.NamedTemporaryFile(suffix=suffix, delete=False) as tmp:
        tmp.write(audio_bytes)
        tmp_path = tmp.name
    try:
        return transcribe(tmp_path, language)
    finally:
        try:
            os.unlink(tmp_path)
        except Exception:
            pass
