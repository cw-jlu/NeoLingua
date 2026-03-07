package com.speakmaster.ai.controller;

import com.speakmaster.ai.dto.ModelConfigDTO;
import com.speakmaster.ai.dto.ModelHealthStatus;
import com.speakmaster.ai.dto.ModelMetrics;
import com.speakmaster.ai.dto.ModelStatistics;
import com.speakmaster.ai.dto.ModelTestResult;
import com.speakmaster.ai.service.IModelService;
import com.speakmaster.common.dto.Result;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 管理端模型控制器
 * 提供模型配置的增删改查、启用禁用、测试、健康检查、指标统计等接口
 *
 * @author SpeakMaster
 */
@RestController
@RequestMapping("/admin/ai/models")
@RequiredArgsConstructor
public class AdminModelController {

    private final IModelService modelService;

    /** 获取所有模型配置列�?*/
    @GetMapping
    public Result<List<ModelConfigDTO>> getAllModels() {
        return Result.success(modelService.getAllModels());
    }

    /** 创建模型配置 */
    @PostMapping
    public Result<ModelConfigDTO> createModel(@RequestBody ModelConfigDTO dto) {
        return Result.success(modelService.createModel(dto));
    }

    /** 获取模型详情 */
    @GetMapping("/{id}")
    public Result<ModelConfigDTO> getModelById(@PathVariable Long id) {
        return Result.success(modelService.getModelById(id));
    }

    /** 更新模型配置 */
    @PutMapping("/{id}")
    public Result<ModelConfigDTO> updateModel(@PathVariable Long id, @RequestBody ModelConfigDTO dto) {
        return Result.success(modelService.updateModel(id, dto));
    }

    /** 删除模型配置 */
    @DeleteMapping("/{id}")
    public Result<Void> deleteModel(@PathVariable Long id) {
        modelService.deleteModel(id);
        return Result.success();
    }

    /** 启用模型 */
    @PostMapping("/{id}/enable")
    public Result<Void> enableModel(@PathVariable Long id) {
        modelService.enableModel(id);
        return Result.success();
    }

    /** 禁用模型 */
    @PostMapping("/{id}/disable")
    public Result<Void> disableModel(@PathVariable Long id) {
        modelService.disableModel(id);
        return Result.success();
    }

    /** 测试模型连通�?*/
    @PostMapping("/{id}/test")
    public Result<ModelTestResult> testModel(@PathVariable Long id) {
        return Result.success(modelService.testModel(id));
    }

    /** 获取模型健康状�?*/
    @GetMapping("/{id}/health")
    public Result<ModelHealthStatus> getModelHealth(@PathVariable Long id) {
        return Result.success(modelService.getModelHealth(id));
    }

    /** 获取模型使用指标 */
    @GetMapping("/{id}/metrics")
    public Result<ModelMetrics> getModelMetrics(@PathVariable Long id) {
        return Result.success(modelService.getModelMetrics(id));
    }

    /** 获取模型统计数据 */
    @GetMapping("/statistics")
    public Result<ModelStatistics> getModelStatistics() {
        return Result.success(modelService.getModelStatistics());
    }
}
