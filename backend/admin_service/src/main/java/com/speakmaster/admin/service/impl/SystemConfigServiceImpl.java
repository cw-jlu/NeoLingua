package com.speakmaster.admin.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.speakmaster.admin.dto.SystemConfigDTO;
import com.speakmaster.admin.entity.SystemConfig;
import com.speakmaster.admin.mapper.SystemConfigMapper;
import com.speakmaster.admin.service.ISystemConfigService;
import com.speakmaster.common.enums.ErrorCode;
import com.speakmaster.common.exception.BusinessException;
import com.speakmaster.common.utils.RedisUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 系统配置服务实现类
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SystemConfigServiceImpl extends ServiceImpl<SystemConfigMapper, SystemConfig> implements ISystemConfigService {

    private final RedisUtil redisUtil;

    @Override
    @Cacheable(value = "system_config", key = "#configKey")
    public String getConfigValue(String configKey) {
        LambdaQueryWrapper<SystemConfig> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SystemConfig::getConfigKey, configKey)
                .eq(SystemConfig::getDeleted, 0);
        SystemConfig config = this.getOne(wrapper);
        return config != null ? config.getConfigValue() : null;
    }

    @Override
    public List<SystemConfigDTO> getAllConfigs() {
        LambdaQueryWrapper<SystemConfig> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SystemConfig::getDeleted, 0);
        List<SystemConfig> configs = this.list(wrapper);
        return configs.stream().map(this::convertToDTO).collect(Collectors.toList());
    }

    @Override
    public List<SystemConfigDTO> getConfigsByCategory(String category) {
        LambdaQueryWrapper<SystemConfig> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SystemConfig::getCategory, category)
                .eq(SystemConfig::getDeleted, 0);
        List<SystemConfig> configs = this.list(wrapper);
        return configs.stream().map(this::convertToDTO).collect(Collectors.toList());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public SystemConfigDTO createConfig(SystemConfigDTO configDTO) {
        // 检查配置键是否已存在
        LambdaQueryWrapper<SystemConfig> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SystemConfig::getConfigKey, configDTO.getConfigKey())
                .eq(SystemConfig::getDeleted, 0);
        if (this.count(wrapper) > 0) {
            throw new BusinessException(ErrorCode.CONFIG_KEY_EXISTS);
        }

        SystemConfig config = new SystemConfig();
        config.setConfigKey(configDTO.getConfigKey());
        config.setConfigValue(configDTO.getConfigValue());
        config.setDescription(configDTO.getDescription());
        config.setCategory(configDTO.getCategory());
        config.setIsEnabled(configDTO.getIsEnabled() != null ? configDTO.getIsEnabled() : 1);

        this.save(config);
        log.info("创建系统配置: configKey={}", config.getConfigKey());
        return convertToDTO(config);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    @CacheEvict(value = "system_config", key = "#configKey")
    public SystemConfigDTO updateConfig(String configKey, SystemConfigDTO configDTO) {
        LambdaQueryWrapper<SystemConfig> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SystemConfig::getConfigKey, configKey)
                .eq(SystemConfig::getDeleted, 0);
        SystemConfig config = this.getOne(wrapper);
        if (config == null) {
            throw new BusinessException(ErrorCode.CONFIG_NOT_FOUND);
        }

        if (configDTO.getConfigValue() != null) config.setConfigValue(configDTO.getConfigValue());
        if (configDTO.getDescription() != null) config.setDescription(configDTO.getDescription());
        if (configDTO.getCategory() != null) config.setCategory(configDTO.getCategory());
        if (configDTO.getIsEnabled() != null) config.setIsEnabled(configDTO.getIsEnabled());

        this.updateById(config);
        log.info("更新系统配置: configKey={}", configKey);
        return convertToDTO(config);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    @CacheEvict(value = "system_config", key = "#configKey")
    public void deleteConfig(String configKey) {
        LambdaQueryWrapper<SystemConfig> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SystemConfig::getConfigKey, configKey)
                .eq(SystemConfig::getDeleted, 0);
        SystemConfig config = this.getOne(wrapper);
        if (config == null) {
            throw new BusinessException(ErrorCode.CONFIG_NOT_FOUND);
        }

        config.markDeleted();
        this.updateById(config);
        log.info("删除系统配置: configKey={}", configKey);
    }

    @Override
    public List<String> getCategories() {
        return baseMapper.selectDistinctCategories();
    }

    /**
     * 转换为DTO
     */
    private SystemConfigDTO convertToDTO(SystemConfig config) {
        SystemConfigDTO dto = new SystemConfigDTO();
        dto.setId(config.getId());
        dto.setConfigKey(config.getConfigKey());
        dto.setConfigValue(config.getConfigValue());
        dto.setDescription(config.getDescription());
        dto.setCategory(config.getCategory());
        dto.setIsEnabled(config.getIsEnabled());
        return dto;
    }
}
