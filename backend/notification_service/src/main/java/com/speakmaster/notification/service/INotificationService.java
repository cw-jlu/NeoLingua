package com.speakmaster.notification.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.speakmaster.notification.dto.NotificationDTO;
import com.speakmaster.notification.entity.Notification;

import java.util.Map;

/**
 * 通知服务接口
 * 
 * @author SpeakMaster
 */
public interface INotificationService extends IService<Notification> {

    /**
     * 创建通知
     */
    NotificationDTO createNotification(NotificationDTO notificationDTO);

    /**
     * 获取通知列表
     */
    Page<NotificationDTO> getNotificationList(Long userId, int page, int size);

    /**
     * 获取未读通知列表
     */
    Page<NotificationDTO> getUnreadNotifications(Long userId, int page, int size);

    /**
     * 获取通知详情
     */
    NotificationDTO getNotificationById(Long notificationId, Long userId);

    /**
     * 标记已读
     */
    void markAsRead(Long notificationId, Long userId);

    /**
     * 全部标记已读
     */
    void markAllAsRead(Long userId);

    /**
     * 删除通知
     */
    void deleteNotification(Long notificationId, Long userId);

    /**
     * 清空通知
     */
    void clearNotifications(Long userId);

    /**
     * 获取未读数量
     */
    Long getUnreadCount(Long userId);

    // ==================== 管理端方法 ====================

    /**
     * [管理端] 分页查询所有通知
     */
    Page<NotificationDTO> adminGetNotificationList(Integer type, int page, int size);

    /**
     * [管理端] 获取通知详情(无需权限检查)
     */
    NotificationDTO adminGetNotificationById(Long notificationId);

    /**
     * [管理端] 删除通知(无需权限检查)
     */
    void adminDeleteNotification(Long notificationId);

    /**
     * [管理端] 发送广播通知(给所有用户)
     */
    NotificationDTO adminBroadcast(String title, String content);

    /**
     * [管理端] 发送定向通知
     */
    NotificationDTO adminSendTargeted(Long receiverId, String title, String content);

    /**
     * [管理端] 通知统计
     */
    Map<String, Object> adminGetNotificationStatistics();
}
