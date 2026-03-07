package com.speakmaster.user.controller;

import com.speakmaster.common.constant.LogMessages;
import com.speakmaster.common.context.UserContextHolder;
import com.speakmaster.common.dto.Result;
import com.speakmaster.common.utils.JwtUtil;
import com.speakmaster.user.dto.LoginRequest;
import com.speakmaster.user.dto.RegisterRequest;
import com.speakmaster.user.dto.UserDTO;
import com.speakmaster.user.service.IUserService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * 认证控制器
 * 
 * @author SpeakMaster
 */
@Slf4j
@RestController
@RequestMapping("/user/auth")
@RequiredArgsConstructor
public class AuthController {

    private final IUserService userService;

    /**
     * 用户注册
     */
    @PostMapping("/register")
    public Result<UserDTO> register(@Valid @RequestBody RegisterRequest request) {
        log.info(LogMessages.USER_REGISTER_REQUEST, request.getUsername());
        UserDTO user = userService.register(request);
        return Result.success("注册成功", user);
    }

    /**
     * 用户登录
     */
    @PostMapping("/login")
    public Result<Map<String, Object>> login(@Valid @RequestBody LoginRequest request, HttpServletRequest httpRequest) {
        String ip = getClientIp(httpRequest);
        log.info(LogMessages.USER_LOGIN_REQUEST, request.getUsername(), ip);
        
        String token = userService.login(request, ip);
        
        Map<String, Object> data = new HashMap<>();
        data.put("token", token);
        data.put("tokenType", "Bearer");
        
        return Result.success("登录成功", data);
    }

    /**
     * 用户登出
     */
    @PostMapping("/logout")
    public Result<Void> logout() {
        log.info(LogMessages.USER_LOGOUT_REQUEST);
        // Token由客户端删除,服务端可以将Token加入黑名单(可选)
        return Result.success("登出成功", null);
    }

    /**
     * 刷新Token
     */
    @GetMapping("/refresh")
    public Result<Map<String, Object>> refresh(@RequestHeader("Authorization") String authorization) {
        String token = authorization.substring(7); // 去掉 "Bearer "
        
        if (!JwtUtil.shouldRefresh(token)) {
            Map<String, Object> data = new HashMap<>();
            data.put("token", token);
            data.put("tokenType", "Bearer");
            return Result.success("Token无需刷新", data);
        }
        
        String newToken = JwtUtil.refreshToken(token);
        Map<String, Object> data = new HashMap<>();
        data.put("token", newToken);
        data.put("tokenType", "Bearer");
        
        log.info(LogMessages.TOKEN_REFRESH_SUCCESS);
        return Result.success("Token刷新成功", data);
    }

    /**
     * 注销账号
     */
    @DeleteMapping("/account")
    public Result<Void> deleteAccount() {
        Long userId = UserContextHolder.getCurrentUserId();
        log.info(LogMessages.USER_DELETE_ACCOUNT_REQUEST, userId);
        userService.deleteUser(userId);
        return Result.success("账号注销成功", null);
    }

    /**
     * 获取客户端IP
     */
    private String getClientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("X-Real-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        return ip;
    }
}
