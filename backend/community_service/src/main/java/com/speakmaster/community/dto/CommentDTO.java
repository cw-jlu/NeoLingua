package com.speakmaster.community.dto;

import lombok.Data;

import java.util.List;

/**
 * 评论DTO
 * 
 * @author SpeakMaster
 */
@Data
public class CommentDTO {
    
    /** 评论ID */
    private Long id;
    
    /** 帖子ID */
    private Long postId;
    
    /** 评论者ID */
    private Long userId;
    
    /** 评论者昵�?*/
    private String userNickname;
    
    /** 评论者头�?*/
    private String userAvatar;
    
    /** 父评论ID */
    private Long parentId;
    
    /** 评论内容 */
    private String content;
    
    /** 点赞�?*/
    private Integer likeCount;
    
    /** 创建时间 */
    private String createTime;
    
    /** 回复列表 */
    private List<CommentDTO> replies;
}
