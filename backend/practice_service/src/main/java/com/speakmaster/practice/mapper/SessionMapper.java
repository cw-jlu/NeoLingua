package com.speakmaster.practice.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.speakmaster.practice.entity.Session;
import org.apache.ibatis.annotations.Mapper;

/**
 * 会话Mapper
 * 
 * @author SpeakMaster
 */
@Mapper
public interface SessionMapper extends BaseMapper<Session> {
}
