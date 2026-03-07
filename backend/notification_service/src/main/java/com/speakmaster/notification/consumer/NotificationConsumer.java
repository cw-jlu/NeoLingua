package com.speakmaster.notification.consumer;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.speakmaster.notification.dto.NotificationDTO;
import com.speakmaster.notification.service.INotificationService;
import com.speakmaster.notification.websocket.NotificationWebSocketHandler;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

/**
 * 通知Kafka消费�?
 * 消费Kafka通知消息，持久化后通过WebSocket实时推送给用户
 * 
 * @author SpeakMaster
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class NotificationConsumer {

    private final INotificationService notificationService;
    private final NotificationWebSocketHandler webSocketHandler;
    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * 消费通知消息
     */
    @KafkaListener(topics = "notifications", groupId = "notification-service")
    public void consumeNotification(String message) {
        try {
            log.info("收到通知消息: {}", message);
            
            NotificationDTO notificationDTO = objectMapper.readValue(message, NotificationDTO.class);
            NotificationDTO savedNotification = notificationService.createNotification(notificationDTO);
            
            // 通过WebSocket实时推送给目标用户
            Long receiverId = savedNotification.getReceiverId();
            if (receiverId != null && receiverId > 0) {
                // 定向推�?
                webSocketHandler.pushNotification(receiverId, savedNotification);
            } else {
                // 广播推送（receiverId=0表示广播�?
                webSocketHandler.broadcastNotification(savedNotification);
            }
            
            log.info("处理通知消息成功: receiverId={}", notificationDTO.getReceiverId());
        } catch (Exception e) {
            log.error("处理通知消息失败: {}", e.getMessage(), e);
        }
    }
}
