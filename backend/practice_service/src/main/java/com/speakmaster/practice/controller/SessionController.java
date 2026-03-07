package com.speakmaster.practice.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.speakmaster.common.dto.Result;
import com.speakmaster.common.context.UserContextHolder;
import com.speakmaster.practice.dto.MessageDTO;
import com.speakmaster.practice.dto.SessionDTO;
import com.speakmaster.practice.service.IMessageService;
import com.speakmaster.practice.service.ISessionService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 练习会话控制器
 * 
 * @author SpeakMaster
 */
@RestController
@RequiredArgsConstructor
public class SessionController {

    private final ISessionService sessionService;
    private final IMessageService messageService;

    /**
     * 创建练习会话 (用户端)
     */
    @PostMapping("/user/practice/sessions")
    public Result<SessionDTO> createSession(
            @RequestBody SessionDTO sessionDTO) {
        Long userId = UserContextHolder.getCurrentUserId();
        SessionDTO session = sessionService.createSession(sessionDTO, userId);
        return Result.success(session);
    }

    /**
     * 获取会话列表 (用户端)
     */
    @GetMapping("/user/practice/sessions")
    public Result<Page<SessionDTO>> getUserSessions(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        Long userId = UserContextHolder.getCurrentUserId();
        Page<SessionDTO> sessions = sessionService.getUserSessions(userId, page, size);
        return Result.success(sessions);
    }

    /**
     * 获取会话详情 (用户端)
     */
    @GetMapping("/user/practice/sessions/{id}")
    public Result<SessionDTO> getSessionById(
            @PathVariable Long id) {
        Long userId = UserContextHolder.getCurrentUserId();
        SessionDTO session = sessionService.getSessionById(id, userId);
        return Result.success(session);
    }

    /**
     * 结束会话 (用户端)
     */
    @PostMapping("/user/practice/sessions/{id}/end")
    public Result<SessionDTO> endSession(
            @PathVariable Long id,
            @RequestParam Integer score,
            @RequestParam String feedback) {
        Long userId = UserContextHolder.getCurrentUserId();
        SessionDTO session = sessionService.endSession(id, userId, score, feedback);
        return Result.success(session);
    }

    /**
     * 删除会话 (用户端)
     */
    @DeleteMapping("/user/practice/sessions/{id}")
    public Result<Void> deleteSession(
            @PathVariable Long id) {
        Long userId = UserContextHolder.getCurrentUserId();
        sessionService.deleteSession(id, userId);
        return Result.success();
    }

    /**
     * 发送消息(用户端)
     */
    @PostMapping("/user/practice/sessions/{id}/messages")
    public Result<MessageDTO> sendMessage(
            @PathVariable Long id,
            @RequestBody MessageDTO messageDTO) {
        Long userId = UserContextHolder.getCurrentUserId();
        MessageDTO message = messageService.sendMessage(id, messageDTO, userId);
        return Result.success(message);
    }

    /**
     * 获取消息列表 (用户端)
     */
    @GetMapping("/user/practice/sessions/{id}/messages")
    public Result<Page<MessageDTO>> getSessionMessages(
            @PathVariable Long id,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Long userId = UserContextHolder.getCurrentUserId();
        Page<MessageDTO> messages = messageService.getSessionMessages(id, userId, page, size);
        return Result.success(messages);
    }
}
