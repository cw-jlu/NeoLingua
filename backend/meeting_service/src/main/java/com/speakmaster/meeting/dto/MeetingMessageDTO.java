package com.speakmaster.meeting.dto;

import lombok.Data;

/**
 * Meeting消息DTO
 * 
 * @author SpeakMaster
 */
@Data
public class MeetingMessageDTO {
    
    /** 消息ID */
    private Long id;
    
    /** Meeting ID */
    private Long meetingId;
    
    /** 发送者ID */
    private Long senderId;
    
    /** 消息类型 (0-文本 1-音频 2-系统) */
    private Integer messageType;
    
    /** 消息内容 */
    private String content;
    
    /** 音频URL */
    private String audioUrl;
    
    /** 创建时间 */
    private String createTime;
    
    /** 发送者昵称 */
    private String senderNickname;

    /** 发送者名称（AI角色名等） */
    private String senderName;
}
