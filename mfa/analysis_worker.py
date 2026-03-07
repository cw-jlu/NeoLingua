"""Analysis worker: consumes Kafka messages and generates analysis reports."""
import json
import logging
import asyncio
from sqlalchemy.orm import Session
from app.database import SessionLocal
from app.models import Session as SessionModel, SessionMessage, AnalysisReport
from app.services.kafka_service import create_kafka_consumer
from app.services.redis_service import redis_service
from app.services.pronunciation_service import pronunciation_service
from app.services.audio_storage import audio_storage
from app.config import settings

logging.basicConfig(level=logging.INFO)
logger = logging.getLogger(__name__)


def analyze_text(text: str, role: str, metadata: dict = None, audio_url: str = None) -> dict:
    """
    Analyze text for grammar, pronunciation, and fluency.
    Uses pronunciation_service for user audio analysis.
    """
    # Base scores (mock for grammar/fluency)
    grammar_score = 85
    fluency_score = 82
    pronunciation_score = 0 # Default 0 if no audio
    detailed_feedback = {
        "grammar_errors": [],
        "pronunciation_hints": [],
        "fluency_notes": []
    }
    native_suggestion = None

    # 1. Pronunciation Analysis (Real)
    # Check if we have audio URL either directly or in metadata
    target_audio_url = audio_url or (metadata.get("audio_url") if metadata else None)
    
    if role == "user" and target_audio_url:
        try:
            logger.info(f"Debug Audio URL: {target_audio_url}")
            
            # 解析音频URL到本地文件路径
            # audio_url格式: "/audio/2026/02/27/xxx.webm" 或 "audio_files/2026/02/27/xxx.webm"
            rel_path = target_audio_url.lstrip("/")
            
            # 移除可能的 "audio/" 前缀
            if rel_path.startswith("audio/"):
                rel_path = rel_path[6:]  # 移除 "audio/"
            
            # 构建完整路径
            audio_path = audio_storage.local_storage_path / rel_path
            logger.info(f"Debug Resolved Path: {audio_path.absolute()}")
            
            if audio_path.exists():
                logger.info(f"Running pronunciation analysis on {audio_path}")
                p_result = pronunciation_service.analyze(audio_path, text)
                
                pronunciation_score = p_result.get("pronunciation_score", 0)
                
                # Add feedback
                p_feedback = p_result.get("feedback", "")
                if p_feedback:
                    detailed_feedback["pronunciation_hints"].append(p_feedback)
                    
                # Add detailed scores if available
                if "detailed_scores" in p_result:
                     detailed_feedback["pronunciation_details"] = p_result["detailed_scores"]
            else:
                logger.warning(f"Audio file not found at: {audio_path}")
                
        except Exception as e:
            logger.error(f"Pronunciation analysis error: {e}", exc_info=True)

    # 2. Grammar/Expression (Mock)
    suggestions = []
    if "want" in text.lower() and "wanna" not in text.lower():
        suggestions.append("Native speakers often use 'wanna' instead of 'want to' in casual conversation.")
    
    native_suggestion = "; ".join(suggestions) if suggestions else None
    
    return {
        "grammar_score": grammar_score,
        "pronunciation_score": pronunciation_score,
        "fluency_score": fluency_score,
        "native_expression_suggestion": native_suggestion,
        "detailed_feedback": detailed_feedback
    }


async def process_dialogue_message(message_value: dict, db: Session):
    """Process a dialogue text message and generate analysis."""
    try:
        session_id = message_value.get("session_id")
        text = message_value.get("text")
        role = message_value.get("role", "user")
        metadata = message_value.get("metadata", {})
        
        # 从metadata中获取message_id和audio_url
        message_id = metadata.get("message_id")
        audio_url = metadata.get("audio_url")
        
        if not session_id or not text:
            logger.warning(f"Invalid message: missing session_id or text")
            return
        
        logger.info(f"Processing dialogue: session={session_id}, msg_id={message_id}, text={text[:30]}...")
        
        # Verify session exists
        session = db.query(SessionModel).filter(SessionModel.id == session_id).first()
        if not session:
            logger.warning(f"Session not found: {session_id}")
            return
        
        # Find message record
        message = None
        if message_id:
            message = db.query(SessionMessage).filter(SessionMessage.id == message_id).first()
            if not message:
                logger.warning(f"Message ID {message_id} not found in database")
        
        if not message:
            # Fallback: find by content and role
            message = db.query(SessionMessage).filter(
                SessionMessage.session_id == session_id,
                SessionMessage.content == text,
                SessionMessage.role == role
            ).order_by(SessionMessage.created_at.desc()).first()
            
            if message:
                logger.info(f"Found message by content match: {message.id}")
        
        if not message:
            logger.warning(f"Could not find message in database, creating report without message linkage")
        
        # Perform analysis
        analysis_result = analyze_text(text, role, metadata, audio_url)
        
        # Create analysis report
        report = AnalysisReport(
            session_id=session_id,
            message_id=message.id if message else None,
            grammar_score=analysis_result["grammar_score"],
            pronunciation_score=analysis_result["pronunciation_score"],
            fluency_score=analysis_result["fluency_score"],
            native_expression_suggestion=analysis_result["native_expression_suggestion"],
            detailed_feedback=analysis_result["detailed_feedback"]
        )
        
        db.add(report)
        db.commit()
        db.refresh(report)
        
        logger.info(f"Analysis report created: report_id={report.id}, pronunciation_score={report.pronunciation_score}")
        
        # Send analysis result to Kafka for frontend notification
        if kafka_service.producer:
            result_payload = {
                "type": "analysis_result",
                "session_id": session_id,
                "message_id": message.id if message else None,
                "report_id": report.id,
                "grammar_score": report.grammar_score,
                "pronunciation_score": report.pronunciation_score,
                "fluency_score": report.fluency_score,
                "suggestion": report.native_expression_suggestion,
                "detailed_feedback": report.detailed_feedback
            }
            kafka_service.producer.send("stream.analysis.result", value=result_payload)
            logger.info("Sent analysis result to stream.analysis.result")
        else:
            logger.warning("Kafka producer not connected, skipping notification")
            
        # === Archiving: Upload audio to MinIO ===
        if message and audio_url:
            try:
                # 复用之前的路径解析逻辑
                rel_path = audio_url.lstrip("/")
                if rel_path.startswith("audio/"):
                    rel_path = rel_path[6:]
                
                local_file = audio_storage.local_storage_path / rel_path
                
                if local_file.exists():
                    # Archive
                    minio_url = audio_storage.archive_to_minio(local_file, rel_path)
                    
                    if minio_url:
                        # Update message record with new persistent URL
                        message.audio_url = minio_url
                        db.commit()
                        logger.info(f"Updated message {message.id} audio_url to {minio_url}")
                        
                        # Optional: Delete local file
                        # local_file.unlink()
                else:
                    logger.warning(f"Local audio file not found for archiving: {local_file}")
            except Exception as archive_err:
                logger.error(f"Archiving failed: {archive_err}", exc_info=True)
        
    except Exception as e:
        logger.error(f"Error processing dialogue message: {e}", exc_info=True)
        db.rollback()


def run_analysis_worker():
    """Main worker loop."""
    logger.info("Starting analysis worker...")
    
    # Connect to Redis
    asyncio.run(redis_service.connect())
    
    # Initialize Kafka Producer
    kafka_service.connect()
    
    # Create Kafka consumer
    consumer = create_kafka_consumer("stream.dialogue.text", "analysis-worker-group")
    
    logger.info("Analysis worker started, waiting for messages...")
    
    try:
        for message in consumer:
            try:
                message_value = message.value
                message_key = message.key
                
                logger.debug(f"Received message: key={message_key}")
                
                # Process in async context
                db = SessionLocal()
                try:
                    asyncio.run(process_dialogue_message(message_value, db))
                finally:
                    db.close()
                    
            except Exception as e:
                logger.error(f"Error processing message: {e}", exc_info=True)
    
    except KeyboardInterrupt:
        logger.info("Analysis worker shutting down...")
    finally:
        consumer.close()
        asyncio.run(redis_service.disconnect())


if __name__ == "__main__":
    # Import here to avoid circular dependency
    from app.services.kafka_service import kafka_service
    run_analysis_worker()

