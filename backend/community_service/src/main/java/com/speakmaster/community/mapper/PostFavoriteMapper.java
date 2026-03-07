package com.speakmaster.community.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.speakmaster.community.entity.PostFavorite;
import org.apache.ibatis.annotations.Mapper;

/**
 * 帖子收藏 Mapper
 * 
 * @author SpeakMaster
 */
@Mapper
public interface PostFavoriteMapper extends BaseMapper<PostFavorite> {
}
