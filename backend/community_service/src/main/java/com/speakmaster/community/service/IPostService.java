package com.speakmaster.community.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.speakmaster.community.dto.PostDTO;

import java.util.Map;

/**
 * 帖子服务接口
 * 
 * @author SpeakMaster
 */
public interface IPostService {

    /**
     * 发布帖子
     */
    PostDTO createPost(PostDTO postDTO, Long userId);

    /**
     * 获取帖子列表
     */
    Page<PostDTO> getPostList(int page, int size, Long userId);

    /**
     * 获取帖子详情
     */
    PostDTO getPostById(Long postId, Long userId);

    /**
     * 获取用户的帖子列表
     */
    Page<PostDTO> getUserPosts(Long userId, int page, int size);

    /**
     * 获取热门帖子
     */
    Page<PostDTO> getHotPosts(int page, int size, Long userId);

    /**
     * 更新帖子
     */
    PostDTO updatePost(Long postId, PostDTO postDTO, Long userId);

    /**
     * 删除帖子
     */
    void deletePost(Long postId, Long userId);

    /**
     * [管理端] 分页查询所有帖子
     */
    Page<PostDTO> adminGetPostList(Integer status, int page, int size);

    /**
     * [管理端] 获取帖子详情
     */
    PostDTO adminGetPostById(Long postId);

    /**
     * [管理端] 删除帖子
     */
    void adminDeletePost(Long postId);

    /**
     * [管理端] 置顶帖子
     */
    PostDTO adminPinPost(Long postId);

    /**
     * [管理端] 取消置顶
     */
    PostDTO adminUnpinPost(Long postId);

    /**
     * [管理端] 隐藏帖子
     */
    PostDTO adminHidePost(Long postId);

    /**
     * [管理端] 显示帖子
     */
    PostDTO adminShowPost(Long postId);

    /**
     * [管理端] 社区统计
     */
    Map<String, Object> adminGetPostStatistics();
}
