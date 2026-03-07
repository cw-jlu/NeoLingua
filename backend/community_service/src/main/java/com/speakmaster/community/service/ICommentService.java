package com.speakmaster.community.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.speakmaster.community.dto.CommentDTO;

/**
 * 评论服务接口
 * 
 * @author SpeakMaster
 */
public interface ICommentService {

    /**
     * 发表评论
     */
    CommentDTO createComment(Long postId, CommentDTO commentDTO, Long userId);

    /**
     * 获取帖子的评论列表
     */
    Page<CommentDTO> getPostComments(Long postId, int page, int size);

    /**
     * 更新评论
     */
    CommentDTO updateComment(Long commentId, CommentDTO commentDTO, Long userId);

    /**
     * 删除评论
     */
    void deleteComment(Long commentId, Long userId);

    /**
     * [管理端] 分页查询所有评论
     */
    Page<CommentDTO> adminGetCommentList(Long postId, int page, int size);

    /**
     * [管理端] 获取评论详情
     */
    CommentDTO adminGetCommentById(Long commentId);

    /**
     * [管理端] 删除评论
     */
    void adminDeleteComment(Long commentId);
}
