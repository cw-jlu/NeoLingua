package com.speakmaster.notification.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.speakmaster.common.dto.Result;
import com.speakmaster.common.context.UserContextHolder;
import com.speakmaster.notification.dto.NotificationDTO;
import com.speakmaster.notification.service.INotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 通知控制器
 * 
 * @author SpeakMaster
 */
@RestController
@RequiredArgsConstructor
public class NotificationController {

    private final INotificationService notificationService;

    /**
     * 获取通知列表 (用户端)
     */
    @GetMapping("/user/notifications")
    public Result<Page<NotificationDTO>> getNotificationList(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        Long userId = UserContextHolder.getCurrentUserId();
        Page<NotificationDTO> notifications = notificationService.getNotificationList(userId, page, size);
        return Result.success(notifications);
    }

    /**
     * 获取未读通知列表 (用户端)
     */
    @GetMapping("/user/notifications/unread")
    public Result<Page<NotificationDTO>> getUnreadNotifications(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        Long userId = UserContextHolder.getCurrentUserId();
        Page<NotificationDTO> notifications = notificationService.getUnreadNotifications(userId, page, size);
        return Result.success(notifications);
    }

    /**
     * 获取通知详情 (用户端)
     */
    @GetMapping("/user/notifications/{id}")
    public Result<NotificationDTO> getNotificationById(
            @PathVariable Long id) {
        Long userId = UserContextHolder.getCurrentUserId();
        NotificationDTO notification = notificationService.getNotificationById(id, userId);
        return Result.success(notification);
    }

    /**
     * 标记已读 (用户端)
     */
    @PutMapping("/user/notifications/{id}/read")
    public Result<Void> markAsRead(
            @PathVariable Long id) {
        Long userId = UserContextHolder.getCurrentUserId();
        notificationService.markAsRead(id, userId);
        return Result.success();
    }

    /**
     * 全部标记已读 (用户端)
     */
    @PutMapping("/user/notifications/read-all")
    public Result<Void> markAllAsRead() {
        Long userId = UserContextHolder.getCurrentUserId();
        notificationService.markAllAsRead(userId);
        return Result.success();
    }

    /**
     * 删除通知 (用户端)
     */
    @DeleteMapping("/user/notifications/{id}")
    public Result<Void> deleteNotification(
            @PathVariable Long id) {
        Long userId = UserContextHolder.getCurrentUserId();
        notificationService.deleteNotification(id, userId);
        return Result.success();
    }

    /**
     * 清空通知 (用户端)
     */
    @DeleteMapping("/user/notifications/clear")
    public Result<Void> clearNotifications() {
        Long userId = UserContextHolder.getCurrentUserId();
        notificationService.clearNotifications(userId);
        return Result.success();
    }

    /**
     * 获取未读数量 (用户端)
     */
    @GetMapping("/user/notifications/unread/count")
    public Result<Long> getUnreadCount() {
        Long userId = UserContextHolder.getCurrentUserId();
        Long count = notificationService.getUnreadCount(userId);
        return Result.success(count);
    }
}
