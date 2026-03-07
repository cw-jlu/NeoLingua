"""
Kafka服务 - 消息消费和生产
"""
import json
import logging
from typing import Optional
from kafka import KafkaConsumer, KafkaProducer
from app.config import settings

logger = logging.getLogger(__name__)


class KafkaService:
    """Kafka服务"""

    def __init__(self):
        self.producer: Optional[KafkaProducer] = None
        self.consumer: Optional[KafkaConsumer] = None

    def connect_producer(self):
        """连接Kafka生产者"""
        try:
            self.producer = KafkaProducer(
                bootstrap_servers=settings.KAFKA_BOOTSTRAP_SERVERS,
                value_serializer=lambda v: json.dumps(v, default=str).encode("utf-8"),
                key_serializer=lambda k: k.encode("utf-8") if k else None,
                acks="all",
                retries=3
            )
            logger.info("Kafka生产者连接成功")
        except Exception as e:
            logger.error(f"Kafka生产者连接失败: {e}")
            self.producer = None

    def create_consumer(self, topic: str, group_id: str) -> Optional[KafkaConsumer]:
        """创建Kafka消费者"""
        try:
            consumer = KafkaConsumer(
                topic,
                bootstrap_servers=settings.KAFKA_BOOTSTRAP_SERVERS,
                group_id=group_id,
                auto_offset_reset="latest",
                enable_auto_commit=True,
                value_deserializer=lambda m: json.loads(m.decode("utf-8")),
                key_deserializer=lambda k: k.decode("utf-8") if k else None,
                max_poll_interval_ms=300000,
                session_timeout_ms=30000
            )
            logger.info(f"Kafka消费者创建成功: topic={topic}, group={group_id}")
            return consumer
        except Exception as e:
            logger.error(f"Kafka消费者创建失败: {e}")
            return None

    def send_message(self, topic: str, value: dict, key: str = None):
        """发送消息"""
        if not self.producer:
            logger.warning("Kafka生产者未连接")
            return
        try:
            self.producer.send(topic, value=value, key=key)
            self.producer.flush()
        except Exception as e:
            logger.error(f"Kafka发送消息失败: {e}")

    def close(self):
        """关闭连接"""
        if self.producer:
            self.producer.close()
        if self.consumer:
            self.consumer.close()


kafka_service = KafkaService()
