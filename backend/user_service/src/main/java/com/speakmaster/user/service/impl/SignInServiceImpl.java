package com.speakmaster.user.service.impl;

import com.speakmaster.common.enums.ErrorCode;
import com.speakmaster.common.exception.BusinessException;
import com.speakmaster.user.service.ISignInService;
import com.speakmaster.user.service.IUserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * 签到服务实现类
 * 使用Redis Bitmap实现
 * 
 * @author SpeakMaster
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SignInServiceImpl implements ISignInService {

    private final RedisTemplate<String, Object> redisTemplate;
    private final IUserService userService;
    
    private static final String SIGN_IN_KEY_PREFIX = "sign_in:";
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyyMM");
    private static final Long SIGN_IN_POINTS = 5L; // 签到获得积分

    @Override
    public void signIn(Long userId) {
        LocalDate today = LocalDate.now();
        String key = getSignInKey(userId, today);
        int dayOfMonth = today.getDayOfMonth();

        // 检查今天是否已签到
        Boolean signed = redisTemplate.opsForValue().getBit(key, dayOfMonth - 1);
        if (Boolean.TRUE.equals(signed)) {
            throw new BusinessException(ErrorCode.USER_ALREADY_SIGNED_IN);
        }

        // 签到
        redisTemplate.opsForValue().setBit(key, dayOfMonth - 1, true);
        
        // 增加积分
        userService.addPoints(userId, SIGN_IN_POINTS, "每日签到");
        
        log.info("用户签到成功: userId={}, date={}", userId, today);
    }

    @Override
    public SignInStatus getSignInStatus(Long userId) {
        LocalDate today = LocalDate.now();
        String key = getSignInKey(userId, today);
        int dayOfMonth = today.getDayOfMonth();

        // 今天是否已签到
        Boolean todaySigned = redisTemplate.opsForValue().getBit(key, dayOfMonth - 1);

        // 连续签到天数
        int continuousDays = getContinuousSignInDays(userId);

        // 本月签到天数
        int monthDays = getMonthSignInDays(userId, today);

        SignInStatus status = new SignInStatus();
        status.setTodaySigned(Boolean.TRUE.equals(todaySigned));
        status.setContinuousDays(continuousDays);
        status.setMonthDays(monthDays);
        status.setTotalDays(getTotalSignInDays(userId));

        return status;
    }

    @Override
    public List<Integer> getSignInCalendar(Long userId, LocalDate date) {
        String key = getSignInKey(userId, date);
        int daysInMonth = date.lengthOfMonth();
        
        List<Integer> calendar = new ArrayList<>();
        for (int i = 0; i < daysInMonth; i++) {
            Boolean signed = redisTemplate.opsForValue().getBit(key, i);
            calendar.add(Boolean.TRUE.equals(signed) ? 1 : 0);
        }
        
        return calendar;
    }

    /**
     * 获取连续签到天数
     */
    private int getContinuousSignInDays(Long userId) {
        LocalDate today = LocalDate.now();
        int days = 0;
        
        for (int i = 0; i < 365; i++) {
            LocalDate date = today.minusDays(i);
            String key = getSignInKey(userId, date);
            int dayOfMonth = date.getDayOfMonth();
            
            Boolean signed = redisTemplate.opsForValue().getBit(key, dayOfMonth - 1);
            if (Boolean.TRUE.equals(signed)) {
                days++;
            } else {
                break;
            }
        }
        
        return days;
    }

    /**
     * 获取本月签到天数
     */
    private int getMonthSignInDays(Long userId, LocalDate date) {
        String key = getSignInKey(userId, date);
        int daysInMonth = date.lengthOfMonth();
        int count = 0;
        
        for (int i = 0; i < daysInMonth; i++) {
            Boolean signed = redisTemplate.opsForValue().getBit(key, i);
            if (Boolean.TRUE.equals(signed)) {
                count++;
            }
        }
        
        return count;
    }

    /**
     * 获取总签到天数(简化版,实际应该遍历所有月份)
     */
    private int getTotalSignInDays(Long userId) {
        // 这里简化处理,只统计当前月
        // 实际应该遍历用户注册以来的所有月份
        return getMonthSignInDays(userId, LocalDate.now());
    }

    /**
     * 生成签到Key
     */
    private String getSignInKey(Long userId, LocalDate date) {
        return SIGN_IN_KEY_PREFIX + userId + ":" + date.format(FORMATTER);
    }
}
