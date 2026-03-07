package com.speakmaster.ai.dto;

import lombok.Data;
import java.util.List;

/**
 * 模型统计数据DTO
 * 
 * @author SpeakMaster
 */
@Data
public class ModelStatistics {
    private Integer totalModels;
    private Integer enabledModels;
    private Integer healthyModels;
    private Long totalCalls;
    private Double overallSuccessRate;
    private List<ModelMetrics> modelMetricsList;
}
