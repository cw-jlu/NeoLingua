package com.speakmaster.ai.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.speakmaster.ai.dto.ModelMetrics;
import com.speakmaster.ai.dto.ModelStatistics;
import com.speakmaster.ai.entity.ModelConfig;
import com.speakmaster.ai.mapper.ModelConfigMapper;
import com.speakmaster.ai.service.IMetricsService;
import com.speakmaster.common.utils.RedisUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * 指标统计服务实现类
 * 使用Redis存储和查询模型调用指标
 *
 * @author SpeakMaster
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MetricsServiceImpl implements IMetricsService {

    private final RedisUtil redisUtil;
    private final ModelConfigMapper modelConfigMapper;

    private static final String METRICS_KEY_PREFIX = "ai:metrics:";

    @Override
    public void recordCall(Long modelId, boolean success, long responseTime, int tokenCount) {
        try {
            String key = METRICS_KEY_PREFIX + modelId;
            redisUtil.incr(key + ":totalCalls", 1);
            if (success) {
                redisUtil.incr(key + ":successCalls", 1);
            } else {
                redisUtil.incr(key + ":failedCalls", 1);
            }
            redisUtil.incr(key + ":totalResponseTime", responseTime);
            redisUtil.incr(key + ":totalTokens", tokenCount);
        } catch (Exception e) {
            log.warn("记录调用指标失败: modelId={}", modelId, e);
        }
    }

    @Override
    public ModelMetrics getMetrics(Long modelId) {
        String key = METRICS_KEY_PREFIX + modelId;
        ModelConfig model = modelConfigMapper.selectById(modelId);

        long totalCalls = getLongValue(key + ":totalCalls");
        long successCalls = getLongValue(key + ":successCalls");
        long failedCalls = getLongValue(key + ":failedCalls");
        long totalResponseTime = getLongValue(key + ":totalResponseTime");
        long totalTokens = getLongValue(key + ":totalTokens");

        ModelMetrics metrics = new ModelMetrics();
        metrics.setModelId(modelId);
        metrics.setModelName(model != null ? model.getName() : "未知模型");
        metrics.setTotalCalls(totalCalls);
        metrics.setSuccessCalls(successCalls);
        metrics.setFailedCalls(failedCalls);
        metrics.setAvgResponseTime(totalCalls > 0 ? (double) totalResponseTime / totalCalls : 0.0);
        metrics.setSuccessRate(totalCalls > 0 ? (double) successCalls / totalCalls : 0.0);
        metrics.setTotalTokens(totalTokens);
        return metrics;
    }

    @Override
    public ModelStatistics getStatistics() {
        LambdaQueryWrapper<ModelConfig> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ModelConfig::getDeleted, 0)
                .orderByAsc(ModelConfig::getPriority);
        List<ModelConfig> allModels = modelConfigMapper.selectList(wrapper);

        long totalCalls = 0;
        long totalSuccess = 0;
        List<ModelMetrics> metricsList = new ArrayList<>();

        for (ModelConfig model : allModels) {
            ModelMetrics metrics = getMetrics(model.getId());
            metricsList.add(metrics);
            totalCalls += metrics.getTotalCalls();
            totalSuccess += metrics.getSuccessCalls();
        }

        ModelStatistics stats = new ModelStatistics();
        stats.setTotalModels(allModels.size());
        stats.setEnabledModels((int) allModels.stream().filter(ModelConfig::getEnabled).count());
        stats.setHealthyModels((int) allModels.stream().filter(ModelConfig::getHealthy).count());
        stats.setTotalCalls(totalCalls);
        stats.setOverallSuccessRate(totalCalls > 0 ? (double) totalSuccess / totalCalls : 0.0);
        stats.setModelMetricsList(metricsList);
        return stats;
    }

    /**
     * 从Redis获取Long值
     */
    private long getLongValue(String key) {
        Object value = redisUtil.get(key);
        if (value == null) return 0;
        if (value instanceof Number) return ((Number) value).longValue();
        try {
            return Long.parseLong(value.toString());
        } catch (NumberFormatException e) {
            return 0;
        }
    }
}
