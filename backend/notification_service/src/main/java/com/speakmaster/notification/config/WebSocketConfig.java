package com.speakmaster.notification.config;

import com.speakmaster.notification.websocket.NotificationWebSocketHandler;
import com.speakmaster.notification.websocket.NotificationHandshakeInterceptor;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

/**
 * WebSocket配置�?
 * 注册通知实时推送的WebSocket端点
 * 
 * @author SpeakMaster
 */
@Configuration
@EnableWebSocket
@RequiredArgsConstructor
public class WebSocketConfig implements WebSocketConfigurer {

    private final NotificationWebSocketHandler notificationWebSocketHandler;
    private final NotificationHandshakeInterceptor handshakeInterceptor;

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        // 注册通知推送WebSocket端点，支持跨�?
        registry.addHandler(notificationWebSocketHandler, "/ws/notification")
                .addInterceptors(handshakeInterceptor)
                .setAllowedOrigins("*");
    }
}
