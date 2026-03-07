package com.speakmaster.meeting.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.speakmaster.common.dto.Result;
import com.speakmaster.common.context.UserContextHolder;
import com.speakmaster.meeting.dto.MeetingDTO;
import com.speakmaster.meeting.dto.MeetingMessageDTO;
import com.speakmaster.meeting.service.IMeetingMessageService;
import com.speakmaster.meeting.service.IMeetingService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * Meeting控制器
 * 
 * @author SpeakMaster
 */
@RestController
@RequiredArgsConstructor
public class MeetingController {

    private final IMeetingService meetingService;
    private final IMeetingMessageService messageService;

    /**
     * 创建Meeting (用户端)
     */
    @PostMapping("/user/meetings")
    public Result<MeetingDTO> createMeeting(
            @RequestBody MeetingDTO meetingDTO) {
        Long userId = UserContextHolder.getCurrentUserId();
        MeetingDTO meeting = meetingService.createMeeting(meetingDTO, userId);
        return Result.success(meeting);
    }

    /**
     * 获取Meeting列表 (用户端)
     */
    @GetMapping("/user/meetings")
    public Result<Page<MeetingDTO>> getMeetingList(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        Long userId = UserContextHolder.getCurrentUserId();
        Page<MeetingDTO> meetings = meetingService.getMeetingList(userId, page, size);
        return Result.success(meetings);
    }

    /**
     * 获取Meeting详情 (用户端)
     */
    @GetMapping("/user/meetings/{id}")
    public Result<MeetingDTO> getMeetingById(@PathVariable Long id) {
        MeetingDTO meeting = meetingService.getMeetingById(id);
        return Result.success(meeting);
    }

    /**
     * 加入Meeting (用户端)
     */
    @PostMapping("/user/meetings/{id}/join")
    public Result<Void> joinMeeting(
            @PathVariable Long id) {
        Long userId = UserContextHolder.getCurrentUserId();
        meetingService.joinMeeting(id, userId);
        return Result.success();
    }

    /**
     * 离开Meeting (用户端)
     */
    @PostMapping("/user/meetings/{id}/leave")
    public Result<Void> leaveMeeting(
            @PathVariable Long id) {
        Long userId = UserContextHolder.getCurrentUserId();
        meetingService.leaveMeeting(id, userId);
        return Result.success();
    }

    /**
     * 开始Meeting (用户端)
     */
    @PostMapping("/user/meetings/{id}/start")
    public Result<MeetingDTO> startMeeting(
            @PathVariable Long id) {
        Long userId = UserContextHolder.getCurrentUserId();
        MeetingDTO meeting = meetingService.startMeeting(id, userId);
        return Result.success(meeting);
    }

    /**
     * 结束Meeting (用户端)
     */
    @PostMapping("/user/meetings/{id}/end")
    public Result<MeetingDTO> endMeeting(
            @PathVariable Long id) {
        Long userId = UserContextHolder.getCurrentUserId();
        MeetingDTO meeting = meetingService.endMeeting(id, userId);
        return Result.success(meeting);
    }

    /**
     * 删除Meeting (用户端)
     */
    @DeleteMapping("/user/meetings/{id}")
    public Result<Void> deleteMeeting(
            @PathVariable Long id) {
        Long userId = UserContextHolder.getCurrentUserId();
        meetingService.deleteMeeting(id, userId);
        return Result.success();
    }

    /**
     * 发送消息(用户端)
     */
    @PostMapping("/user/meetings/{id}/messages")
    public Result<MeetingMessageDTO> sendMessage(
            @PathVariable Long id,
            @RequestBody MeetingMessageDTO messageDTO) {
        Long userId = UserContextHolder.getCurrentUserId();
        MeetingMessageDTO message = messageService.sendMessage(id, messageDTO, userId);
        return Result.success(message);
    }

    /**
     * 获取消息列表 (用户端)
     */
    @GetMapping("/user/meetings/{id}/messages")
    public Result<Page<MeetingMessageDTO>> getMessages(
            @PathVariable Long id,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Long userId = UserContextHolder.getCurrentUserId();
        Page<MeetingMessageDTO> messages = messageService.getMessages(id, userId, page, size);
        return Result.success(messages);
    }

    /**
     * 删除消息 (用户端)
     */
    @DeleteMapping("/user/meetings/{meetingId}/messages/{messageId}")
    public Result<Void> deleteMessage(
            @PathVariable Long messageId) {
        Long userId = UserContextHolder.getCurrentUserId();
        messageService.deleteMessage(messageId, userId);
        return Result.success();
    }

    /**
     * 邀请好友加入Meeting (用户端)
     */
    @PostMapping("/user/meetings/{id}/invite/{friendId}")
    public Result<Void> inviteFriend(
            @PathVariable Long id,
            @PathVariable Long friendId) {
        Long userId = UserContextHolder.getCurrentUserId();
        meetingService.inviteFriend(id, friendId, userId);
        return Result.success();
    }

    /**
     * 添加AI参与者到Meeting (用户端)
     */
    @PostMapping("/user/meetings/{id}/add-ai")
    public Result<Void> addAiParticipant(
            @PathVariable Long id,
            @RequestParam(required = false) Long roleId,
            @RequestParam(required = false, defaultValue = "AI助手") String aiName) {
        Long userId = UserContextHolder.getCurrentUserId();
        meetingService.addAiParticipant(id, roleId, aiName, userId);
        return Result.success();
    }

    /**
     * 移除AI参与者 (用户端)
     */
    @DeleteMapping("/user/meetings/{meetingId}/participants/{participantId}")
    public Result<Void> removeParticipant(
            @PathVariable Long meetingId,
            @PathVariable Long participantId) {
        Long userId = UserContextHolder.getCurrentUserId();
        meetingService.removeParticipant(meetingId, participantId, userId);
        return Result.success();
    }

    /**
     * 更新AI参与者设定 (用户端)
     */
    @PutMapping("/user/meetings/{meetingId}/participants/{participantId}")
    public Result<Void> updateAiParticipant(
            @PathVariable Long meetingId,
            @PathVariable Long participantId,
            @RequestBody MeetingDTO.ParticipantInfo participantInfo) {
        Long userId = UserContextHolder.getCurrentUserId();
        meetingService.updateAiParticipant(meetingId, participantId, participantInfo, userId);
        return Result.success();
    }

    /**
     * 获取可用的AI角色列表 (用户端)
     * 注意: 实际应该调用practice_service的角色接口,这里返回简化版本
     */
    @GetMapping("/user/meetings/ai-roles")
    public Result<java.util.List<java.util.Map<String, Object>>> getAiRoles() {
        java.util.List<java.util.Map<String, Object>> roles = new java.util.ArrayList<>();
        
        // 预设角色
        java.util.Map<String, Object> role1 = new java.util.HashMap<>();
        role1.put("id", 1L);
        role1.put("name", "Emily");
        role1.put("description", "友善的美国女孩，擅长日常对话");
        role1.put("setting", "你是Emily，一个来自纽约的25岁女孩，性格开朗友善，喜欢聊天。说话风格轻松自然，会使用一些美式口语表达。");
        role1.put("type", "preset");
        roles.add(role1);
        
        java.util.Map<String, Object> role2 = new java.util.HashMap<>();
        role2.put("id", 2L);
        role2.put("name", "James");
        role2.put("description", "专业的商务人士，擅长职场英语");
        role2.put("setting", "你是James，一个35岁的英国商务顾问，说话专业严谨，擅长商务英语和正式场合的表达。");
        role2.put("type", "preset");
        roles.add(role2);
        
        java.util.Map<String, Object> role3 = new java.util.HashMap<>();
        role3.put("id", 3L);
        role3.put("name", "Professor Chen");
        role3.put("description", "严格的考试面试官");
        role3.put("setting", "你是Professor Chen，一位严格的英语考试面试官，会针对学生的回答提出深入的问题，帮助他们提高口语水平。");
        role3.put("type", "preset");
        roles.add(role3);
        
        return Result.success(roles);
    }
}
