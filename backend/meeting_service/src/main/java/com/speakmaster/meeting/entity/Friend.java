package com.speakmaster.meeting.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.speakmaster.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 好友实体
 * 
 * @author SpeakMaster
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("friend")
public class Friend extends BaseEntity {

    /** 用户ID */
    @TableField("user_id")
    private Long userId;

    /** 好友ID */
    @TableField("friend_id")
    private Long friendId;

    /** 状态(0-待确认 1-已接受 2-已拒绝) */
    @TableField("status")
    private Integer status = 0;

    /** 备注名称 */
    @TableField("remark")
    private String remark;
}
