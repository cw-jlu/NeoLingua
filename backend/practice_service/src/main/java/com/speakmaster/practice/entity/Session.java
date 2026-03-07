package com.speakmaster.practice.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.speakmaster.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 练习会话实体类
 * 
 * @author SpeakMaster
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("session")
public class Session extends BaseEntity {

    /** 用户ID */
    @TableField("user_id")
    private Long userId;

    /** 主题ID */
    @TableField("theme_id")
    private Long themeId;

    /** 角色ID */
    @TableField("role_id")
    private Long roleId;

    /** 会话标题 */
    @TableField("title")
    private String title;

    /** 开始时间 */
    @TableField("start_time")
    private String startTime;

    /** 结束时间 */
    @TableField("end_time")
    private String endTime;

    /** 消息数量 */
    @TableField("message_count")
    private Integer messageCount = 0;

    /** 总分数 */
    @TableField("total_score")
    private Double totalScore;

    /** 平均分数 */
    @TableField("avg_score")
    private Double avgScore;

    /** 状态(0-进行中 1-已结束) */
    @TableField("status")
    private Integer status = 0;
}
