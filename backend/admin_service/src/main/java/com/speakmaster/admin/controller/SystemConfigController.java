package com.speakmaster.admin.controller;

import com.speakmaster.admin.dto.SystemConfigDTO;
import com.speakmaster.admin.service.ISystemConfigService;
import com.speakmaster.common.dto.Result;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 系统配置控制器
 * 
 * @author SpeakMaster
 */
@RestController
@RequestMapping("/admin/system/config")
@RequiredArgsConstructor
public class SystemConfigController {

    private final ISystemConfigService configService;

    /**
     * 获取所有配�?
     */
    @GetMapping
    public Result<List<SystemConfigDTO>> getAllConfigs() {
        return Result.success(configService.getAllConfigs());
    }

    /**
     * 创建配置�?
     */
    @PostMapping
    public Result<SystemConfigDTO> createConfig(@RequestBody SystemConfigDTO configDTO) {
        return Result.success(configService.createConfig(configDTO));
    }

    /**
     * 更新配置�?
     */
    @PutMapping("/{key}")
    public Result<SystemConfigDTO> updateConfig(@PathVariable String key, @RequestBody SystemConfigDTO configDTO) {
        return Result.success(configService.updateConfig(key, configDTO));
    }

    /**
     * 删除配置�?
     */
    @DeleteMapping("/{key}")
    public Result<Void> deleteConfig(@PathVariable String key) {
        configService.deleteConfig(key);
        return Result.success();
    }

    /**
     * 获取配置分类列表
     */
    @GetMapping("/categories")
    public Result<List<String>> getCategories() {
        return Result.success(configService.getCategories());
    }
}
