package com.speakmaster.community.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.speakmaster.common.dto.Result;
import com.speakmaster.community.dto.CommentDTO;
import com.speakmaster.community.dto.PostDTO;
import com.speakmaster.community.service.ICommentService;
import com.speakmaster.community.service.IPostService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 管理端 - 社区内容管理控制器
 * 
 * @author SpeakMaster
 */
@Slf4j
@RestController
@RequestMapping("/admin/community")
@RequiredArgsConstructor
public class AdminPostController {

    private final IPostService postService;
    private final ICommentService commentService;

    // ==================== 帖子管理 ====================

    /**
     * 获取帖子列表（分页）
     */
    @GetMapping("/posts")
    public Result<Page<PostDTO>> getPostList(
            @RequestParam(required = false) Integer status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        Page<PostDTO> posts = postService.adminGetPostList(status, page, size);
        return Result.success(posts);
    }

    /**
     * 获取帖子详情
     */
    @GetMapping("/posts/{id}")
    public Result<PostDTO> getPostById(@PathVariable Long id) {
        PostDTO post = postService.adminGetPostById(id);
        return Result.success(post);
    }

    /**
     * 删除帖子
     */
    @DeleteMapping("/posts/{id}")
    public Result<Void> deletePost(@PathVariable Long id) {
        postService.adminDeletePost(id);
        return Result.success();
    }

    /**
     * 置顶帖子
     */
    @PutMapping("/posts/{id}/pin")
    public Result<PostDTO> pinPost(@PathVariable Long id) {
        PostDTO post = postService.adminPinPost(id);
        return Result.success(post);
    }

    /**
     * 取消置顶
     */
    @PutMapping("/posts/{id}/unpin")
    public Result<PostDTO> unpinPost(@PathVariable Long id) {
        PostDTO post = postService.adminUnpinPost(id);
        return Result.success(post);
    }

    /**
     * 隐藏帖子
     */
    @PutMapping("/posts/{id}/hide")
    public Result<PostDTO> hidePost(@PathVariable Long id) {
        PostDTO post = postService.adminHidePost(id);
        return Result.success(post);
    }

    /**
     * 显示帖子
     */
    @PutMapping("/posts/{id}/show")
    public Result<PostDTO> showPost(@PathVariable Long id) {
        PostDTO post = postService.adminShowPost(id);
        return Result.success(post);
    }

    /**
     * 社区统计
     */
    @GetMapping("/statistics")
    public Result<Map<String, Object>> getStatistics() {
        Map<String, Object> stats = postService.adminGetPostStatistics();
        return Result.success(stats);
    }

    // ==================== 评论管理 ====================

    /**
     * 获取评论列表（分页）
     */
    @GetMapping("/comments")
    public Result<Page<CommentDTO>> getCommentList(
            @RequestParam(required = false) Long postId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        Page<CommentDTO> comments = commentService.adminGetCommentList(postId, page, size);
        return Result.success(comments);
    }

    /**
     * 获取评论详情
     */
    @GetMapping("/comments/{id}")
    public Result<CommentDTO> getCommentById(@PathVariable Long id) {
        CommentDTO comment = commentService.adminGetCommentById(id);
        return Result.success(comment);
    }

    /**
     * 删除评论
     */
    @DeleteMapping("/comments/{id}")
    public Result<Void> deleteComment(@PathVariable Long id) {
        commentService.adminDeleteComment(id);
        return Result.success();
    }
}
