package com.speakmaster.meeting.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.speakmaster.common.enums.ErrorCode;
import com.speakmaster.common.exception.BusinessException;
import com.speakmaster.meeting.dto.FriendDTO;
import com.speakmaster.meeting.entity.Friend;
import com.speakmaster.meeting.mapper.FriendMapper;
import com.speakmaster.meeting.service.IFriendService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 好友服务实现
 * 
 * @author SpeakMaster
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class FriendServiceImpl implements IFriendService {

    private final FriendMapper friendMapper;

    @Override
    public Page<FriendDTO> getFriendList(Long userId, int page, int size) {
        Page<Friend> friendPage = new Page<>(page, size);
        LambdaQueryWrapper<Friend> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Friend::getUserId, userId)
                .eq(Friend::getStatus, 1)
                .eq(Friend::getDeleted, 0)
                .orderByDesc(Friend::getCreateTime);
        
        Page<Friend> result = friendMapper.selectPage(friendPage, wrapper);
        return (Page<FriendDTO>) result.convert(this::convertToDTO);
    }

    @Override
    public Page<FriendDTO> getFriendRequests(Long userId, int page, int size) {
        Page<Friend> friendPage = new Page<>(page, size);
        LambdaQueryWrapper<Friend> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Friend::getFriendId, userId)
                .eq(Friend::getStatus, 0)
                .eq(Friend::getDeleted, 0)
                .orderByDesc(Friend::getCreateTime);
        
        Page<Friend> result = friendMapper.selectPage(friendPage, wrapper);
        return (Page<FriendDTO>) result.convert(this::convertToDTO);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public FriendDTO sendFriendRequest(Long userId, Long friendId) {
        // 检查是否已经是好友
        LambdaQueryWrapper<Friend> friendWrapper = new LambdaQueryWrapper<>();
        friendWrapper.eq(Friend::getUserId, userId)
                .eq(Friend::getFriendId, friendId)
                .eq(Friend::getStatus, 1)
                .eq(Friend::getDeleted, 0);
        if (friendMapper.selectCount(friendWrapper) > 0) {
            throw new BusinessException(ErrorCode.ALREADY_FRIENDS);
        }

        // 检查是否已有待处理的请求
        LambdaQueryWrapper<Friend> requestWrapper = new LambdaQueryWrapper<>();
        requestWrapper.eq(Friend::getUserId, userId)
                .eq(Friend::getFriendId, friendId)
                .eq(Friend::getStatus, 0)
                .eq(Friend::getDeleted, 0);
        if (friendMapper.selectCount(requestWrapper) > 0) {
            throw new BusinessException(ErrorCode.FRIEND_REQUEST_EXISTS);
        }

        Friend friend = new Friend();
        friend.setUserId(userId);
        friend.setFriendId(friendId);
        friend.setStatus(0);

        friendMapper.insert(friend);
        log.info("发送好友请求: userId={}, friendId={}", userId, friendId);
        return convertToDTO(friend);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public FriendDTO acceptFriendRequest(Long requestId, Long userId) {
        Friend request = friendMapper.selectById(requestId);
        if (request == null || request.getDeleted() == 1) {
            throw new BusinessException(ErrorCode.FRIEND_REQUEST_NOT_FOUND);
        }

        // 检查权限
        if (!request.getFriendId().equals(userId)) {
            throw new BusinessException(ErrorCode.NO_PERMISSION);
        }

        // 检查状态
        if (request.getStatus() != 0) {
            throw new BusinessException(ErrorCode.FRIEND_REQUEST_ALREADY_HANDLED);
        }

        request.setStatus(1);
        friendMapper.updateById(request);

        // 创建反向好友关系
        Friend reverseFriend = new Friend();
        reverseFriend.setUserId(request.getFriendId());
        reverseFriend.setFriendId(request.getUserId());
        reverseFriend.setStatus(1);
        friendMapper.insert(reverseFriend);

        log.info("接受好友请求: requestId={}, userId={}", requestId, userId);
        return convertToDTO(request);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void rejectFriendRequest(Long requestId, Long userId) {
        Friend request = friendMapper.selectById(requestId);
        if (request == null || request.getDeleted() == 1) {
            throw new BusinessException(ErrorCode.FRIEND_REQUEST_NOT_FOUND);
        }

        // 检查权限
        if (!request.getFriendId().equals(userId)) {
            throw new BusinessException(ErrorCode.NO_PERMISSION);
        }

        request.setStatus(2);
        friendMapper.updateById(request);
        log.info("拒绝好友请求: requestId={}, userId={}", requestId, userId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteFriend(Long friendId, Long userId) {
        LambdaQueryWrapper<Friend> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Friend::getUserId, userId)
                .eq(Friend::getFriendId, friendId)
                .eq(Friend::getDeleted, 0);
        
        Friend friend = friendMapper.selectOne(wrapper);
        if (friend == null) {
            throw new BusinessException(ErrorCode.FRIEND_NOT_FOUND);
        }

        friend.markDeleted();
        friendMapper.updateById(friend);

        // 删除反向关系
        LambdaQueryWrapper<Friend> reverseWrapper = new LambdaQueryWrapper<>();
        reverseWrapper.eq(Friend::getUserId, friendId)
                .eq(Friend::getFriendId, userId)
                .eq(Friend::getDeleted, 0);
        
        Friend reverseFriend = friendMapper.selectOne(reverseWrapper);
        if (reverseFriend != null) {
            reverseFriend.markDeleted();
            friendMapper.updateById(reverseFriend);
        }

        log.info("删除好友: userId={}, friendId={}", userId, friendId);
    }

    @Override
    public Page<FriendDTO> adminGetFriendList(int page, int size) {
        Page<Friend> friendPage = new Page<>(page, size);
        Page<Friend> result = friendMapper.selectPage(friendPage, null);
        return (Page<FriendDTO>) result.convert(this::convertToDTO);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void adminDeleteFriend(Long id) {
        Friend friend = friendMapper.selectById(id);
        if (friend == null || friend.getDeleted() == 1) {
            throw new BusinessException(ErrorCode.FRIEND_NOT_FOUND);
        }
        friend.markDeleted();
        friendMapper.updateById(friend);
        log.info("[管理端] 删除好友关系: id={}", id);
    }

    /**
     * 转换为DTO
     */
    private FriendDTO convertToDTO(Friend friend) {
        FriendDTO dto = new FriendDTO();
        dto.setId(friend.getId());
        dto.setUserId(friend.getUserId());
        dto.setFriendId(friend.getFriendId());
        dto.setStatus(friend.getStatus());
        dto.setRemark(friend.getRemark());
        return dto;
    }
}
