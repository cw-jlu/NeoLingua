package com.speakmaster.user.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.speakmaster.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 积分记录实体类
 * 
 * @author SpeakMaster
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("points_record")
public class PointsRecord extends BaseEntity {

    /**
     * 用户ID
     */
    @TableField("user_id")
    private Long userId;

    /**
     * 积分变化 (正数为增加，负数为减少)
     */
    @TableField("points_change")
    private Long pointsChange;

    /**
     * 变化后积分
     */
    @TableField("points_after")
    private Long pointsAfter;

    /**
     * 变化原因
     */
    @TableField("reason")
    private String reason;

    /**
     * 关联业务类型 (1-签到, 2-练习, 3-Meeting, 4-社区, 5-其他)
     */
    @TableField("business_type")
    private Integer businessType;

    /**
     * 关联业务ID
     */
    @TableField("business_id")
    private Long businessId;
}
