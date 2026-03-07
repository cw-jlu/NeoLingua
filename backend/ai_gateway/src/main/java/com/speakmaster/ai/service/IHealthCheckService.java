package com.speakmaster.ai.service;

import com.speakmaster.ai.dto.ModelHealthStatus;

/**
 * 健康检查服务接口
 */
public interface IHealthCheckService {

    /**
     * 检查单个模型健康状态
     */
    ModelHealthStatus checkModelHealth(Long modelId);

    /**
     * 定时检查所有启用模型的健康状态
     */
    void scheduledHealthCheck();
}
