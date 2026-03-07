package com.speakmaster.community.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.speakmaster.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 帖子实体
 * 
 * @author SpeakMaster
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("post")
public class Post extends BaseEntity {

    /** 标题 */
    @TableField("title")
    private String title;

    /** 内容 */
    @TableField("content")
    private String content;

    /** 作者ID */
    @TableField("author_id")
    private Long authorId;

    /** 分类 */
    @TableField("category")
    private String category;

    /** 标签 */
    @TableField("tags")
    private String tags;

    /** 封面图片 */
    @TableField("cover_image")
    private String coverImage;

    /** 点赞数 */
    @TableField("like_count")
    private Integer likeCount = 0;

    /** 评论数 */
    @TableField("comment_count")
    private Integer commentCount = 0;

    /** 浏览数 */
    @TableField("view_count")
    private Integer viewCount = 0;

    /** 收藏数 */
    @TableField("favorite_count")
    private Integer favoriteCount = 0;

    /** 是否置顶 (0-否 1-是) */
    @TableField("is_pinned")
    private Integer isPinned = 0;

    /** 是否隐藏 (0-否 1-是) */
    @TableField("is_hidden")
    private Integer isHidden = 0;

    /** 状态(0-草稿 1-已发布) */
    @TableField("status")
    private Integer status = 1;
}
