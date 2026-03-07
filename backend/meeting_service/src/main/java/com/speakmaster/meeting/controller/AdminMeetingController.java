package com.speakmaster.meeting.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.speakmaster.common.dto.Result;
import com.speakmaster.meeting.dto.FriendDTO;
import com.speakmaster.meeting.dto.MeetingDTO;
import com.speakmaster.meeting.service.IFriendService;
import com.speakmaster.meeting.service.IMeetingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 管理端 - Meeting管理控制器
 * 
 * @author SpeakMaster
 */
@Slf4j
@RestController
@RequestMapping("/admin")
@RequiredArgsConstructor
public class AdminMeetingController {

    private final IMeetingService meetingService;
    private final IFriendService friendService;

    // ==================== Meeting管理 ====================

    /**
     * 获取Meeting列表（分页）
     */
    @GetMapping("/meetings")
    public Result<Page<MeetingDTO>> getMeetingList(
            @RequestParam(required = false) Integer status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        Page<MeetingDTO> meetings = meetingService.adminGetMeetingList(status, page, size);
        return Result.success(meetings);
    }

    /**
     * 获取Meeting详情
     */
    @GetMapping("/meetings/{id}")
    public Result<MeetingDTO> getMeetingById(@PathVariable Long id) {
        MeetingDTO meeting = meetingService.getMeetingById(id);
        return Result.success(meeting);
    }

    /**
     * 删除Meeting
     */
    @DeleteMapping("/meetings/{id}")
    public Result<Void> deleteMeeting(@PathVariable Long id) {
        meetingService.adminDeleteMeeting(id);
        return Result.success();
    }

    /**
     * 强制关闭Meeting
     */
    @PutMapping("/meetings/{id}/close")
    public Result<MeetingDTO> closeMeeting(@PathVariable Long id) {
        MeetingDTO meeting = meetingService.adminCloseMeeting(id);
        return Result.success(meeting);
    }

    /**
     * Meeting统计
     */
    @GetMapping("/meetings/statistics")
    public Result<Map<String, Object>> getMeetingStatistics() {
        Map<String, Object> stats = meetingService.adminGetMeetingStatistics();
        return Result.success(stats);
    }

    // ==================== 好友关系管理 ====================

    /**
     * 获取好友关系列表（分页）
     */
    @GetMapping("/friends")
    public Result<Page<FriendDTO>> getFriendList(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        Page<FriendDTO> friends = friendService.adminGetFriendList(page, size);
        return Result.success(friends);
    }

    /**
     * 删除好友关系
     */
    @DeleteMapping("/friends/{id}")
    public Result<Void> deleteFriend(@PathVariable Long id) {
        friendService.adminDeleteFriend(id);
        return Result.success();
    }
}
