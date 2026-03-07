package com.speakmaster.ai.vo;

import lombok.Data;

/**
 * 模型视图（用户端�?
 * 
 * @author SpeakMaster
 */
@Data
public class ModelVO {
    private Long id;
    private String name;
    private String modelId;
    private String description;
    private Boolean recommended;
}
