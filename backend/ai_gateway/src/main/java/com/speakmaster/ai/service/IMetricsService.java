package com.speakmaster.ai.service;

import com.speakmaster.ai.dto.ModelMetrics;
import com.speakmaster.ai.dto.ModelStatistics;

/**
 * 指标统计服务接口
 */
public interface IMetricsService {

    /**
     * 记录一次模型调用
     */
    void recordCall(Long modelId, boolean success, long responseTime, int tokenCount);

    /**
     * 获取单个模型的使用指标
     */
    ModelMetrics getMetrics(Long modelId);

    /**
     * 获取所有模型的汇总统计
     */
    ModelStatistics getStatistics();
}
