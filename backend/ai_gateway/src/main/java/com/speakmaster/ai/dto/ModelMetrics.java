package com.speakmaster.ai.dto;

import lombok.Data;

/**
 * 模型使用指标DTO
 * 
 * @author SpeakMaster
 */
@Data
public class ModelMetrics {
    private Long modelId;
    private String modelName;
    private Long totalCalls;
    private Long successCalls;
    private Long failedCalls;
    private Double avgResponseTime;
    private Double successRate;
    private Long totalTokens;
}
