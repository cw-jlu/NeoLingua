package com.speakmaster.ai.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.speakmaster.ai.dto.ModelHealthStatus;
import com.speakmaster.ai.entity.ModelConfig;
import com.speakmaster.ai.mapper.ModelConfigMapper;
import com.speakmaster.ai.provider.ModelProvider;
import com.speakmaster.ai.provider.ModelProviderFactory;
import com.speakmaster.ai.service.IHealthCheckService;
import com.speakmaster.common.enums.ErrorCode;
import com.speakmaster.common.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 健康检查服务实现类
 * 定期检测所有启用模型的可用性，更新健康状态
 *
 * @author SpeakMaster
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class HealthCheckServiceImpl implements IHealthCheckService {

    private final ModelConfigMapper modelConfigMapper;
    private final ModelProviderFactory modelProviderFactory;

    @Override
    public ModelHealthStatus checkModelHealth(Long modelId) {
        LambdaQueryWrapper<ModelConfig> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ModelConfig::getId, modelId)
                .eq(ModelConfig::getDeleted, 0);
        ModelConfig model = modelConfigMapper.selectOne(wrapper);
        
        if (model == null) {
            throw new BusinessException(ErrorCode.AI_MODEL_NOT_FOUND);
        }

        boolean healthy = false;
        String message;
        try {
            ModelProvider provider = modelProviderFactory.getProvider(model);
            healthy = provider.healthCheck(model);
            message = healthy ? "模型运行正常" : "模型健康检查失败";
        } catch (Exception e) {
            message = "健康检查异常: " + e.getMessage();
        }

        // 更新健康状态
        if (healthy != model.getHealthy()) {
            model.setHealthy(healthy);
            modelConfigMapper.updateById(model);
            log.info("模型健康状态变更: id={}, name={}, healthy={}", model.getId(), model.getName(), healthy);
        }

        ModelHealthStatus status = new ModelHealthStatus();
        status.setModelId(model.getId());
        status.setModelName(model.getName());
        status.setHealthy(healthy);
        status.setLastCheckTime(LocalDateTime.now());
        status.setMessage(message);
        return status;
    }

    @Override
    @Scheduled(fixedDelayString = "${ai.gateway.health-check-interval:60}000")
    public void scheduledHealthCheck() {
        LambdaQueryWrapper<ModelConfig> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ModelConfig::getEnabled, true)
                .eq(ModelConfig::getDeleted, 0);
        List<ModelConfig> enabledModels = modelConfigMapper.selectList(wrapper);
        
        if (enabledModels.isEmpty()) return;

        log.debug("开始定时健康检查，共{}个模型", enabledModels.size());
        for (ModelConfig model : enabledModels) {
            try {
                checkModelHealth(model.getId());
            } catch (Exception e) {
                log.warn("定时健康检查失败: model={}, error={}", model.getName(), e.getMessage());
            }
        }
    }
}
