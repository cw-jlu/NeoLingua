package com.speakmaster.ai.dto;

import lombok.Data;

/**
 * 聊天响应DTO
 * 
 * @author SpeakMaster
 */
@Data
public class ChatResponse {
    /** AI回复内容 */
    private String content;
    /** 使用的模型ID */
    private Long modelId;
    /** 使用的模型名�?*/
    private String modelName;
    /** Token使用�?*/
    private Integer tokenCount;
    /** 响应时间(毫秒) */
    private Long responseTime;
}
