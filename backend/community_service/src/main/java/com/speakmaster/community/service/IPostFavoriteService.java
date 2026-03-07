package com.speakmaster.community.service;

/**
 * 帖子收藏服务接口
 * 
 * @author SpeakMaster
 */
public interface IPostFavoriteService {

    /**
     * 收藏帖子
     */
    void favoritePost(Long postId, Long userId);

    /**
     * 取消收藏
     */
    void unfavoritePost(Long postId, Long userId);
}
