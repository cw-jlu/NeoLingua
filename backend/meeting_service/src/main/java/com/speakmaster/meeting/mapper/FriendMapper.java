package com.speakmaster.meeting.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.speakmaster.meeting.entity.Friend;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * 好友 Mapper
 * 
 * @author SpeakMaster
 */
@Mapper
public interface FriendMapper extends BaseMapper<Friend> {

    /**
     * 根据用户ID和状态分页查询好友
     */
    IPage<Friend> selectByUserIdAndStatusPage(Page<?> page, @Param("userId") Long userId, @Param("status") Integer status, @Param("deleted") Integer deleted);

    /**
     * 根据好友ID和状态分页查询好友请求
     */
    IPage<Friend> selectByFriendIdAndStatusPage(Page<?> page, @Param("friendId") Long friendId, @Param("status") Integer status, @Param("deleted") Integer deleted);
}
