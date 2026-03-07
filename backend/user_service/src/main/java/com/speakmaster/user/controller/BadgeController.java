package com.speakmaster.user.controller;

import com.speakmaster.common.constant.LogMessages;
import com.speakmaster.common.dto.Result;
import com.speakmaster.user.dto.BadgeDTO;
import com.speakmaster.user.service.IBadgeService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 徽章控制器
 * 
 * @author SpeakMaster
 */
@Slf4j
@RestController
@RequestMapping("/user/badges")
@RequiredArgsConstructor
public class BadgeController {

    private final IBadgeService badgeService;

    /**
     * 获取徽章列表
     */
    @GetMapping
    public Result<List<BadgeDTO>> getBadges(@RequestHeader("X-User-Id") Long userId) {
        log.info("获取用户徽章列表: userId={}", userId);
        List<BadgeDTO> badges = badgeService.getUserBadges(userId);
        return Result.success(badges);
    }

    /**
     * 获取徽章详情
     */
    @GetMapping("/{id}")
    public Result<BadgeDTO> getBadgeDetail(@PathVariable Long id) {
        log.info("获取徽章详情: badgeId={}", id);
        BadgeDTO badge = badgeService.getBadgeById(id);
        return Result.success(badge);
    }

    /**
     * 领取徽章
     */
    @PostMapping("/{id}/claim")
    public Result<Void> claimBadge(@RequestHeader("X-User-Id") Long userId,
                                    @PathVariable Long id) {
        log.info("领取徽章: userId={}, badgeId={}", userId, id);
        badgeService.claimBadge(userId, id);
        return Result.success("领取成功", null);
    }
}
