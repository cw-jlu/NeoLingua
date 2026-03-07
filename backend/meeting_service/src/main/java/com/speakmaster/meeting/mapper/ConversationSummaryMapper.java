package com.speakmaster.meeting.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.speakmaster.meeting.entity.ConversationSummary;
import org.apache.ibatis.annotations.Mapper;

/**
 * 对话总结Mapper
 * 
 * @author SpeakMaster
 */
@Mapper
public interface ConversationSummaryMapper extends BaseMapper<ConversationSummary> {
}
