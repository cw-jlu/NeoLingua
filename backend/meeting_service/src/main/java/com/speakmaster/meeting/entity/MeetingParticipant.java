package com.speakmaster.meeting.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.speakmaster.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * Meeting参与者实体
 * 
 * @author SpeakMaster
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("meeting_participant")
public class MeetingParticipant extends BaseEntity {

    /** Meeting ID */
    @TableField("meeting_id")
    private Long meetingId;

    /** 用户ID */
    @TableField("user_id")
    private Long userId;

    /** 角色 (0-参与者 1-主持人) */
    @TableField("role")
    private Integer role = 0;

    /** 加入时间 */
    @TableField("join_time")
    private String joinTime;

    /** 离开时间 */
    @TableField("leave_time")
    private String leaveTime;

    /** 状态(0-在线 1-离线) */
    @TableField("status")
    private Integer status = 0;

    /** AI角色名称 */
    @TableField("ai_role_name")
    private String aiRoleName;

    /** AI角色设定 */
    @TableField("ai_role_setting")
    private String aiRoleSetting;
}
