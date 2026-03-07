package com.speakmaster.ai.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.speakmaster.ai.dto.*;
import com.speakmaster.ai.entity.ModelConfig;
import com.speakmaster.ai.vo.ModelVO;

import java.util.List;

/**
 * 模型管理服务接口
 */
public interface IModelService extends IService<ModelConfig> {

    // ==================== 管理端接口 ====================

    /**
     * 获取所有模型配置（管理端）
     */
    List<ModelConfigDTO> getAllModels();

    /**
     * 根据ID获取模型配置（管理端）
     */
    ModelConfigDTO getModelById(Long id);

    /**
     * 创建模型配置
     */
    ModelConfigDTO createModel(ModelConfigDTO dto);

    /**
     * 更新模型配置
     */
    ModelConfigDTO updateModel(Long id, ModelConfigDTO dto);

    /**
     * 删除模型配置（逻辑删除）
     */
    void deleteModel(Long id);

    /**
     * 启用模型
     */
    void enableModel(Long id);

    /**
     * 禁用模型
     */
    void disableModel(Long id);

    // ==================== 用户端接口 ====================

    /**
     * 获取启用的模型列表（用户端）
     */
    List<ModelVO> getEnabledModels();

    /**
     * 获取用户端模型详情
     */
    ModelVO getModelVOById(Long id);

    /**
     * 获取推荐模型列表
     */
    List<ModelVO> getRecommendedModels();

    // ==================== 测试/健康/指标 ====================

    /**
     * 测试模型连通性
     */
    ModelTestResult testModel(Long id);

    /**
     * 获取模型健康状态
     */
    ModelHealthStatus getModelHealth(Long id);

    /**
     * 获取模型使用指标
     */
    ModelMetrics getModelMetrics(Long id);

    /**
     * 获取模型统计数据
     */
    ModelStatistics getModelStatistics();

    /**
     * 根据ID查找模型，不存在则抛出异常
     */
    ModelConfig findModelOrThrow(Long id);
}
