package com.speakmaster.meeting.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.speakmaster.meeting.entity.UserProfile;
import org.apache.ibatis.annotations.Mapper;

/**
 * 用户画像Mapper
 * 
 * @author SpeakMaster
 */
@Mapper
public interface UserProfileMapper extends BaseMapper<UserProfile> {
}
