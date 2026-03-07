package com.speakmaster.meeting.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.speakmaster.meeting.entity.MeetingMessage;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * Meeting消息 Mapper
 * 
 * @author SpeakMaster
 */
@Mapper
public interface MeetingMessageMapper extends BaseMapper<MeetingMessage> {

    /**
     * 根据Meeting ID分页查询消息
     */
    IPage<MeetingMessage> selectByMeetingIdPage(Page<?> page, @Param("meetingId") Long meetingId, @Param("deleted") Integer deleted);
}
