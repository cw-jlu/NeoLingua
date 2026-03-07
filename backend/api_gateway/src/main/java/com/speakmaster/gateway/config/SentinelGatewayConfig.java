package com.speakmaster.gateway.config;

import com.alibaba.csp.sentinel.adapter.gateway.common.SentinelGatewayConstants;
import com.alibaba.csp.sentinel.adapter.gateway.common.api.ApiDefinition;
import com.alibaba.csp.sentinel.adapter.gateway.common.api.ApiPathPredicateItem;
import com.alibaba.csp.sentinel.adapter.gateway.common.api.GatewayApiDefinitionManager;
import com.alibaba.csp.sentinel.adapter.gateway.common.rule.GatewayFlowRule;
import com.alibaba.csp.sentinel.adapter.gateway.common.rule.GatewayRuleManager;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;

import java.util.HashSet;
import java.util.Set;

/**
 * Sentinel Gateway 限流熔断配置
 * 定义API分组和限流规则
 * 
 * 注意：这里是代码方式配置默认规则，生产环境建议通过Sentinel Dashboard动态配置
 * 
 * @author SpeakMaster
 */
@Slf4j
@Configuration
public class SentinelGatewayConfig {

    @PostConstruct
    public void init() {
        initCustomizedApis();
        initGatewayRules();
        log.info("Sentinel Gateway 限流规则初始化完成");
    }

    /**
     * 自定义API分组
     */
    private void initCustomizedApis() {
        Set<ApiDefinition> definitions = new HashSet<>();

        // 用户服务API
        definitions.add(new ApiDefinition("user-api")
                .setPredicateItems(Set.of(
                        new ApiPathPredicateItem().setPattern("/user/**")
                                .setMatchStrategy(SentinelGatewayConstants.URL_MATCH_STRATEGY_PREFIX)
                )));

        // AI服务API（限流更严格）
        definitions.add(new ApiDefinition("ai-api")
                .setPredicateItems(Set.of(
                        new ApiPathPredicateItem().setPattern("/user/ai/**")
                                .setMatchStrategy(SentinelGatewayConstants.URL_MATCH_STRATEGY_PREFIX),
                        new ApiPathPredicateItem().setPattern("/user/ai-service/**")
                                .setMatchStrategy(SentinelGatewayConstants.URL_MATCH_STRATEGY_PREFIX)
                )));

        // 管理端API
        definitions.add(new ApiDefinition("admin-api")
                .setPredicateItems(Set.of(
                        new ApiPathPredicateItem().setPattern("/admin/**")
                                .setMatchStrategy(SentinelGatewayConstants.URL_MATCH_STRATEGY_PREFIX)
                )));

        GatewayApiDefinitionManager.loadApiDefinitions(definitions);
    }

    /**
     * 网关限流规则
     */
    private void initGatewayRules() {
        Set<GatewayFlowRule> rules = new HashSet<>();

        // 按路由ID限流 - 每个服务的默认限流
        String[] routeIds = {
                "user-service", "practice-service", "meeting-service",
                "community-service", "notification-service", "admin-service",
                "analysis-service"
        };
        for (String routeId : routeIds) {
            rules.add(new GatewayFlowRule(routeId)
                    .setCount(20)           // QPS阈值
                    .setIntervalSec(1));     // 统计窗口1秒
        }

        // AI相关服务限流更严格
        rules.add(new GatewayFlowRule("ai-gateway")
                .setCount(10)
                .setIntervalSec(1));
        rules.add(new GatewayFlowRule("ai-service")
                .setCount(10)
                .setIntervalSec(1));

        // 按API分组限流
        rules.add(new GatewayFlowRule("ai-api")
                .setResourceMode(SentinelGatewayConstants.RESOURCE_MODE_CUSTOM_API_NAME)
                .setCount(10)
                .setIntervalSec(1));

        GatewayRuleManager.loadRules(rules);
    }
}
