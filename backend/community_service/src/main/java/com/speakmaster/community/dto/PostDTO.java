package com.speakmaster.community.dto;

import lombok.Data;

/**
 * 帖子DTO
 * 
 * @author SpeakMaster
 */
@Data
public class PostDTO {
    
    /** 帖子ID */
    private Long id;
    
    /** 标题 */
    private String title;
    
    /** 内容 */
    private String content;
    
    /** 作者ID */
    private Long authorId;
    
    /** 作者昵�?*/
    private String authorNickname;
    
    /** 作者头�?*/
    private String authorAvatar;
    
    /** 分类 */
    private String category;
    
    /** 标签 */
    private String tags;
    
    /** 封面图片 */
    private String coverImage;
    
    /** 点赞�?*/
    private Integer likeCount;
    
    /** 评论�?*/
    private Integer commentCount;
    
    /** 浏览�?*/
    private Integer viewCount;
    
    /** 收藏�?*/
    private Integer favoriteCount;
    
    /** 是否置顶 */
    private Integer isPinned;
    
    /** 是否隐藏 */
    private Integer isHidden;
    
    /** 状�?*/
    private Integer status;
    
    /** 创建时间 */
    private String createTime;
    
    /** 是否已点�?*/
    private Boolean isLiked;
    
    /** 是否已收�?*/
    private Boolean isFavorited;
}
