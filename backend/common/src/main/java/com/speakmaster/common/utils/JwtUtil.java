package com.speakmaster.common.utils;

import com.speakmaster.common.constant.LogMessages;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * JWT工具类
 * 用于生成和解析JWT Token
 * 
 * @author SpeakMaster
 */
@Slf4j
public class JwtUtil {

    /**
     * 密钥 (从环境变量获取，至少256位)
     */
    private static final String SECRET_KEY = System.getenv().getOrDefault(
            "JWT_SECRET", 
            "your-very-secure-secret-key-should-be-at-least-256-bits-long"
    );

    /**
     * Token过期时间 (7天)
     */
    private static final long EXPIRATION_TIME = 7 * 24 * 60 * 60 * 1000L;

    /**
     * Token刷新时间 (1天)
     */
    private static final long REFRESH_TIME = 24 * 60 * 60 * 1000L;

    /**
     * 获取密钥
     */
    private static SecretKey getSecretKey() {
        return Keys.hmacShaKeyFor(SECRET_KEY.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * 生成Token
     * 
     * @param userId 用户ID
     * @param username 用户名
     * @return Token字符串
     */
    public static String generateToken(Long userId, String username) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("userId", userId);
        claims.put("username", username);
        return generateToken(claims);
    }

    /**
     * 生成Token (自定义Claims)
     * 
     * @param claims 自定义声明
     * @return Token字符串
     */
    public static String generateToken(Map<String, Object> claims) {
        Date now = new Date();
        Date expirationDate = new Date(now.getTime() + EXPIRATION_TIME);

        return Jwts.builder()
                .claims(claims)
                .issuedAt(now)
                .expiration(expirationDate)
                .signWith(getSecretKey())
                .compact();
    }

    /**
     * 解析Token
     * 
     * @param token Token字符串
     * @return Claims对象
     */
    public static Claims parseToken(String token) {
        try {
            return Jwts.parser()
                    .verifyWith(getSecretKey())
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
        } catch (Exception e) {
            log.error(LogMessages.TOKEN_PARSE_FAILED, e.getMessage());
            return null;
        }
    }

    /**
     * 从Token中获取用户ID
     * 
     * @param token Token字符串
     * @return 用户ID
     */
    public static Long getUserId(String token) {
        Claims claims = parseToken(token);
        if (claims == null) {
            return null;
        }
        Object userId = claims.get("userId");
        if (userId instanceof Integer) {
            return ((Integer) userId).longValue();
        }
        return (Long) userId;
    }

    /**
     * 从Token中获取用户名
     * 
     * @param token Token字符串
     * @return 用户名
     */
    public static String getUsername(String token) {
        Claims claims = parseToken(token);
        return claims != null ? (String) claims.get("username") : null;
    }

    /**
     * 验证Token是否有效
     * 
     * @param token Token字符串
     * @return 是否有效
     */
    public static boolean validateToken(String token) {
        try {
            Claims claims = parseToken(token);
            if (claims == null) {
                return false;
            }
            Date expiration = claims.getExpiration();
            return expiration.after(new Date());
        } catch (Exception e) {
            log.error(LogMessages.TOKEN_VALIDATE_FAILED, e.getMessage());
            return false;
        }
    }

    /**
     * 判断Token是否需要刷新
     * 
     * @param token Token字符串
     * @return 是否需要刷新
     */
    public static boolean shouldRefresh(String token) {
        try {
            Claims claims = parseToken(token);
            if (claims == null) {
                return false;
            }
            Date expiration = claims.getExpiration();
            Date now = new Date();
            // 如果Token在1天内过期，则需要刷新
            return expiration.getTime() - now.getTime() < REFRESH_TIME;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * 刷新Token
     * 
     * @param token 旧Token
     * @return 新Token
     */
    public static String refreshToken(String token) {
        Claims claims = parseToken(token);
        if (claims == null) {
            return null;
        }
        // 移除过期时间相关的claim
        claims.remove("exp");
        claims.remove("iat");
        return generateToken(new HashMap<>(claims));
    }
}
