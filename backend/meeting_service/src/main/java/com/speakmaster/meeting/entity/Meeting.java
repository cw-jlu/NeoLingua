package com.speakmaster.meeting.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.speakmaster.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * Meeting实体
 * 
 * @author SpeakMaster
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("meeting")
public class Meeting extends BaseEntity {

    /** 房间名称 */
    @TableField("name")
    private String name;

    /** 房间描述 */
    @TableField("description")
    private String description;

    /** 创建者ID */
    @TableField("creator_id")
    private Long creatorId;

    /** 主题ID */
    @TableField("theme_id")
    private Long themeId;

    /** 最大人数 */
    @TableField("max_participants")
    private Integer maxParticipants = 4;

    /** 当前人数 */
    @TableField("current_participants")
    private Integer currentParticipants = 0;

    /** 状态(0-等待中 1-进行中 2-已结束) */
    @TableField("status")
    private Integer status = 0;

    /** 开始时间 */
    @TableField("start_time")
    private String startTime;

    /** 结束时间 */
    @TableField("end_time")
    private String endTime;

    /** 是否公开 (0-私密 1-公开) */
    @TableField("is_public")
    private Integer isPublic = 0;
}
