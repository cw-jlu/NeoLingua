package com.speakmaster.ai.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 模型测试结果DTO
 * 
 * @author SpeakMaster
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ModelTestResult {
    private boolean success;
    private String message;
    private Long responseTime;
}
