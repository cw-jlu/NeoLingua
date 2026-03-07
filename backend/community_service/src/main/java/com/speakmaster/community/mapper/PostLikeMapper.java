package com.speakmaster.community.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.speakmaster.community.entity.PostLike;
import org.apache.ibatis.annotations.Mapper;

/**
 * 帖子点赞 Mapper
 * 
 * @author SpeakMaster
 */
@Mapper
public interface PostLikeMapper extends BaseMapper<PostLike> {
}
