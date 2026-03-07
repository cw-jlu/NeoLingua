package com.speakmaster.notification.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.speakmaster.common.enums.ErrorCode;
import com.speakmaster.common.exception.BusinessException;
import com.speakmaster.common.utils.RedisUtil;
import com.speakmaster.notification.dto.NotificationDTO;
import com.speakmaster.notification.entity.Notification;
import com.speakmaster.notification.mapper.NotificationMapper;
import com.speakmaster.notification.service.INotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

/**
 * 通知服务实现类
 * 
 * @author SpeakMaster
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationServiceImpl extends ServiceImpl<NotificationMapper, Notification> implements INotificationService {

    private final RedisUtil redisUtil;
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @Override
    @Transactional(rollbackFor = Exception.class)
    public NotificationDTO createNotification(NotificationDTO notificationDTO) {
        Notification notification = new Notification();
        notification.setReceiverId(notificationDTO.getReceiverId());
        notification.setSenderId(notificationDTO.getSenderId());
        notification.setType(notificationDTO.getType());
        notification.setTitle(notificationDTO.getTitle());
        notification.setContent(notificationDTO.getContent());
        notification.setRelatedId(notificationDTO.getRelatedId());
        notification.setIsRead(0);

        this.save(notification);

        // 缓存未读通知数到Redis
        String key = "notification:unread:" + notificationDTO.getReceiverId();
        redisUtil.increment(key);

        log.info("创建通知成功: notificationId={}, receiverId={}", notification.getId(), notificationDTO.getReceiverId());
        return convertToDTO(notification);
    }

    @Override
    public Page<NotificationDTO> getNotificationList(Long userId, int page, int size) {
        LambdaQueryWrapper<Notification> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Notification::getReceiverId, userId)
                .eq(Notification::getDeleted, 0)
                .orderByDesc(Notification::getCreateTime);
        
        Page<Notification> pageRequest = new Page<>(page + 1, size);
        Page<Notification> notifications = this.page(pageRequest, wrapper);
        
        return (Page<NotificationDTO>) notifications.convert(this::convertToDTO);
    }

    @Override
    public Page<NotificationDTO> getUnreadNotifications(Long userId, int page, int size) {
        LambdaQueryWrapper<Notification> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Notification::getReceiverId, userId)
                .eq(Notification::getIsRead, 0)
                .eq(Notification::getDeleted, 0)
                .orderByDesc(Notification::getCreateTime);
        
        Page<Notification> pageRequest = new Page<>(page + 1, size);
        Page<Notification> notifications = this.page(pageRequest, wrapper);
        
        return (Page<NotificationDTO>) notifications.convert(this::convertToDTO);
    }

    @Override
    public NotificationDTO getNotificationById(Long notificationId, Long userId) {
        LambdaQueryWrapper<Notification> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Notification::getId, notificationId)
                .eq(Notification::getDeleted, 0);
        Notification notification = this.getOne(wrapper);
        
        if (notification == null) {
            throw new BusinessException(ErrorCode.NOTIFICATION_NOT_FOUND);
        }

        // 检查权限
        if (!notification.getReceiverId().equals(userId)) {
            throw new BusinessException(ErrorCode.NO_PERMISSION);
        }

        return convertToDTO(notification);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void markAsRead(Long notificationId, Long userId) {
        LambdaQueryWrapper<Notification> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Notification::getId, notificationId)
                .eq(Notification::getDeleted, 0);
        Notification notification = this.getOne(wrapper);
        
        if (notification == null) {
            throw new BusinessException(ErrorCode.NOTIFICATION_NOT_FOUND);
        }

        // 检查权限
        if (!notification.getReceiverId().equals(userId)) {
            throw new BusinessException(ErrorCode.NO_PERMISSION);
        }

        if (notification.getIsRead() == 0) {
            notification.setIsRead(1);
            notification.setReadTime(LocalDateTime.now().format(FORMATTER));
            this.updateById(notification);

            // 更新Redis中的未读数
            String key = "notification:unread:" + userId;
            Long count = redisUtil.decrement(key);
            if (count != null && count < 0) {
                redisUtil.delete(key);
            }

            log.info("标记通知已读: notificationId={}, userId={}", notificationId, userId);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void markAllAsRead(Long userId) {
        LambdaQueryWrapper<Notification> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Notification::getReceiverId, userId)
                .eq(Notification::getIsRead, 0)
                .eq(Notification::getDeleted, 0)
                .last("LIMIT 1000");
        
        java.util.List<Notification> unreadNotifications = this.list(wrapper);

        String readTime = LocalDateTime.now().format(FORMATTER);
        for (Notification notification : unreadNotifications) {
            notification.setIsRead(1);
            notification.setReadTime(readTime);
        }

        this.updateBatchById(unreadNotifications);

        // 清空Redis中的未读数
        String key = "notification:unread:" + userId;
        redisUtil.delete(key);

        log.info("全部标记已读: userId={}, count={}", userId, unreadNotifications.size());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteNotification(Long notificationId, Long userId) {
        LambdaQueryWrapper<Notification> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Notification::getId, notificationId)
                .eq(Notification::getDeleted, 0);
        Notification notification = this.getOne(wrapper);
        
        if (notification == null) {
            throw new BusinessException(ErrorCode.NOTIFICATION_NOT_FOUND);
        }

        // 检查权限
        if (!notification.getReceiverId().equals(userId)) {
            throw new BusinessException(ErrorCode.NO_PERMISSION);
        }

        notification.markDeleted();
        this.updateById(notification);

        // 如果是未读通知,更新Redis
        if (notification.getIsRead() == 0) {
            String key = "notification:unread:" + userId;
            redisUtil.decrement(key);
        }

        log.info("删除通知: notificationId={}", notificationId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void clearNotifications(Long userId) {
        LambdaQueryWrapper<Notification> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Notification::getReceiverId, userId)
                .eq(Notification::getDeleted, 0)
                .last("LIMIT 1000");
        
        java.util.List<Notification> notifications = this.list(wrapper);

        for (Notification notification : notifications) {
            notification.markDeleted();
        }

        this.updateBatchById(notifications);

        // 清空Redis
        String key = "notification:unread:" + userId;
        redisUtil.delete(key);

        log.info("清空通知: userId={}, count={}", userId, notifications.size());
    }

    @Override
    public Long getUnreadCount(Long userId) {
        // 优先从Redis获取
        String key = "notification:unread:" + userId;
        Long count = redisUtil.get(key, Long.class);

        if (count == null) {
            // Redis中没有,从数据库查询并缓存
            LambdaQueryWrapper<Notification> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(Notification::getReceiverId, userId)
                    .eq(Notification::getIsRead, 0)
                    .eq(Notification::getDeleted, 0);
            count = this.count(wrapper);
            redisUtil.set(key, count, 3600);
        }

        return count;
    }

    // ==================== 管理端方法 ====================

    @Override
    public Page<NotificationDTO> adminGetNotificationList(Integer type, int page, int size) {
        Page<Notification> pageRequest = new Page<>(page + 1, size);
        Page<Notification> notifications = this.page(pageRequest);
        return (Page<NotificationDTO>) notifications.convert(this::convertToDTO);
    }

    @Override
    public NotificationDTO adminGetNotificationById(Long notificationId) {
        LambdaQueryWrapper<Notification> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Notification::getId, notificationId)
                .eq(Notification::getDeleted, 0);
        Notification notification = this.getOne(wrapper);
        
        if (notification == null) {
            throw new BusinessException(ErrorCode.NOTIFICATION_NOT_FOUND);
        }
        return convertToDTO(notification);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void adminDeleteNotification(Long notificationId) {
        LambdaQueryWrapper<Notification> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Notification::getId, notificationId)
                .eq(Notification::getDeleted, 0);
        Notification notification = this.getOne(wrapper);
        
        if (notification == null) {
            throw new BusinessException(ErrorCode.NOTIFICATION_NOT_FOUND);
        }
        notification.markDeleted();
        this.updateById(notification);
        log.info("[管理端] 删除通知: notificationId={}", notificationId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public NotificationDTO adminBroadcast(String title, String content) {
        // 创建系统广播通知(receiverId=0表示广播)
        Notification notification = new Notification();
        notification.setReceiverId(0L);
        notification.setSenderId(null);
        notification.setType(0); // 系统通知
        notification.setTitle(title);
        notification.setContent(content);
        notification.setIsRead(0);
        this.save(notification);
        log.info("[管理端] 发送广播通知: notificationId={}", notification.getId());
        return convertToDTO(notification);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public NotificationDTO adminSendTargeted(Long receiverId, String title, String content) {
        NotificationDTO dto = new NotificationDTO();
        dto.setReceiverId(receiverId);
        dto.setType(0); // 系统通知
        dto.setTitle(title);
        dto.setContent(content);
        return createNotification(dto);
    }

    @Override
    public Map<String, Object> adminGetNotificationStatistics() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("totalNotifications", this.count());
        return stats;
    }

    /**
     * 转换为DTO
     */
    private NotificationDTO convertToDTO(Notification notification) {
        NotificationDTO dto = new NotificationDTO();
        dto.setId(notification.getId());
        dto.setReceiverId(notification.getReceiverId());
        dto.setSenderId(notification.getSenderId());
        dto.setType(notification.getType());
        dto.setTitle(notification.getTitle());
        dto.setContent(notification.getContent());
        dto.setRelatedId(notification.getRelatedId());
        dto.setIsRead(notification.getIsRead());
        dto.setReadTime(notification.getReadTime());
        dto.setCreateTime(notification.getCreateTime() != null ? notification.getCreateTime().toString() : null);
        // TODO: 从User Service获取发送者信息
        return dto;
    }
}
