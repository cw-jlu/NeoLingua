package com.speakmaster.admin.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.speakmaster.admin.dto.SystemConfigDTO;
import com.speakmaster.admin.entity.SystemConfig;

import java.util.List;

/**
 * 系统配置服务接口
 */
public interface ISystemConfigService extends IService<SystemConfig> {

    /**
     * 获取配置值
     */
    String getConfigValue(String configKey);

    /**
     * 获取所有配置
     */
    List<SystemConfigDTO> getAllConfigs();

    /**
     * 获取分类下的配置
     */
    List<SystemConfigDTO> getConfigsByCategory(String category);

    /**
     * 创建配置
     */
    SystemConfigDTO createConfig(SystemConfigDTO configDTO);

    /**
     * 更新配置
     */
    SystemConfigDTO updateConfig(String configKey, SystemConfigDTO configDTO);

    /**
     * 删除配置
     */
    void deleteConfig(String configKey);

    /**
     * 获取所有配置分类
     */
    List<String> getCategories();
}
