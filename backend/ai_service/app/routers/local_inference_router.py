"""
本地Transformers模型推理路由
供 ai_gateway 的 LocalProvider 调用
支持文本模型和多模态模型（音频输入）
"""
import base64
import tempfile
import os
from typing import Optional, List, Dict, Any
from fastapi import APIRouter, HTTPException
from pydantic import BaseModel
from loguru import logger

router = APIRouter(prefix="/ai/local", tags=["local-inference"])

# 懒加载，避免启动时占用大量内存
_model = None
_processor = None
_model_name = None


class LocalChatMessage(BaseModel):
    role: str
    content: str


class LocalChatRequest(BaseModel):
    model_name: str          # Transformers模型名或本地路径
    messages: List[LocalChatMessage]
    audio_url: Optional[str] = None   # MinIO URL，多模态时使用
    audio_base64: Optional[str] = None  # base64编码的音频，多模态时使用
    temperature: Optional[float] = 0.7
    max_tokens: Optional[int] = 2048
    multimodal: Optional[bool] = False


class LocalChatResponse(BaseModel):
    content: str
    model_name: str
    token_count: int = 0


def _load_model(model_name: str):
    """懒加载Transformers模型"""
    global _model, _processor, _model_name
    if _model_name == model_name and _model is not None:
        return _model, _processor

    logger.info(f"加载本地模型: {model_name}")
    try:
        from transformers import AutoModelForCausalLM, AutoTokenizer, AutoProcessor
        import torch

        device = "cuda" if torch.cuda.is_available() else "cpu"
        logger.info(f"使用设备: {device}")

        # 尝试加载processor（多模态模型有processor）
        try:
            _processor = AutoProcessor.from_pretrained(model_name, trust_remote_code=True)
        except Exception:
            _processor = AutoTokenizer.from_pretrained(model_name, trust_remote_code=True)

        _model = AutoModelForCausalLM.from_pretrained(
            model_name,
            torch_dtype="auto",
            device_map="auto",
            trust_remote_code=True
        )
        _model_name = model_name
        logger.info(f"模型加载完成: {model_name}")
        return _model, _processor
    except Exception as e:
        logger.error(f"模型加载失败: {model_name}, error={e}")
        raise HTTPException(status_code=500, detail=f"模型加载失败: {str(e)}")


def _get_audio_bytes(audio_url: str) -> Optional[bytes]:
    """从MinIO获取音频字节"""
    try:
        from minio import Minio
        import re
        # 解析 http://host:port/bucket/object 格式
        match = re.match(r'https?://([^/]+)/([^/]+)/(.+)', audio_url)
        if not match:
            return None
        endpoint, bucket, obj = match.group(1), match.group(2), match.group(3)
        client = Minio(endpoint, access_key="minioadmin", secret_key="minioadmin", secure=False)
        response = client.get_object(bucket, obj)
        data = response.read()
        response.close()
        response.release_conn()
        return data
    except Exception as e:
        logger.error(f"从MinIO获取音频失败: {e}")
        return None


@router.post("/chat", response_model=LocalChatResponse)
async def local_chat(request: LocalChatRequest):
    """
    本地Transformers模型推理
    - 文本模型：直接用tokenizer处理messages
    - 多模态模型：从MinIO取音频，用processor处理
    """
    try:
        model, processor = _load_model(request.model_name)
        import torch

        # 构建对话文本
        if hasattr(processor, "apply_chat_template"):
            messages = [{"role": m.role, "content": m.content} for m in request.messages]
            text = processor.apply_chat_template(messages, tokenize=False, add_generation_prompt=True)
        else:
            text = "\n".join([f"{m.role}: {m.content}" for m in request.messages])
            text += "\nassistant:"

        # 多模态：处理音频输入
        audio_array = None
        if request.multimodal and (request.audio_url or request.audio_base64):
            audio_bytes = None
            if request.audio_base64:
                audio_bytes = base64.b64decode(request.audio_base64)
            elif request.audio_url:
                audio_bytes = _get_audio_bytes(request.audio_url)

            if audio_bytes:
                try:
                    import io
                    import soundfile as sf
                    audio_array, sample_rate = sf.read(io.BytesIO(audio_bytes))
                    logger.info(f"音频加载成功: sample_rate={sample_rate}, shape={audio_array.shape}")
                except Exception as e:
                    logger.warning(f"音频解析失败，降级为纯文本: {e}")

        # 推理
        device = next(model.parameters()).device
        if audio_array is not None and hasattr(processor, "__call__"):
            # 多模态推理
            inputs = processor(text=text, audios=audio_array, return_tensors="pt").to(device)
        else:
            # 纯文本推理
            inputs = processor(text, return_tensors="pt").to(device)

        with torch.no_grad():
            output_ids = model.generate(
                **inputs,
                max_new_tokens=request.max_tokens or 2048,
                temperature=request.temperature or 0.7,
                do_sample=True,
                pad_token_id=processor.eos_token_id if hasattr(processor, "eos_token_id") else None
            )

        # 只取新生成的部分
        input_len = inputs["input_ids"].shape[1]
        new_tokens = output_ids[0][input_len:]
        reply = processor.decode(new_tokens, skip_special_tokens=True)

        return LocalChatResponse(
            content=reply.strip(),
            model_name=request.model_name,
            token_count=len(new_tokens)
        )

    except HTTPException:
        raise
    except Exception as e:
        logger.error(f"本地推理失败: {e}", exc_info=True)
        raise HTTPException(status_code=500, detail=f"推理失败: {str(e)}")


@router.get("/health")
async def local_health():
    return {"status": "ok", "model_loaded": _model_name}
