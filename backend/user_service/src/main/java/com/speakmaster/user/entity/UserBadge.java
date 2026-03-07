package com.speakmaster.user.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.speakmaster.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 用户徽章关联实体类
 * 
 * @author SpeakMaster
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("user_badge")
public class UserBadge extends BaseEntity {

    /**
     * 用户ID
     */
    @TableField("user_id")
    private Long userId;

    /**
     * 徽章ID
     */
    @TableField("badge_id")
    private Long badgeId;

    /**
     * 获得时间
     */
    @TableField("obtained_time")
    private String obtainedTime;
}
