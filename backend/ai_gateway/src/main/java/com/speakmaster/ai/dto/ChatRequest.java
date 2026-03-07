package com.speakmaster.ai.dto;

import lombok.Data;
import java.util.List;

/**
 * 聊天请求DTO
 * 
 * @author SpeakMaster
 */
@Data
public class ChatRequest {
    /** 会话标识 */
    private String sessionId;
    /** 指定模型ID（可选） */
    private Long modelId;
    /** 消息列表 */
    private List<Message> messages;
    /** 温度（可选） */
    private Double temperature;
    /** 最大Token数（可选） */
    private Integer maxTokens;
    /** 音频URL（多模态模型使用，可选） */
    private String audioUrl;
}
