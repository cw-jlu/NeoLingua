package com.speakmaster.user.controller;

import com.speakmaster.common.constant.LogMessages;
import com.speakmaster.common.dto.Result;
import com.speakmaster.user.service.ISignInService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

/**
 * 签到控制器
 * 
 * @author SpeakMaster
 */
@Slf4j
@RestController
@RequestMapping("/user/signin")
@RequiredArgsConstructor
public class SignInController {

    private final ISignInService signInService;

    /**
     * 每日签到
     */
    @PostMapping
    public Result<Void> signIn(@RequestHeader("X-User-Id") Long userId) {
        log.info("用户签到请求: userId={}", userId);
        signInService.signIn(userId);
        return Result.success("签到成功", null);
    }

    /**
     * 获取签到状态
     */
    @GetMapping("/status")
    public Result<ISignInService.SignInStatus> getStatus(@RequestHeader("X-User-Id") Long userId) {
        log.info("获取签到状态: userId={}", userId);
        ISignInService.SignInStatus status = signInService.getSignInStatus(userId);
        return Result.success(status);
    }

    /**
     * 获取签到日历
     */
    @GetMapping("/calendar")
    public Result<List<Integer>> getCalendar(@RequestHeader("X-User-Id") Long userId,
                                              @RequestParam(required = false) String month) {
        log.info("获取签到日历: userId={}, month={}", userId, month);
        LocalDate date = month != null ? LocalDate.parse(month + "-01") : LocalDate.now();
        List<Integer> calendar = signInService.getSignInCalendar(userId, date);
        return Result.success(calendar);
    }

    /**
     * 获取签到统计
     */
    @GetMapping("/statistics")
    public Result<ISignInService.SignInStatus> getStatistics(@RequestHeader("X-User-Id") Long userId) {
        log.info("获取签到统计: userId={}", userId);
        ISignInService.SignInStatus status = signInService.getSignInStatus(userId);
        return Result.success(status);
    }
}
