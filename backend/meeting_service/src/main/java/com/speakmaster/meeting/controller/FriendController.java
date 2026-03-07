package com.speakmaster.meeting.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.speakmaster.common.dto.Result;
import com.speakmaster.common.context.UserContextHolder;
import com.speakmaster.meeting.dto.FriendDTO;
import com.speakmaster.meeting.service.IFriendService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 好友控制器
 * 
 * @author SpeakMaster
 */
@RestController
@RequiredArgsConstructor
public class FriendController {

    private final IFriendService friendService;

    /**
     * 获取好友列表 (用户端)
     */
    @GetMapping("/user/friends")
    public Result<Page<FriendDTO>> getFriendList(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        Long userId = UserContextHolder.getCurrentUserId();
        Page<FriendDTO> friends = friendService.getFriendList(userId, page, size);
        return Result.success(friends);
    }

    /**
     * 获取好友请求列表 (用户端)
     */
    @GetMapping("/user/friends/requests")
    public Result<Page<FriendDTO>> getFriendRequests(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        Long userId = UserContextHolder.getCurrentUserId();
        Page<FriendDTO> requests = friendService.getFriendRequests(userId, page, size);
        return Result.success(requests);
    }

    /**
     * 发送好友请求(用户端)
     */
    @PostMapping("/user/friends/requests")
    public Result<FriendDTO> sendFriendRequest(
            @RequestParam Long friendId) {
        Long userId = UserContextHolder.getCurrentUserId();
        FriendDTO friend = friendService.sendFriendRequest(userId, friendId);
        return Result.success(friend);
    }

    /**
     * 接受好友请求 (用户端)
     */
    @PutMapping("/user/friends/requests/{id}/accept")
    public Result<FriendDTO> acceptFriendRequest(
            @PathVariable Long id) {
        Long userId = UserContextHolder.getCurrentUserId();
        FriendDTO friend = friendService.acceptFriendRequest(id, userId);
        return Result.success(friend);
    }

    /**
     * 拒绝好友请求 (用户端)
     */
    @PutMapping("/user/friends/requests/{id}/reject")
    public Result<Void> rejectFriendRequest(
            @PathVariable Long id) {
        Long userId = UserContextHolder.getCurrentUserId();
        friendService.rejectFriendRequest(id, userId);
        return Result.success();
    }

    /**
     * 删除好友 (用户端)
     */
    @DeleteMapping("/user/friends/{id}")
    public Result<Void> deleteFriend(
            @PathVariable Long id) {
        Long userId = UserContextHolder.getCurrentUserId();
        friendService.deleteFriend(id, userId);
        return Result.success();
    }
}
