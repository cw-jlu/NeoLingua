package com.speakmaster.user.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.speakmaster.user.dto.BadgeDTO;

import java.util.List;

/**
 * 徽章服务接口
 * 
 * @author SpeakMaster
 */
public interface IBadgeService {

    /**
     * 获取用户徽章列表
     */
    List<BadgeDTO> getUserBadges(Long userId);

    /**
     * 根据ID获取徽章
     */
    BadgeDTO getBadgeById(Long badgeId);

    /**
     * 领取徽章
     */
    void claimBadge(Long userId, Long badgeId);

    // ==================== 管理端方法 ====================

    /**
     * [管理端] 分页查询徽章列表
     */
    Page<BadgeDTO> adminGetBadgeList(int page, int size);

    /**
     * [管理端] 创建徽章
     */
    BadgeDTO adminCreateBadge(BadgeDTO badgeDTO);

    /**
     * [管理端] 更新徽章
     */
    BadgeDTO adminUpdateBadge(Long badgeId, BadgeDTO badgeDTO);

    /**
     * [管理端] 删除徽章
     */
    void adminDeleteBadge(Long badgeId);
}
