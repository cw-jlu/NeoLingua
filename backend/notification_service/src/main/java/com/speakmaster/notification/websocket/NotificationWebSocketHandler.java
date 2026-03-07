package com.speakmaster.notification.websocket;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.speakmaster.notification.dto.NotificationDTO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.*;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 通知WebSocket处理�?
 * 管理用户WebSocket连接，支持实时推送通知
 * 
 * @author SpeakMaster
 */
@Slf4j
@Component
public class NotificationWebSocketHandler extends TextWebSocketHandler {

    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * 在线用户会话映射
     * key: userId, value: WebSocketSession
     */
    private static final Map<Long, WebSocketSession> USER_SESSIONS = new ConcurrentHashMap<>();

    /**
     * 连接建立
     */
    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        Long userId = (Long) session.getAttributes().get("userId");
        USER_SESSIONS.put(userId, session);
        log.info("用户连接通知WebSocket: userId={}, 当前在线人数={}", userId, USER_SESSIONS.size());
    }

    /**
     * 接收消息（客户端心跳等）
     */
    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) {
        // 客户端发送心跳ping，回复pong
        String payload = message.getPayload();
        if ("ping".equals(payload)) {
            try {
                session.sendMessage(new TextMessage("pong"));
            } catch (IOException e) {
                log.error("回复心跳失败: {}", e.getMessage());
            }
        }
    }

    /**
     * 连接关闭
     */
    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        Long userId = (Long) session.getAttributes().get("userId");
        USER_SESSIONS.remove(userId);
        log.info("用户断开通知WebSocket: userId={}, 当前在线人数={}", userId, USER_SESSIONS.size());
    }

    /**
     * 传输错误处理
     */
    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) {
        Long userId = (Long) session.getAttributes().get("userId");
        log.error("通知WebSocket传输错误: userId={}, error={}", userId, exception.getMessage());
    }

    /**
     * 推送通知给指定用�?
     * 
     * @param userId 目标用户ID
     * @param notification 通知内容
     */
    public void pushNotification(Long userId, NotificationDTO notification) {
        WebSocketSession session = USER_SESSIONS.get(userId);
        if (session != null && session.isOpen()) {
            try {
                String payload = objectMapper.writeValueAsString(notification);
                session.sendMessage(new TextMessage(payload));
                log.debug("推送通知成功: userId={}, notificationId={}", userId, notification.getId());
            } catch (IOException e) {
                log.error("推送通知失败: userId={}, error={}", userId, e.getMessage());
            }
        }
    }

    /**
     * 广播通知给所有在线用�?
     * 
     * @param notification 通知内容
     */
    public void broadcastNotification(NotificationDTO notification) {
        try {
            String payload = objectMapper.writeValueAsString(notification);
            TextMessage textMessage = new TextMessage(payload);
            USER_SESSIONS.values().forEach(session -> {
                try {
                    if (session.isOpen()) {
                        session.sendMessage(textMessage);
                    }
                } catch (IOException e) {
                    log.error("广播通知失败: error={}", e.getMessage());
                }
            });
            log.info("广播通知完成: 在线用户�?{}", USER_SESSIONS.size());
        } catch (Exception e) {
            log.error("广播通知序列化失�? {}", e.getMessage());
        }
    }

    /**
     * 检查用户是否在�?
     */
    public boolean isUserOnline(Long userId) {
        WebSocketSession session = USER_SESSIONS.get(userId);
        return session != null && session.isOpen();
    }

    /**
     * 获取在线用户�?
     */
    public int getOnlineCount() {
        return USER_SESSIONS.size();
    }
}
