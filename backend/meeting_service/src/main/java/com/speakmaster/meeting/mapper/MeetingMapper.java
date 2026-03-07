package com.speakmaster.meeting.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.speakmaster.meeting.entity.Meeting;
import org.apache.ibatis.annotations.Mapper;

/**
 * Meeting Mapper
 * 
 * @author SpeakMaster
 */
@Mapper
public interface MeetingMapper extends BaseMapper<Meeting> {
}
