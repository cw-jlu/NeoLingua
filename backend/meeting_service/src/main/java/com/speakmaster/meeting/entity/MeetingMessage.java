package com.speakmaster.meeting.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.speakmaster.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * Meeting消息实体
 * 
 * @author SpeakMaster
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("meeting_message")
public class MeetingMessage extends BaseEntity {

    /** Meeting ID */
    @TableField("meeting_id")
    private Long meetingId;

    /** 发送者ID */
    @TableField("sender_id")
    private Long senderId;

    /** 消息类型 (0-文本 1-音频 2-系统) */
    @TableField("message_type")
    private Integer messageType = 0;

    /** 消息内容 */
    @TableField("content")
    private String content;

    /** 音频URL */
    @TableField("audio_url")
    private String audioUrl;
}
