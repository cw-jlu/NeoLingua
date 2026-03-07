package com.speakmaster.ai.controller;

import com.speakmaster.ai.service.IModelService;
import com.speakmaster.ai.vo.ModelVO;
import com.speakmaster.common.dto.Result;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 用户端模型控制器
 * 提供可用模型列表、模型详情、推荐模型等接口
 *
 * @author SpeakMaster
 */
@RestController
@RequestMapping("/user/ai/models")
@RequiredArgsConstructor
public class UserModelController {

    private final IModelService modelService;

    /** 获取可用模型列表 */
    @GetMapping
    public Result<List<ModelVO>> getEnabledModels() {
        return Result.success(modelService.getEnabledModels());
    }

    /** 获取模型详情 */
    @GetMapping("/{id}")
    public Result<ModelVO> getModelById(@PathVariable Long id) {
        return Result.success(modelService.getModelVOById(id));
    }

    /** 获取推荐模型列表 */
    @GetMapping("/recommended")
    public Result<List<ModelVO>> getRecommendedModels() {
        return Result.success(modelService.getRecommendedModels());
    }
}
