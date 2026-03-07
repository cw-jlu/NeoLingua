package com.speakmaster.gateway.filter;

import com.speakmaster.common.constant.LogMessages;
import com.speakmaster.common.utils.JwtUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.Arrays;
import java.util.List;

/**
 * JWT认证过滤器
 * 验证请求中的JWT Token
 * 
 * @author SpeakMaster
 */
@Slf4j
@Component
public class AuthFilter implements GlobalFilter, Ordered {

    /**
     * 不需要认证的路径
     */
    private static final List<String> WHITE_LIST = Arrays.asList(
            "/user/auth/register",
            "/user/auth/login",
            "/actuator",
            "/fallback"
    );

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        String path = request.getURI().getPath();

        // 白名单路径直接放行
        if (isWhiteList(path)) {
            return chain.filter(exchange);
        }

        // 获取Token
        String token = getToken(request);
        if (token == null || token.isEmpty()) {
            log.warn(LogMessages.REQUEST_WITHOUT_TOKEN, path);
            return unauthorized(exchange.getResponse(), "未登录或登录已过期");
        }

        // 验证Token
        if (!JwtUtil.validateToken(token)) {
            log.warn(LogMessages.TOKEN_VALIDATION_FAILED, path, token);
            return unauthorized(exchange.getResponse(), "Token无效或已过期");
        }

        // 从Token中获取用户信息并添加到请求头
        Long userId = JwtUtil.getUserId(token);
        String username = JwtUtil.getUsername(token);
        
        if (userId != null && username != null) {
            ServerHttpRequest modifiedRequest = request.mutate()
                    .header("X-User-Id", userId.toString())
                    .header("X-Username", username)
                    .build();
            exchange = exchange.mutate().request(modifiedRequest).build();
        }

        return chain.filter(exchange);
    }

    /**
     * 判断是否是白名单路径
     */
    private boolean isWhiteList(String path) {
        return WHITE_LIST.stream().anyMatch(path::startsWith);
    }

    /**
     * 从请求中获取Token
     */
    private String getToken(ServerHttpRequest request) {
        // 从Header中获取
        String token = request.getHeaders().getFirst("Authorization");
        if (token != null && token.startsWith("Bearer ")) {
            return token.substring(7);
        }
        
        // 从Query参数中获取
        List<String> tokenList = request.getQueryParams().get("token");
        if (tokenList != null && !tokenList.isEmpty()) {
            return tokenList.get(0);
        }
        
        return null;
    }

    /**
     * 返回未授权响应
     */
    private Mono<Void> unauthorized(ServerHttpResponse response, String message) {
        response.setStatusCode(HttpStatus.UNAUTHORIZED);
        response.getHeaders().add("Content-Type", "application/json;charset=UTF-8");
        String body = String.format("{\"code\":2000,\"msg\":\"%s\",\"data\":null}", message);
        return response.writeWith(Mono.just(response.bufferFactory().wrap(body.getBytes())));
    }

    @Override
    public int getOrder() {
        return -100; // 优先级最高
    }
}
