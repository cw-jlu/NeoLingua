package com.speakmaster.community.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.speakmaster.common.enums.ErrorCode;
import com.speakmaster.common.exception.BusinessException;
import com.speakmaster.community.dto.CommentDTO;
import com.speakmaster.community.entity.Comment;
import com.speakmaster.community.entity.Post;
import com.speakmaster.community.mapper.CommentMapper;
import com.speakmaster.community.mapper.PostMapper;
import com.speakmaster.community.service.ICommentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 评论服务实现
 * 
 * @author SpeakMaster
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CommentServiceImpl implements ICommentService {

    private final CommentMapper commentMapper;
    private final PostMapper postMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public CommentDTO createComment(Long postId, CommentDTO commentDTO, Long userId) {
        // 检查帖子是否存在
        Post post = postMapper.selectById(postId);
        if (post == null || post.getDeleted() == 1) {
            throw new BusinessException(ErrorCode.POST_NOT_FOUND);
        }

        Comment comment = new Comment();
        comment.setPostId(postId);
        comment.setUserId(userId);
        comment.setParentId(commentDTO.getParentId());
        comment.setContent(commentDTO.getContent());

        commentMapper.insert(comment);

        // 更新帖子评论数
        post.setCommentCount(post.getCommentCount() + 1);
        postMapper.updateById(post);

        log.info("发表评论成功: commentId={}, postId={}, userId={}", comment.getId(), postId, userId);
        return convertToDTO(comment);
    }

    @Override
    public Page<CommentDTO> getPostComments(Long postId, int page, int size) {
        Page<Comment> commentPage = new Page<>(page, size);
        LambdaQueryWrapper<Comment> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Comment::getPostId, postId)
                .isNull(Comment::getParentId)
                .eq(Comment::getDeleted, 0)
                .orderByDesc(Comment::getCreateTime);
        
        Page<Comment> result = commentMapper.selectPage(commentPage, wrapper);
        
        Page<CommentDTO> dtoPage = new Page<>(result.getCurrent(), result.getSize(), result.getTotal());
        List<CommentDTO> dtoList = result.getRecords().stream().map(comment -> {
            CommentDTO dto = convertToDTO(comment);
            // 加载回复
            LambdaQueryWrapper<Comment> replyWrapper = new LambdaQueryWrapper<>();
            replyWrapper.eq(Comment::getParentId, comment.getId())
                    .eq(Comment::getDeleted, 0)
                    .orderByAsc(Comment::getCreateTime);
            List<Comment> replies = commentMapper.selectList(replyWrapper);
            dto.setReplies(replies.stream().map(this::convertToDTO).collect(Collectors.toList()));
            return dto;
        }).collect(Collectors.toList());
        dtoPage.setRecords(dtoList);
        return dtoPage;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public CommentDTO updateComment(Long commentId, CommentDTO commentDTO, Long userId) {
        Comment comment = commentMapper.selectById(commentId);
        if (comment == null || comment.getDeleted() == 1) {
            throw new BusinessException(ErrorCode.COMMENT_NOT_FOUND);
        }

        // 检查权限
        if (!comment.getUserId().equals(userId)) {
            throw new BusinessException(ErrorCode.NO_PERMISSION);
        }

        comment.setContent(commentDTO.getContent());
        commentMapper.updateById(comment);

        log.info("更新评论成功: commentId={}", commentId);
        return convertToDTO(comment);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteComment(Long commentId, Long userId) {
        Comment comment = commentMapper.selectById(commentId);
        if (comment == null || comment.getDeleted() == 1) {
            throw new BusinessException(ErrorCode.COMMENT_NOT_FOUND);
        }

        // 检查权限
        if (!comment.getUserId().equals(userId)) {
            throw new BusinessException(ErrorCode.NO_PERMISSION);
        }

        comment.markDeleted();
        commentMapper.updateById(comment);

        // 更新帖子评论数
        Post post = postMapper.selectById(comment.getPostId());
        if (post != null && post.getCommentCount() > 0) {
            post.setCommentCount(post.getCommentCount() - 1);
            postMapper.updateById(post);
        }

        log.info("删除评论成功: commentId={}", commentId);
    }

    @Override
    public Page<CommentDTO> adminGetCommentList(Long postId, int page, int size) {
        Page<Comment> commentPage = new Page<>(page, size);
        LambdaQueryWrapper<Comment> wrapper = new LambdaQueryWrapper<>();
        
        if (postId != null) {
            wrapper.eq(Comment::getPostId, postId)
                    .isNull(Comment::getParentId);
        }
        wrapper.eq(Comment::getDeleted, 0)
                .orderByDesc(Comment::getCreateTime);
        
        Page<Comment> result = commentMapper.selectPage(commentPage, wrapper);
        Page<CommentDTO> dtoPage = new Page<>(result.getCurrent(), result.getSize(), result.getTotal());
        List<CommentDTO> dtoList = result.getRecords().stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
        dtoPage.setRecords(dtoList);
        return dtoPage;
    }

    @Override
    public CommentDTO adminGetCommentById(Long commentId) {
        Comment comment = commentMapper.selectById(commentId);
        if (comment == null || comment.getDeleted() == 1) {
            throw new BusinessException(ErrorCode.COMMENT_NOT_FOUND);
        }
        return convertToDTO(comment);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void adminDeleteComment(Long commentId) {
        Comment comment = commentMapper.selectById(commentId);
        if (comment == null || comment.getDeleted() == 1) {
            throw new BusinessException(ErrorCode.COMMENT_NOT_FOUND);
        }
        comment.markDeleted();
        commentMapper.updateById(comment);

        // 更新帖子评论数
        Post post = postMapper.selectById(comment.getPostId());
        if (post != null && post.getCommentCount() > 0) {
            post.setCommentCount(post.getCommentCount() - 1);
            postMapper.updateById(post);
        }
        log.info("[管理端] 删除评论: commentId={}", commentId);
    }

    /**
     * 转换为DTO
     */
    private CommentDTO convertToDTO(Comment comment) {
        CommentDTO dto = new CommentDTO();
        dto.setId(comment.getId());
        dto.setPostId(comment.getPostId());
        dto.setUserId(comment.getUserId());
        dto.setParentId(comment.getParentId());
        dto.setContent(comment.getContent());
        dto.setLikeCount(comment.getLikeCount());
        dto.setCreateTime(comment.getCreateTime() != null ? comment.getCreateTime().toString() : null);
        return dto;
    }
}
