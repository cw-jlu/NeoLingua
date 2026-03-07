"""
音频存储服务 - 本地存储 + MinIO归档
"""
import logging
import os
from pathlib import Path
from typing import Optional
from minio import Minio
from minio.error import S3Error
from app.config import settings

logger = logging.getLogger(__name__)


class AudioStorage:
    """音频存储服务"""

    def __init__(self):
        self.local_storage_path = Path(settings.AUDIO_STORAGE_PATH)
        self.local_storage_path.mkdir(parents=True, exist_ok=True)
        self.minio_client: Optional[Minio] = None

    def connect_minio(self):
        """连接MinIO"""
        try:
            self.minio_client = Minio(
                settings.MINIO_ENDPOINT,
                access_key=settings.MINIO_ACCESS_KEY,
                secret_key=settings.MINIO_SECRET_KEY,
                secure=settings.MINIO_SECURE
            )
            # 确保bucket存在
            if not self.minio_client.bucket_exists(settings.MINIO_BUCKET):
                self.minio_client.make_bucket(settings.MINIO_BUCKET)
            logger.info("MinIO连接成功")
        except Exception as e:
            logger.error(f"MinIO连接失败: {e}")
            self.minio_client = None

    def archive_to_minio(self, local_path: Path, object_name: str) -> Optional[str]:
        """将本地音频归档到MinIO"""
        if not self.minio_client:
            logger.warning("MinIO未连接，跳过归档")
            return None
        try:
            self.minio_client.fput_object(
                settings.MINIO_BUCKET,
                object_name,
                str(local_path)
            )
            url = f"minio://{settings.MINIO_BUCKET}/{object_name}"
            logger.info(f"音频归档成功: {url}")
            return url
        except S3Error as e:
            logger.error(f"MinIO归档失败: {e}")
            return None

    def resolve_audio_path(self, audio_url: str) -> Optional[Path]:
        """
        解析音频URL到本地文件路径。
        优先找本地缓存，找不到则尝试从MinIO下载。
        支持格式:
          - /audio/xxx.wav  (本地相对路径)
          - minio://bucket/xxx.wav
          - http://minio-host:9000/bucket/xxx.wav
        """
        if not audio_url:
            return None

        # 先尝试本地路径
        rel_path = audio_url.lstrip("/")
        if rel_path.startswith("audio/"):
            rel_path = rel_path[6:]
        audio_path = self.local_storage_path / rel_path
        if audio_path.exists():
            return audio_path

        # 本地不存在，尝试从MinIO下载
        object_name = self._extract_object_name(audio_url)
        if object_name:
            return self.download_from_minio(object_name)

        return None

    def _extract_object_name(self, audio_url: str) -> Optional[str]:
        """从各种格式的URL中提取MinIO object name"""
        if not audio_url:
            return None
        # minio://bucket/path/to/file.wav
        if audio_url.startswith("minio://"):
            parts = audio_url[8:].split("/", 1)
            return parts[1] if len(parts) == 2 else None
        # http://host:port/bucket/path/to/file.wav
        if audio_url.startswith("http://") or audio_url.startswith("https://"):
            try:
                from urllib.parse import urlparse
                parsed = urlparse(audio_url)
                # path = /bucket/object_name
                path_parts = parsed.path.lstrip("/").split("/", 1)
                return path_parts[1] if len(path_parts) == 2 else None
            except Exception:
                return None
        return None

    def download_from_minio(self, object_name: str) -> Optional[Path]:
        """从MinIO下载音频到本地临时目录，返回本地路径"""
        if not self.minio_client:
            logger.warning("MinIO未连接，无法下载音频")
            return None
        try:
            # 保持目录结构，存到本地缓存目录
            local_path = self.local_storage_path / object_name
            local_path.parent.mkdir(parents=True, exist_ok=True)
            self.minio_client.fget_object(settings.MINIO_BUCKET, object_name, str(local_path))
            logger.info(f"音频从MinIO下载成功: {object_name} -> {local_path}")
            return local_path
        except S3Error as e:
            logger.error(f"MinIO下载失败: object={object_name}, error={e}")
            return None
        except Exception as e:
            logger.error(f"音频下载异常: {e}")
            return None

    def get_audio_bytes(self, audio_url: str) -> Optional[bytes]:
        """获取音频的原始字节（用于base64编码传给远程API）"""
        local_path = self.resolve_audio_path(audio_url)
        if local_path and local_path.exists():
            return local_path.read_bytes()
        # 直接从MinIO流式读取
        if not self.minio_client:
            return None
        object_name = self._extract_object_name(audio_url)
        if not object_name:
            return None
        try:
            response = self.minio_client.get_object(settings.MINIO_BUCKET, object_name)
            data = response.read()
            response.close()
            response.release_conn()
            return data
        except Exception as e:
            logger.error(f"读取MinIO音频字节失败: {e}")
            return None


audio_storage = AudioStorage()
