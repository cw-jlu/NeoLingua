package com.speakmaster.ai.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.speakmaster.ai.dto.*;
import com.speakmaster.ai.entity.ModelConfig;
import com.speakmaster.ai.mapper.ModelConfigMapper;
import com.speakmaster.ai.provider.ModelProvider;
import com.speakmaster.ai.provider.ModelProviderFactory;
import com.speakmaster.ai.service.*;
import com.speakmaster.ai.vo.ModelVO;
import com.speakmaster.common.enums.ErrorCode;
import com.speakmaster.common.exception.BusinessException;
import com.speakmaster.common.utils.RedisUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 模型管理服务实现类
 */
@Slf4j
@Service
public class ModelServiceImpl extends ServiceImpl<ModelConfigMapper, ModelConfig> implements IModelService {

    private final RedisUtil redisUtil;
    private final IHealthCheckService healthCheckService;
    private final IMetricsService metricsService;
    private final ModelProviderFactory modelProviderFactory;

    public ModelServiceImpl(RedisUtil redisUtil,
                            @Lazy IHealthCheckService healthCheckService,
                            @Lazy IMetricsService metricsService,
                            @Lazy ModelProviderFactory modelProviderFactory) {
        this.redisUtil = redisUtil;
        this.healthCheckService = healthCheckService;
        this.metricsService = metricsService;
        this.modelProviderFactory = modelProviderFactory;
    }

    private static final String CACHE_KEY_ALL_MODELS = "ai:models:all";
    private static final String CACHE_KEY_ENABLED_MODELS = "ai:models:enabled";
    private static final String CACHE_KEY_RECOMMENDED_MODELS = "ai:models:recommended";

    @Value("${ai.gateway.cache-ttl:300}")
    private long cacheTtl;

    @Override
    public List<ModelConfigDTO> getAllModels() {
        LambdaQueryWrapper<ModelConfig> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ModelConfig::getDeleted, 0)
                .orderByAsc(ModelConfig::getPriority);
        List<ModelConfig> models = this.list(wrapper);
        return models.stream().map(this::toDTO).collect(Collectors.toList());
    }

    @Override
    public ModelConfigDTO getModelById(Long id) {
        ModelConfig model = findModelOrThrow(id);
        return toDTO(model);
    }

    @Override
    @Transactional
    public ModelConfigDTO createModel(ModelConfigDTO dto) {
        ModelConfig model = new ModelConfig();
        copyDtoToEntity(dto, model);
        this.save(model);
        clearCache();
        log.info("创建模型配置成功: id={}, name={}", model.getId(), model.getName());
        return toDTO(model);
    }

    @Override
    @Transactional
    public ModelConfigDTO updateModel(Long id, ModelConfigDTO dto) {
        ModelConfig model = findModelOrThrow(id);
        copyDtoToEntity(dto, model);
        this.updateById(model);
        clearCache();
        log.info("更新模型配置成功: id={}, name={}", model.getId(), model.getName());
        return toDTO(model);
    }

    @Override
    @Transactional
    public void deleteModel(Long id) {
        ModelConfig model = findModelOrThrow(id);
        model.markDeleted();
        this.updateById(model);
        clearCache();
        log.info("删除模型配置成功: id={}", id);
    }

    @Override
    @Transactional
    public void enableModel(Long id) {
        ModelConfig model = findModelOrThrow(id);
        model.setEnabled(true);
        this.updateById(model);
        clearCache();
        log.info("启用模型成功: id={}", id);
    }

    @Override
    @Transactional
    public void disableModel(Long id) {
        ModelConfig model = findModelOrThrow(id);
        model.setEnabled(false);
        this.updateById(model);
        clearCache();
        log.info("禁用模型成功: id={}", id);
    }

    @Override
    public List<ModelVO> getEnabledModels() {
        LambdaQueryWrapper<ModelConfig> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ModelConfig::getEnabled, true)
                .eq(ModelConfig::getDeleted, 0);
        List<ModelConfig> models = this.list(wrapper);
        return models.stream().map(this::toVO).collect(Collectors.toList());
    }

    @Override
    public ModelVO getModelVOById(Long id) {
        LambdaQueryWrapper<ModelConfig> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ModelConfig::getId, id)
                .eq(ModelConfig::getDeleted, 0)
                .eq(ModelConfig::getEnabled, true);
        ModelConfig model = this.getOne(wrapper);
        if (model == null) {
            throw new BusinessException(ErrorCode.AI_MODEL_NOT_FOUND);
        }
        return toVO(model);
    }

    @Override
    public List<ModelVO> getRecommendedModels() {
        LambdaQueryWrapper<ModelConfig> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ModelConfig::getEnabled, true)
                .eq(ModelConfig::getRecommended, true)
                .eq(ModelConfig::getDeleted, 0);
        List<ModelConfig> models = this.list(wrapper);
        return models.stream().map(this::toVO).collect(Collectors.toList());
    }

    @Override
    public ModelTestResult testModel(Long id) {
        ModelConfig model = findModelOrThrow(id);
        ModelTestResult result = new ModelTestResult();
        long startTime = System.currentTimeMillis();

        try {
            ModelProvider provider = modelProviderFactory.getProvider(model);
            boolean healthy = provider.healthCheck(model);
            long responseTime = System.currentTimeMillis() - startTime;
            result.setSuccess(healthy);
            result.setMessage(healthy ? "模型连通性测试通过" : "模型连通性测试失败");
            result.setResponseTime(responseTime);
        } catch (Exception e) {
            long responseTime = System.currentTimeMillis() - startTime;
            result.setSuccess(false);
            result.setMessage("测试异常: " + e.getMessage());
            result.setResponseTime(responseTime);
        }
        return result;
    }

    @Override
    public ModelHealthStatus getModelHealth(Long id) {
        return healthCheckService.checkModelHealth(id);
    }

    @Override
    public ModelMetrics getModelMetrics(Long id) {
        findModelOrThrow(id);
        return metricsService.getMetrics(id);
    }

    @Override
    public ModelStatistics getModelStatistics() {
        return metricsService.getStatistics();
    }

    @Override
    public ModelConfig findModelOrThrow(Long id) {
        LambdaQueryWrapper<ModelConfig> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ModelConfig::getId, id)
                .eq(ModelConfig::getDeleted, 0);
        ModelConfig model = this.getOne(wrapper);
        if (model == null) {
            throw new BusinessException(ErrorCode.AI_MODEL_NOT_FOUND);
        }
        return model;
    }

    private void clearCache() {
        try {
            redisUtil.delete(CACHE_KEY_ALL_MODELS);
            redisUtil.delete(CACHE_KEY_ENABLED_MODELS);
            redisUtil.delete(CACHE_KEY_RECOMMENDED_MODELS);
            log.debug("清除模型缓存成功");
        } catch (Exception e) {
            log.warn("清除模型缓存失败", e);
        }
    }

    private void copyDtoToEntity(ModelConfigDTO dto, ModelConfig entity) {
        if (dto.getName() != null) entity.setName(dto.getName());
        if (dto.getProviderType() != null) entity.setProviderType(dto.getProviderType());
        if (dto.getModelId() != null) entity.setModelId(dto.getModelId());
        if (dto.getEndpoint() != null) entity.setEndpoint(dto.getEndpoint());
        if (dto.getApiKey() != null) entity.setApiKey(dto.getApiKey());
        if (dto.getEnabled() != null) entity.setEnabled(dto.getEnabled());
        if (dto.getRecommended() != null) entity.setRecommended(dto.getRecommended());
        if (dto.getWeight() != null) entity.setWeight(dto.getWeight());
        if (dto.getPriority() != null) entity.setPriority(dto.getPriority());
        if (dto.getMaxTokens() != null) entity.setMaxTokens(dto.getMaxTokens());
        if (dto.getTemperature() != null) entity.setTemperature(dto.getTemperature());
        if (dto.getTimeout() != null) entity.setTimeout(dto.getTimeout());
        if (dto.getDescription() != null) entity.setDescription(dto.getDescription());
    }

    private ModelConfigDTO toDTO(ModelConfig entity) {
        ModelConfigDTO dto = new ModelConfigDTO();
        dto.setId(entity.getId());
        dto.setName(entity.getName());
        dto.setProviderType(entity.getProviderType());
        dto.setModelId(entity.getModelId());
        dto.setEndpoint(entity.getEndpoint());
        dto.setApiKey(entity.getApiKey());
        dto.setEnabled(entity.getEnabled());
        dto.setRecommended(entity.getRecommended());
        dto.setWeight(entity.getWeight());
        dto.setPriority(entity.getPriority());
        dto.setMaxTokens(entity.getMaxTokens());
        dto.setTemperature(entity.getTemperature());
        dto.setTimeout(entity.getTimeout());
        dto.setDescription(entity.getDescription());
        dto.setHealthy(entity.getHealthy());
        return dto;
    }

    private ModelVO toVO(ModelConfig entity) {
        ModelVO vo = new ModelVO();
        vo.setId(entity.getId());
        vo.setName(entity.getName());
        vo.setModelId(entity.getModelId());
        vo.setDescription(entity.getDescription());
        vo.setRecommended(entity.getRecommended());
        return vo;
    }
}
