package com.speakmaster.community.service;

/**
 * 帖子点赞服务接口
 * 
 * @author SpeakMaster
 */
public interface IPostLikeService {

    /**
     * 点赞帖子
     */
    void likePost(Long postId, Long userId);

    /**
     * 取消点赞
     */
    void unlikePost(Long postId, Long userId);

    /**
     * 获取点赞数
     */
    Long getLikeCount(Long postId);
}
