package com.speakmaster.ai.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.speakmaster.ai.dto.ChatRequest;
import com.speakmaster.ai.dto.RoutingRuleDTO;
import com.speakmaster.ai.entity.ModelConfig;
import com.speakmaster.ai.entity.RoutingRule;
import com.speakmaster.ai.mapper.ModelConfigMapper;
import com.speakmaster.ai.mapper.RoutingRuleMapper;
import com.speakmaster.ai.service.IRoutingService;
import com.speakmaster.common.enums.ErrorCode;
import com.speakmaster.common.exception.BusinessException;
import com.speakmaster.common.utils.RedisUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

/**
 * 路由服务实现类
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RoutingServiceImpl extends ServiceImpl<RoutingRuleMapper, RoutingRule> implements IRoutingService {

    private final ModelConfigMapper modelConfigMapper;
    private final RedisUtil redisUtil;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final AtomicInteger roundRobinCounter = new AtomicInteger(0);

    private static final String CACHE_KEY_ROUTING_RULES = "ai:routing:rules";

    @Override
    public ModelConfig selectModel(ChatRequest request) {
        // 如果指定了模型ID，直接使用
        if (request.getModelId() != null) {
            LambdaQueryWrapper<ModelConfig> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(ModelConfig::getId, request.getModelId())
                    .eq(ModelConfig::getDeleted, 0)
                    .eq(ModelConfig::getEnabled, true)
                    .eq(ModelConfig::getHealthy, true);
            ModelConfig model = modelConfigMapper.selectOne(wrapper);
            if (model == null) {
                throw new BusinessException(ErrorCode.AI_MODEL_UNAVAILABLE);
            }
            return model;
        }

        // 获取启用的路由规则（按优先级排序）
        LambdaQueryWrapper<RoutingRule> ruleWrapper = new LambdaQueryWrapper<>();
        ruleWrapper.eq(RoutingRule::getEnabled, true)
                .eq(RoutingRule::getDeleted, 0)
                .orderByAsc(RoutingRule::getPriority);
        List<RoutingRule> rules = this.list(ruleWrapper);

        // 如果没有路由规则，使用默认策略
        if (rules.isEmpty()) {
            return selectFromHealthyModels(null);
        }

        // 按优先级尝试每条规则
        for (RoutingRule rule : rules) {
            try {
                List<Long> modelIds = parseModelIds(rule.getModelIds());
                List<ModelConfig> healthyModels = getHealthyModels(modelIds);
                if (healthyModels.isEmpty()) continue;

                ModelConfig selected = applyStrategy(rule.getStrategy(), healthyModels);
                if (selected != null) {
                    log.debug("路由选择: 规则={}, 策略={}, 模型={}", rule.getName(), rule.getStrategy(), selected.getName());
                    return selected;
                }
            } catch (Exception e) {
                log.warn("路由规则执行失败: rule={}, error={}", rule.getName(), e.getMessage());
            }
        }

        return selectFromHealthyModels(null);
    }

    @Override
    public List<RoutingRuleDTO> getAllRules() {
        LambdaQueryWrapper<RoutingRule> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(RoutingRule::getDeleted, 0)
                .orderByAsc(RoutingRule::getPriority);
        List<RoutingRule> rules = this.list(wrapper);
        return rules.stream().map(this::toDTO).collect(Collectors.toList());
    }

    @Override
    @Transactional
    public RoutingRuleDTO createRule(RoutingRuleDTO dto) {
        RoutingRule rule = new RoutingRule();
        copyDtoToEntity(dto, rule);
        this.save(rule);
        clearCache();
        log.info("创建路由规则成功: id={}, name={}", rule.getId(), rule.getName());
        return toDTO(rule);
    }

    @Override
    @Transactional
    public RoutingRuleDTO updateRule(Long id, RoutingRuleDTO dto) {
        LambdaQueryWrapper<RoutingRule> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(RoutingRule::getId, id)
                .eq(RoutingRule::getDeleted, 0);
        RoutingRule rule = this.getOne(wrapper);
        if (rule == null) {
            throw new BusinessException(ErrorCode.AI_MODEL_NOT_FOUND);
        }
        copyDtoToEntity(dto, rule);
        this.updateById(rule);
        clearCache();
        log.info("更新路由规则成功: id={}, name={}", rule.getId(), rule.getName());
        return toDTO(rule);
    }

    @Override
    @Transactional
    public void deleteRule(Long id) {
        LambdaQueryWrapper<RoutingRule> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(RoutingRule::getId, id)
                .eq(RoutingRule::getDeleted, 0);
        RoutingRule rule = this.getOne(wrapper);
        if (rule == null) {
            throw new BusinessException(ErrorCode.AI_MODEL_NOT_FOUND);
        }
        rule.markDeleted();
        this.updateById(rule);
        clearCache();
        log.info("删除路由规则成功: id={}", id);
    }

    private ModelConfig selectFromHealthyModels(List<Long> modelIds) {
        LambdaQueryWrapper<ModelConfig> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ModelConfig::getEnabled, true)
                .eq(ModelConfig::getHealthy, true)
                .eq(ModelConfig::getDeleted, 0);
        if (modelIds != null && !modelIds.isEmpty()) {
            wrapper.in(ModelConfig::getId, modelIds);
        }
        List<ModelConfig> healthyModels = modelConfigMapper.selectList(wrapper);
        
        if (healthyModels.isEmpty()) {
            throw new BusinessException(ErrorCode.AI_MODEL_UNAVAILABLE);
        }
        return healthyModels.stream()
                .min(Comparator.comparingInt(ModelConfig::getPriority))
                .orElseThrow(() -> new BusinessException(ErrorCode.AI_MODEL_UNAVAILABLE));
    }

    private ModelConfig applyStrategy(String strategy, List<ModelConfig> models) {
        return switch (strategy.toUpperCase()) {
            case "WEIGHT" -> weightStrategy(models);
            case "PRIORITY" -> priorityStrategy(models);
            case "ROUND_ROBIN" -> roundRobinStrategy(models);
            default -> priorityStrategy(models);
        };
    }

    private ModelConfig weightStrategy(List<ModelConfig> models) {
        int totalWeight = models.stream().mapToInt(ModelConfig::getWeight).sum();
        if (totalWeight <= 0) return models.get(0);

        int random = ThreadLocalRandom.current().nextInt(totalWeight);
        int cumulative = 0;
        for (ModelConfig model : models) {
            cumulative += model.getWeight();
            if (random < cumulative) {
                return model;
            }
        }
        return models.get(models.size() - 1);
    }

    private ModelConfig priorityStrategy(List<ModelConfig> models) {
        return models.stream()
                .min(Comparator.comparingInt(ModelConfig::getPriority))
                .orElse(models.get(0));
    }

    private ModelConfig roundRobinStrategy(List<ModelConfig> models) {
        int index = Math.abs(roundRobinCounter.getAndIncrement()) % models.size();
        return models.get(index);
    }

    private List<ModelConfig> getHealthyModels(List<Long> modelIds) {
        LambdaQueryWrapper<ModelConfig> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ModelConfig::getEnabled, true)
                .eq(ModelConfig::getHealthy, true)
                .eq(ModelConfig::getDeleted, 0);
        if (modelIds != null && !modelIds.isEmpty()) {
            wrapper.in(ModelConfig::getId, modelIds);
        }
        return modelConfigMapper.selectList(wrapper);
    }

    private List<Long> parseModelIds(String modelIdsJson) {
        if (modelIdsJson == null || modelIdsJson.isBlank()) {
            return Collections.emptyList();
        }
        try {
            return objectMapper.readValue(modelIdsJson, new TypeReference<List<Long>>() {});
        } catch (JsonProcessingException e) {
            log.warn("解析模型ID列表失败: {}", modelIdsJson, e);
            return Collections.emptyList();
        }
    }

    private String toModelIdsJson(List<Long> modelIds) {
        if (modelIds == null || modelIds.isEmpty()) {
            return "[]";
        }
        try {
            return objectMapper.writeValueAsString(modelIds);
        } catch (JsonProcessingException e) {
            return "[]";
        }
    }

    private void clearCache() {
        try {
            redisUtil.delete(CACHE_KEY_ROUTING_RULES);
            log.debug("清除路由规则缓存成功");
        } catch (Exception e) {
            log.warn("清除路由规则缓存失败", e);
        }
    }

    private void copyDtoToEntity(RoutingRuleDTO dto, RoutingRule entity) {
        if (dto.getName() != null) entity.setName(dto.getName());
        if (dto.getStrategy() != null) entity.setStrategy(dto.getStrategy());
        if (dto.getEnabled() != null) entity.setEnabled(dto.getEnabled());
        if (dto.getPriority() != null) entity.setPriority(dto.getPriority());
        if (dto.getDescription() != null) entity.setDescription(dto.getDescription());
        if (dto.getModelIds() != null) entity.setModelIds(toModelIdsJson(dto.getModelIds()));
    }

    private RoutingRuleDTO toDTO(RoutingRule entity) {
        RoutingRuleDTO dto = new RoutingRuleDTO();
        dto.setId(entity.getId());
        dto.setName(entity.getName());
        dto.setStrategy(entity.getStrategy());
        dto.setEnabled(entity.getEnabled());
        dto.setPriority(entity.getPriority());
        dto.setDescription(entity.getDescription());
        dto.setModelIds(parseModelIds(entity.getModelIds()));
        return dto;
    }
}
