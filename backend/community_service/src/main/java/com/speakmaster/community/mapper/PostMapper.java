package com.speakmaster.community.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.speakmaster.community.entity.Post;
import org.apache.ibatis.annotations.Mapper;

/**
 * 帖子 Mapper
 * 
 * @author SpeakMaster
 */
@Mapper
public interface PostMapper extends BaseMapper<Post> {
}
