package com.speakmaster.notification.websocket;

import com.speakmaster.common.constant.LogMessages;
import com.speakmaster.common.utils.JwtUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;

import java.util.Map;

/**
 * 通知WebSocket握手拦截器
 * 从URL参数中提取token验证用户身份
 * 
 * @author SpeakMaster
 */
@Slf4j
@Component
public class NotificationHandshakeInterceptor implements HandshakeInterceptor {

    @Override
    public boolean beforeHandshake(ServerHttpRequest request, ServerHttpResponse response,
                                   WebSocketHandler wsHandler, Map<String, Object> attributes) {
        try {
            if (request instanceof ServletServerHttpRequest servletRequest) {
                // 从URL参数获取token
                String token = servletRequest.getServletRequest().getParameter("token");
                if (token == null || token.isEmpty()) {
                    log.warn("通知WebSocket握手失败: 缺少token参数");
                    return false;
                }

                // 验证token
                if (!JwtUtil.validateToken(token)) {
                    log.warn("通知WebSocket握手失败: token无效");
                    return false;
                }

                Long userId = JwtUtil.getUserId(token);
                if (userId == null) {
                    log.warn("通知WebSocket握手失败: 无法解析userId");
                    return false;
                }

                // 存入WebSocket会话属性
                attributes.put("userId", userId);

                log.info("通知WebSocket握手成功: userId={}", userId);
                return true;
            }
        } catch (Exception e) {
            log.error("通知WebSocket握手异常: {}", e.getMessage(), e);
        }
        return false;
    }

    @Override
    public void afterHandshake(ServerHttpRequest request, ServerHttpResponse response,
                               WebSocketHandler wsHandler, Exception exception) {
        // 握手完成后的处理(无需额外操作)
    }
}
