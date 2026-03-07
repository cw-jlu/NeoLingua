package com.speakmaster.community.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.speakmaster.common.enums.ErrorCode;
import com.speakmaster.common.exception.BusinessException;
import com.speakmaster.common.utils.RedisUtil;
import com.speakmaster.community.entity.Post;
import com.speakmaster.community.entity.PostLike;
import com.speakmaster.community.mapper.PostLikeMapper;
import com.speakmaster.community.mapper.PostMapper;
import com.speakmaster.community.service.IPostLikeService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 帖子点赞服务实现
 * 
 * @author SpeakMaster
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PostLikeServiceImpl implements IPostLikeService {

    private final PostLikeMapper likeMapper;
    private final PostMapper postMapper;
    private final RedisUtil redisUtil;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void likePost(Long postId, Long userId) {
        // 检查帖子是否存在
        Post post = postMapper.selectById(postId);
        if (post == null || post.getDeleted() == 1) {
            throw new BusinessException(ErrorCode.POST_NOT_FOUND);
        }

        // 检查是否已点赞
        LambdaQueryWrapper<PostLike> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(PostLike::getPostId, postId)
                .eq(PostLike::getUserId, userId)
                .eq(PostLike::getDeleted, 0);
        
        if (likeMapper.selectCount(wrapper) > 0) {
            throw new BusinessException(ErrorCode.ALREADY_LIKED);
        }

        // 创建点赞记录
        PostLike like = new PostLike();
        like.setPostId(postId);
        like.setUserId(userId);
        likeMapper.insert(like);

        // 使用Redis HyperLogLog统计点赞数
        String key = "post:like:" + postId;
        redisUtil.pfAdd(key, userId.toString());

        // 更新帖子点赞数
        post.setLikeCount(post.getLikeCount() + 1);
        postMapper.updateById(post);

        log.info("点赞帖子成功: postId={}, userId={}", postId, userId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void unlikePost(Long postId, Long userId) {
        LambdaQueryWrapper<PostLike> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(PostLike::getPostId, postId)
                .eq(PostLike::getUserId, userId)
                .eq(PostLike::getDeleted, 0);
        
        PostLike like = likeMapper.selectOne(wrapper);
        if (like == null) {
            throw new BusinessException(ErrorCode.NOT_LIKED);
        }

        like.markDeleted();
        likeMapper.updateById(like);

        // 更新帖子点赞数
        Post post = postMapper.selectById(postId);
        if (post != null && post.getLikeCount() > 0) {
            post.setLikeCount(post.getLikeCount() - 1);
            postMapper.updateById(post);
        }

        log.info("取消点赞成功: postId={}, userId={}", postId, userId);
    }

    @Override
    public Long getLikeCount(Long postId) {
        // 优先从Redis获取
        String key = "post:like:" + postId;
        Long count = redisUtil.pfCount(key);
        
        if (count == null || count == 0) {
            // Redis中没有，从数据库获取
            LambdaQueryWrapper<PostLike> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(PostLike::getPostId, postId)
                    .eq(PostLike::getDeleted, 0);
            count = likeMapper.selectCount(wrapper);
        }
        
        return count;
    }
}
