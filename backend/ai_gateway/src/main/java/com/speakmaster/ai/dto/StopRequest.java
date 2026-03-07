package com.speakmaster.ai.dto;

import lombok.Data;

/**
 * 停止生成请求DTO
 * 
 * @author SpeakMaster
 */
@Data
public class StopRequest {
    /** 会话标识 */
    private String sessionId;
}
