"""
Kafka消费者Worker
独立进程运行，消费对话消息并生成分析报告
启动方式: python -m app.worker
"""
import asyncio
import logging
from app.config import settings
from app.database import SessionLocal, engine, Base
from app.services.kafka_service import kafka_service
from app.services.redis_service import redis_service
from app.services.audio_storage import audio_storage
from app.services.analysis_service import analysis_business_service

logging.basicConfig(
    level=logging.INFO,
    format="%(asctime)s [%(levelname)s] %(name)s - %(message)s"
)
logger = logging.getLogger(__name__)


async def process_message(message_value: dict):
    """处理单条Kafka消息"""
    db = SessionLocal()
    try:
        session_id = message_value.get("session_id")
        text = message_value.get("text")
        role = message_value.get("role", "user")
        user_id = message_value.get("user_id", 0)
        metadata = message_value.get("metadata", {})
        message_id = metadata.get("message_id")
        audio_url = metadata.get("audio_url")

        if not session_id or not text:
            logger.warning("消息缺少session_id或text，跳过")
            return

        logger.info(f"处理消息: session={session_id}, user={user_id}, text={text[:30]}...")

        # 分析消息
        report = await analysis_business_service.analyze_message(
            db, session_id, user_id, text, role,
            message_id=message_id, audio_url=audio_url, metadata=metadata
        )

        if report:
            # 发送分析结果到Kafka
            kafka_service.send_message(
                settings.KAFKA_TOPIC_RESULT,
                {
                    "type": "analysis_result",
                    "session_id": session_id,
                    "message_id": message_id,
                    "report_id": report.id,
                    "user_id": user_id,
                    "grammar_score": report.grammar_score,
                    "pronunciation_score": report.pronunciation_score,
                    "fluency_score": report.fluency_score,
                    "overall_score": report.overall_score,
                    "suggestion": report.native_expression_suggestion,
                }
            )
            logger.info(f"分析完成: report_id={report.id}, overall={report.overall_score}")

            # 归档音频到MinIO
            if audio_url:
                try:
                    audio_path = audio_storage.resolve_audio_path(audio_url)
                    if audio_path:
                        rel = audio_url.lstrip("/")
                        if rel.startswith("audio/"):
                            rel = rel[6:]
                        audio_storage.archive_to_minio(audio_path, rel)
                except Exception as e:
                    logger.error(f"音频归档失败: {e}")
    except Exception as e:
        logger.error(f"消息处理失败: {e}", exc_info=True)
    finally:
        db.close()


def run_worker():
    """启动Worker"""
    logger.info("Analysis Worker 启动中...")

    # 创建数据库表
    Base.metadata.create_all(bind=engine)

    # 连接服务
    asyncio.run(redis_service.connect())
    kafka_service.connect_producer()
    audio_storage.connect_minio()

    # 创建消费者
    consumer = kafka_service.create_consumer(
        settings.KAFKA_TOPIC_DIALOGUE,
        settings.KAFKA_CONSUMER_GROUP
    )
    if not consumer:
        logger.error("Kafka消费者创建失败，退出")
        return

    logger.info(f"Worker已启动，监听topic: {settings.KAFKA_TOPIC_DIALOGUE}")

    try:
        for message in consumer:
            try:
                asyncio.run(process_message(message.value))
            except Exception as e:
                logger.error(f"处理消息异常: {e}", exc_info=True)
    except KeyboardInterrupt:
        logger.info("Worker正在关闭...")
    finally:
        consumer.close()
        asyncio.run(redis_service.disconnect())
        kafka_service.close()
        logger.info("Worker已关闭")


if __name__ == "__main__":
    run_worker()
