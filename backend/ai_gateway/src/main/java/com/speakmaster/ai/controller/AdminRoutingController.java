package com.speakmaster.ai.controller;

import com.speakmaster.ai.dto.RoutingRuleDTO;
import com.speakmaster.ai.service.IRoutingService;
import com.speakmaster.common.dto.Result;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 管理端路由控制器
 * 提供路由规则的增删改查接口
 *
 * @author SpeakMaster
 */
@RestController
@RequestMapping("/admin/ai/routing")
@RequiredArgsConstructor
public class AdminRoutingController {

    private final IRoutingService routingService;

    /** 获取路由规则列表 */
    @GetMapping("/rules")
    public Result<List<RoutingRuleDTO>> getAllRules() {
        return Result.success(routingService.getAllRules());
    }

    /** 创建路由规则 */
    @PostMapping("/rules")
    public Result<RoutingRuleDTO> createRule(@RequestBody RoutingRuleDTO dto) {
        return Result.success(routingService.createRule(dto));
    }

    /** 更新路由规则 */
    @PutMapping("/rules/{id}")
    public Result<RoutingRuleDTO> updateRule(@PathVariable Long id, @RequestBody RoutingRuleDTO dto) {
        return Result.success(routingService.updateRule(id, dto));
    }

    /** 删除路由规则 */
    @DeleteMapping("/rules/{id}")
    public Result<Void> deleteRule(@PathVariable Long id) {
        routingService.deleteRule(id);
        return Result.success();
    }
}
