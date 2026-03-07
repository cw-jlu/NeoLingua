package com.speakmaster.practice.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.speakmaster.practice.entity.Message;
import org.apache.ibatis.annotations.Mapper;

/**
 * 消息Mapper
 * 
 * @author SpeakMaster
 */
@Mapper
public interface MessageMapper extends BaseMapper<Message> {
}
