package com.speakmaster.community.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.speakmaster.common.context.UserContextHolder;
import com.speakmaster.common.dto.Result;
import com.speakmaster.community.dto.CommentDTO;
import com.speakmaster.community.service.ICommentService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 评论控制器
 * 
 * @author SpeakMaster
 */
@RestController
@RequiredArgsConstructor
public class CommentController {

    private final ICommentService commentService;

    /**
     * 发表评论 (用户�?
     */
    @PostMapping("/user/community/posts/{id}/comments")
    public Result<CommentDTO> createComment(
            @PathVariable Long id,
            @RequestBody CommentDTO commentDTO) {
        Long userId = UserContextHolder.getCurrentUserId();
        CommentDTO comment = commentService.createComment(id, commentDTO, userId);
        return Result.success(comment);
    }

    /**
     * 获取评论列表 (用户�?
     */
    @GetMapping("/user/community/posts/{id}/comments")
    public Result<Page<CommentDTO>> getPostComments(
            @PathVariable Long id,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Page<CommentDTO> comments = commentService.getPostComments(id, page, size);
        return Result.success(comments);
    }

    /**
     * 更新评论 (用户�?
     */
    @PutMapping("/user/community/comments/{id}")
    public Result<CommentDTO> updateComment(
            @PathVariable Long id,
            @RequestBody CommentDTO commentDTO) {
        Long userId = UserContextHolder.getCurrentUserId();
        CommentDTO comment = commentService.updateComment(id, commentDTO, userId);
        return Result.success(comment);
    }

    /**
     * 删除评论 (用户�?
     */
    @DeleteMapping("/user/community/comments/{id}")
    public Result<Void> deleteComment(@PathVariable Long id) {
        Long userId = UserContextHolder.getCurrentUserId();
        commentService.deleteComment(id, userId);
        return Result.success();
    }

    /**
     * 回复评论 (用户�?
     */
    @PostMapping("/user/community/comments/{id}/reply")
    public Result<CommentDTO> replyComment(
            @PathVariable Long id,
            @RequestBody CommentDTO commentDTO) {
        Long userId = UserContextHolder.getCurrentUserId();
        commentDTO.setParentId(id);
        // 需要从父评论获取postId
        CommentDTO comment = commentService.createComment(commentDTO.getPostId(), commentDTO, userId);
        return Result.success(comment);
    }
}
