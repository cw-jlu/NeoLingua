package com.speakmaster.practice.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.speakmaster.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 消息实体类
 * 
 * @author SpeakMaster
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("message")
public class Message extends BaseEntity {

    /** 会话ID */
    @TableField("session_id")
    private Long sessionId;

    /** 发送者类型(1-用户, 2-AI) */
    @TableField("sender_type")
    private Integer senderType;

    /** 消息内容 */
    @TableField("content")
    private String content;

    /** 音频URL */
    @TableField("audio_url")
    private String audioUrl;

    /** 评分 (1-5) */
    @TableField("score")
    private Integer score;

    /** 反馈建议 */
    @TableField("feedback")
    private String feedback;

    /** 示例音频URL */
    @TableField("example_audio_url")
    private String exampleAudioUrl;
}
