package com.speakmaster.user.controller;

import com.speakmaster.common.dto.Result;
import com.speakmaster.user.dto.BadgeDTO;
import com.speakmaster.user.service.IBadgeService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.springframework.web.bind.annotation.*;

/**
 * 管理端 - 徽章管理控制器
 * 
 * @author SpeakMaster
 */
@Slf4j
@RestController
@RequestMapping("/admin/badges")
@RequiredArgsConstructor
public class AdminBadgeController {

    private final IBadgeService badgeService;

    /**
     * 获取徽章列表（分页）
     */
    @GetMapping
    public Result<Page<BadgeDTO>> getBadgeList(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        Page<BadgeDTO> badges = badgeService.adminGetBadgeList(page, size);
        return Result.success(badges);
    }

    /**
     * 获取徽章详情
     */
    @GetMapping("/{id}")
    public Result<BadgeDTO> getBadgeById(@PathVariable Long id) {
        BadgeDTO badge = badgeService.getBadgeById(id);
        return Result.success(badge);
    }

    /**
     * 创建徽章
     */
    @PostMapping
    public Result<BadgeDTO> createBadge(@RequestBody BadgeDTO badgeDTO) {
        BadgeDTO badge = badgeService.adminCreateBadge(badgeDTO);
        return Result.success(badge);
    }

    /**
     * 更新徽章
     */
    @PutMapping("/{id}")
    public Result<BadgeDTO> updateBadge(@PathVariable Long id, @RequestBody BadgeDTO badgeDTO) {
        BadgeDTO badge = badgeService.adminUpdateBadge(id, badgeDTO);
        return Result.success(badge);
    }

    /**
     * 删除徽章
     */
    @DeleteMapping("/{id}")
    public Result<Void> deleteBadge(@PathVariable Long id) {
        badgeService.adminDeleteBadge(id);
        return Result.success();
    }
}
