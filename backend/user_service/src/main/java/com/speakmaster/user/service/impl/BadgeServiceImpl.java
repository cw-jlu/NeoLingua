package com.speakmaster.user.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.speakmaster.common.enums.ErrorCode;
import com.speakmaster.common.exception.BusinessException;
import com.speakmaster.user.dto.BadgeDTO;
import com.speakmaster.user.entity.Badge;
import com.speakmaster.user.entity.User;
import com.speakmaster.user.entity.UserBadge;
import com.speakmaster.user.mapper.BadgeMapper;
import com.speakmaster.user.mapper.UserBadgeMapper;
import com.speakmaster.user.mapper.UserMapper;
import com.speakmaster.user.service.IBadgeService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 徽章服务实现类
 * 
 * @author SpeakMaster
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class BadgeServiceImpl implements IBadgeService {

    private final BadgeMapper badgeMapper;
    private final UserBadgeMapper userBadgeMapper;
    private final UserMapper userMapper;
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @Override
    public List<BadgeDTO> getUserBadges(Long userId) {
        // 获取所有启用的徽章
        List<Badge> allBadges = badgeMapper.selectByStatusAndDeletedOrderBySortOrderAsc(1, 0);
        
        // 获取用户已获得的徽章
        List<UserBadge> userBadges = userBadgeMapper.selectByUserIdAndDeleted(userId, 0);
        
        // 转换为DTO
        return allBadges.stream().map(badge -> {
            BadgeDTO dto = convertToDTO(badge);
            
            // 检查用户是否已获得该徽章
            userBadges.stream()
                    .filter(ub -> ub.getBadgeId().equals(badge.getId()))
                    .findFirst()
                    .ifPresent(ub -> {
                        dto.setObtained(true);
                        dto.setObtainedTime(ub.getObtainedTime());
                    });
            
            if (dto.getObtained() == null) {
                dto.setObtained(false);
            }
            
            return dto;
        }).collect(Collectors.toList());
    }

    @Override
    public BadgeDTO getBadgeById(Long badgeId) {
        Badge badge = badgeMapper.selectById(badgeId);
        if (badge == null || badge.getDeleted() == 1) {
            throw new BusinessException(ErrorCode.BADGE_NOT_FOUND);
        }
        return convertToDTO(badge);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void claimBadge(Long userId, Long badgeId) {
        // 检查徽章是否存在
        Badge badge = badgeMapper.selectById(badgeId);
        if (badge == null || badge.getDeleted() == 1 || badge.getStatus() != 1) {
            throw new BusinessException(ErrorCode.BADGE_NOT_FOUND);
        }

        // 检查用户是否已拥有该徽章
        if (userBadgeMapper.existsByUserIdAndBadgeIdAndDeleted(userId, badgeId, 0)) {
            throw new BusinessException(ErrorCode.BADGE_ALREADY_CLAIMED);
        }

        // 检查用户是否满足条件
        User user = userMapper.selectById(userId);
        if (user == null || user.getDeleted() == 1) {
            throw new BusinessException(ErrorCode.USER_NOT_FOUND);
        }

        if (badge.getRequiredPoints() != null && user.getPoints() < badge.getRequiredPoints()) {
            throw new BusinessException(ErrorCode.INSUFFICIENT_POINTS);
        }

        // 创建用户徽章关联
        UserBadge userBadge = new UserBadge();
        userBadge.setUserId(userId);
        userBadge.setBadgeId(badgeId);
        userBadge.setObtainedTime(LocalDateTime.now().format(FORMATTER));
        userBadgeMapper.insert(userBadge);

        log.info("用户领取徽章成功: userId={}, badgeId={}", userId, badgeId);
    }

    @Override
    public Page<BadgeDTO> adminGetBadgeList(int page, int size) {
        Page<Badge> badgePage = new Page<>(page + 1, size);
        badgeMapper.selectPage(badgePage, new LambdaQueryWrapper<Badge>()
                .eq(Badge::getDeleted, 0)
                .orderByAsc(Badge::getSortOrder));
        
        Page<BadgeDTO> dtoPage = new Page<>(badgePage.getCurrent(), badgePage.getSize(), badgePage.getTotal());
        dtoPage.setRecords(badgePage.getRecords().stream().map(this::convertToDTO).toList());
        
        return dtoPage;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public BadgeDTO adminCreateBadge(BadgeDTO badgeDTO) {
        Badge badge = new Badge();
        badge.setName(badgeDTO.getName());
        badge.setDescription(badgeDTO.getDescription());
        badge.setIcon(badgeDTO.getIcon());
        badge.setType(badgeDTO.getType());
        badge.setConditionDesc(badgeDTO.getConditionDesc());
        badge.setRequiredPoints(badgeDTO.getRequiredPoints());
        badge.setStatus(1);
        badge.setSortOrder(badgeDTO.getType() != null ? badgeDTO.getType() * 100 : 0);
        
        badgeMapper.insert(badge);
        log.info("[管理端] 创建徽章: badgeId={}, name={}", badge.getId(), badge.getName());
        
        return convertToDTO(badge);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public BadgeDTO adminUpdateBadge(Long badgeId, BadgeDTO badgeDTO) {
        Badge badge = badgeMapper.selectById(badgeId);
        if (badge == null || badge.getDeleted() == 1) {
            throw new BusinessException(ErrorCode.BADGE_NOT_FOUND);
        }

        if (badgeDTO.getName() != null) badge.setName(badgeDTO.getName());
        if (badgeDTO.getDescription() != null) badge.setDescription(badgeDTO.getDescription());
        if (badgeDTO.getIcon() != null) badge.setIcon(badgeDTO.getIcon());
        if (badgeDTO.getType() != null) badge.setType(badgeDTO.getType());
        if (badgeDTO.getConditionDesc() != null) badge.setConditionDesc(badgeDTO.getConditionDesc());
        if (badgeDTO.getRequiredPoints() != null) badge.setRequiredPoints(badgeDTO.getRequiredPoints());

        badgeMapper.updateById(badge);
        log.info("[管理端] 更新徽章: badgeId={}", badgeId);

        return convertToDTO(badge);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void adminDeleteBadge(Long badgeId) {
        Badge badge = badgeMapper.selectById(badgeId);
        if (badge == null || badge.getDeleted() == 1) {
            throw new BusinessException(ErrorCode.BADGE_NOT_FOUND);
        }

        badge.markDeleted();
        badgeMapper.updateById(badge);
        log.info("[管理端] 删除徽章: badgeId={}", badgeId);
    }

    /**
     * 转换为DTO
     */
    private BadgeDTO convertToDTO(Badge badge) {
        BadgeDTO dto = new BadgeDTO();
        dto.setId(badge.getId());
        dto.setName(badge.getName());
        dto.setDescription(badge.getDescription());
        dto.setIcon(badge.getIcon());
        dto.setType(badge.getType());
        dto.setConditionDesc(badge.getConditionDesc());
        dto.setRequiredPoints(badge.getRequiredPoints());
        return dto;
    }
}
