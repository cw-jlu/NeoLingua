package com.speakmaster.community.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.speakmaster.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 评论实体
 * 
 * @author SpeakMaster
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("comment")
public class Comment extends BaseEntity {

    /** 帖子ID */
    @TableField("post_id")
    private Long postId;

    /** 评论者ID */
    @TableField("user_id")
    private Long userId;

    /** 父评论ID (回复功能) */
    @TableField("parent_id")
    private Long parentId;

    /** 评论内容 */
    @TableField("content")
    private String content;

    /** 点赞数 */
    @TableField("like_count")
    private Integer likeCount = 0;
}
