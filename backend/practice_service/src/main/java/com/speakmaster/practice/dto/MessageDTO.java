package com.speakmaster.practice.dto;

import lombok.Data;

/**
 * 消息DTO
 * 
 * @author SpeakMaster
 */
@Data
public class MessageDTO {
    
    /** 消息ID */
    private Long id;
    
    /** 会话ID */
    private Long sessionId;
    
    /** 发送�?(user/ai) */
    private String sender;
    
    /** 消息内容 */
    private String content;
    
    /** 音频URL */
    private String audioUrl;
    
    /** 创建时间 */
    private String createTime;
}
