package com.speakmaster.common.interceptor;

import com.speakmaster.common.constant.LogMessages;
import com.speakmaster.common.context.UserContext;
import com.speakmaster.common.context.UserContextHolder;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * 用户上下文拦截器
 * 从请求头中提取用户信息并存入 ThreadLocal
 * 
 * @author SpeakMaster
 */
@Slf4j
public class UserContextInterceptor implements HandlerInterceptor {
    
    private static final String USER_ID_HEADER = "X-User-Id";
    private static final String USERNAME_HEADER = "X-Username";
    
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        // 从请求头中获取用户信息
        String userIdStr = request.getHeader(USER_ID_HEADER);
        String username = request.getHeader(USERNAME_HEADER);
        
        if (userIdStr != null && username != null) {
            try {
                Long userId = Long.parseLong(userIdStr);
                UserContext userContext = new UserContext(userId, username);
                UserContextHolder.setContext(userContext);
                log.debug(LogMessages.SET_USER_CONTEXT, userId, username);
            } catch (NumberFormatException e) {
                log.warn(LogMessages.PARSE_USER_ID_FAILED, userIdStr);
            }
        }
        
        return true;
    }
    
    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, 
                                Object handler, Exception ex) {
        // 请求完成后清除 ThreadLocal,防止内存泄漏
        UserContextHolder.clear();
        log.debug(LogMessages.CLEAR_USER_CONTEXT);
    }
}
