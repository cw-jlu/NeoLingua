package com.speakmaster.ai.dto;

import lombok.Data;

/**
 * 模型配置DTO（管理端�?
 * 
 * @author SpeakMaster
 */
@Data
public class ModelConfigDTO {
    private Long id;
    private String name;
    private String providerType;
    private String modelId;
    private String endpoint;
    private String apiKey;
    private Boolean enabled;
    private Boolean recommended;
    private Integer weight;
    private Integer priority;
    private Integer maxTokens;
    private Double temperature;
    private Integer timeout;
    private String description;
    private Boolean healthy;
}
