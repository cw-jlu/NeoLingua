package com.speakmaster.meeting.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.speakmaster.meeting.dto.FriendDTO;

/**
 * 好友服务接口
 * 
 * @author SpeakMaster
 */
public interface IFriendService {

    /**
     * 获取好友列表
     */
    Page<FriendDTO> getFriendList(Long userId, int page, int size);

    /**
     * 获取好友请求列表
     */
    Page<FriendDTO> getFriendRequests(Long userId, int page, int size);

    /**
     * 发送好友请求
     */
    FriendDTO sendFriendRequest(Long userId, Long friendId);

    /**
     * 接受好友请求
     */
    FriendDTO acceptFriendRequest(Long requestId, Long userId);

    /**
     * 拒绝好友请求
     */
    void rejectFriendRequest(Long requestId, Long userId);

    /**
     * 删除好友
     */
    void deleteFriend(Long friendId, Long userId);

    /**
     * [管理端] 分页查询所有好友关系
     */
    Page<FriendDTO> adminGetFriendList(int page, int size);

    /**
     * [管理端] 删除好友关系
     */
    void adminDeleteFriend(Long id);
}
