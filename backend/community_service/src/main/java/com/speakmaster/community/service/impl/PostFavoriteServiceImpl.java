package com.speakmaster.community.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.speakmaster.common.enums.ErrorCode;
import com.speakmaster.common.exception.BusinessException;
import com.speakmaster.community.entity.Post;
import com.speakmaster.community.entity.PostFavorite;
import com.speakmaster.community.mapper.PostFavoriteMapper;
import com.speakmaster.community.mapper.PostMapper;
import com.speakmaster.community.service.IPostFavoriteService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 帖子收藏服务实现
 * 
 * @author SpeakMaster
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PostFavoriteServiceImpl implements IPostFavoriteService {

    private final PostFavoriteMapper favoriteMapper;
    private final PostMapper postMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void favoritePost(Long postId, Long userId) {
        // 检查帖子是否存在
        Post post = postMapper.selectById(postId);
        if (post == null || post.getDeleted() == 1) {
            throw new BusinessException(ErrorCode.POST_NOT_FOUND);
        }

        // 检查是否已收藏
        LambdaQueryWrapper<PostFavorite> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(PostFavorite::getPostId, postId)
                .eq(PostFavorite::getUserId, userId)
                .eq(PostFavorite::getDeleted, 0);
        
        if (favoriteMapper.selectCount(wrapper) > 0) {
            throw new BusinessException(ErrorCode.ALREADY_FAVORITED);
        }

        // 创建收藏记录
        PostFavorite favorite = new PostFavorite();
        favorite.setPostId(postId);
        favorite.setUserId(userId);
        favoriteMapper.insert(favorite);

        // 更新帖子收藏数
        post.setFavoriteCount(post.getFavoriteCount() + 1);
        postMapper.updateById(post);

        log.info("收藏帖子成功: postId={}, userId={}", postId, userId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void unfavoritePost(Long postId, Long userId) {
        LambdaQueryWrapper<PostFavorite> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(PostFavorite::getPostId, postId)
                .eq(PostFavorite::getUserId, userId)
                .eq(PostFavorite::getDeleted, 0);
        
        PostFavorite favorite = favoriteMapper.selectOne(wrapper);
        if (favorite == null) {
            throw new BusinessException(ErrorCode.NOT_FAVORITED);
        }

        favorite.markDeleted();
        favoriteMapper.updateById(favorite);

        // 更新帖子收藏数
        Post post = postMapper.selectById(postId);
        if (post != null && post.getFavoriteCount() > 0) {
            post.setFavoriteCount(post.getFavoriteCount() - 1);
            postMapper.updateById(post);
        }

        log.info("取消收藏成功: postId={}, userId={}", postId, userId);
    }
}
