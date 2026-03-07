package com.speakmaster.community.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.speakmaster.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 帖子点赞实体
 * 
 * @author SpeakMaster
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("post_like")
public class PostLike extends BaseEntity {

    /** 帖子ID */
    @TableField("post_id")
    private Long postId;

    /** 用户ID */
    @TableField("user_id")
    private Long userId;
}
