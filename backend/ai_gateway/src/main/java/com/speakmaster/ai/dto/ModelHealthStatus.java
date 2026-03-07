package com.speakmaster.ai.dto;

import lombok.Data;
import java.time.LocalDateTime;

/**
 * 模型健康状态DTO
 * 
 * @author SpeakMaster
 */
@Data
public class ModelHealthStatus {
    private Long modelId;
    private String modelName;
    private boolean healthy;
    private LocalDateTime lastCheckTime;
    private String message;
}
