package com.speakmaster.meeting.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.speakmaster.meeting.entity.MeetingParticipant;
import org.apache.ibatis.annotations.Mapper;

/**
 * Meeting参与者 Mapper
 * 
 * @author SpeakMaster
 */
@Mapper
public interface MeetingParticipantMapper extends BaseMapper<MeetingParticipant> {
}
