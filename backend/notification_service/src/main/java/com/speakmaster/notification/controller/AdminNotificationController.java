package com.speakmaster.notification.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.speakmaster.common.dto.Result;
import com.speakmaster.notification.dto.NotificationDTO;
import com.speakmaster.notification.service.INotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 管理端 - 通知管理控制器
 * 
 * @author SpeakMaster
 */
@Slf4j
@RestController
@RequestMapping("/admin/notifications")
@RequiredArgsConstructor
public class AdminNotificationController {

    private final INotificationService notificationService;

    /**
     * 获取通知列表（分页）
     */
    @GetMapping
    public Result<Page<NotificationDTO>> getNotificationList(
            @RequestParam(required = false) Integer type,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        Page<NotificationDTO> notifications = notificationService.adminGetNotificationList(type, page, size);
        return Result.success(notifications);
    }

    /**
     * 获取通知详情
     */
    @GetMapping("/{id}")
    public Result<NotificationDTO> getNotificationById(@PathVariable Long id) {
        NotificationDTO notification = notificationService.adminGetNotificationById(id);
        return Result.success(notification);
    }

    /**
     * 删除通知
     */
    @DeleteMapping("/{id}")
    public Result<Void> deleteNotification(@PathVariable Long id) {
        notificationService.adminDeleteNotification(id);
        return Result.success();
    }

    /**
     * 发送广播通知
     */
    @PostMapping("/broadcast")
    public Result<NotificationDTO> broadcast(@RequestBody Map<String, String> request) {
        String title = request.get("title");
        String content = request.get("content");
        NotificationDTO notification = notificationService.adminBroadcast(title, content);
        return Result.success(notification);
    }

    /**
     * 发送定向通知
     */
    @PostMapping("/targeted")
    public Result<NotificationDTO> sendTargeted(@RequestBody Map<String, Object> request) {
        Long receiverId = Long.valueOf(request.get("receiverId").toString());
        String title = (String) request.get("title");
        String content = (String) request.get("content");
        NotificationDTO notification = notificationService.adminSendTargeted(receiverId, title, content);
        return Result.success(notification);
    }

    /**
     * 通知统计
     */
    @GetMapping("/statistics")
    public Result<Map<String, Object>> getStatistics() {
        Map<String, Object> stats = notificationService.adminGetNotificationStatistics();
        return Result.success(stats);
    }
}
