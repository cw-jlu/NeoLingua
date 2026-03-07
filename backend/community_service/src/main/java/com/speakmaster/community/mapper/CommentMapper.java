package com.speakmaster.community.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.speakmaster.community.entity.Comment;
import org.apache.ibatis.annotations.Mapper;

/**
 * 评论 Mapper
 * 
 * @author SpeakMaster
 */
@Mapper
public interface CommentMapper extends BaseMapper<Comment> {
}
