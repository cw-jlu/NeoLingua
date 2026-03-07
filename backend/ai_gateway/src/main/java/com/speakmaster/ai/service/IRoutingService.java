package com.speakmaster.ai.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.speakmaster.ai.dto.ChatRequest;
import com.speakmaster.ai.dto.RoutingRuleDTO;
import com.speakmaster.ai.entity.ModelConfig;
import com.speakmaster.ai.entity.RoutingRule;

import java.util.List;

/**
 * 路由服务接口
 */
public interface IRoutingService extends IService<RoutingRule> {

    // ==================== 模型选择 ====================

    /**
     * 根据路由规则选择模型
     */
    ModelConfig selectModel(ChatRequest request);

    // ==================== 路由规则CRUD ====================

    /**
     * 获取所有路由规则
     */
    List<RoutingRuleDTO> getAllRules();

    /**
     * 创建路由规则
     */
    RoutingRuleDTO createRule(RoutingRuleDTO dto);

    /**
     * 更新路由规则
     */
    RoutingRuleDTO updateRule(Long id, RoutingRuleDTO dto);

    /**
     * 删除路由规则（逻辑删除）
     */
    void deleteRule(Long id);
}
