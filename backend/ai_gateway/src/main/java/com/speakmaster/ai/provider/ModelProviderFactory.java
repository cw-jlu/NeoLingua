package com.speakmaster.ai.provider;

import com.speakmaster.ai.entity.ModelConfig;
import com.speakmaster.common.constant.LogMessages;
import com.speakmaster.common.enums.ErrorCode;
import com.speakmaster.common.exception.BusinessException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 模型提供方工厂
 * 根据模型配置的providerType返回对应的ModelProvider实例
 *
 * @author SpeakMaster
 */
@Slf4j
@Component
public class ModelProviderFactory {

    private final Map<String, ModelProvider> providerMap;

    /**
     * 通过Spring注入所有ModelProvider实现，按providerType建立映射
     */
    public ModelProviderFactory(List<ModelProvider> providers) {
        this.providerMap = providers.stream()
                .collect(Collectors.toMap(ModelProvider::getProviderType, Function.identity()));
        log.info("已注册模型提供方: {}", providerMap.keySet());
    }

    /**
     * 根据模型配置获取对应的Provider
     */
    public ModelProvider getProvider(ModelConfig config) {
        ModelProvider provider = providerMap.get(config.getProviderType());
        if (provider == null) {
            log.error("未找到模型提供方: providerType={}", config.getProviderType());
            throw new BusinessException(ErrorCode.AI_SERVICE_ERROR);
        }
        return provider;
    }

    /**
     * 根据提供方类型获取Provider
     */
    public ModelProvider getProvider(String providerType) {
        ModelProvider provider = providerMap.get(providerType);
        if (provider == null) {
            log.error("未找到模型提供方: providerType={}", providerType);
            throw new BusinessException(ErrorCode.AI_SERVICE_ERROR);
        }
        return provider;
    }
}
