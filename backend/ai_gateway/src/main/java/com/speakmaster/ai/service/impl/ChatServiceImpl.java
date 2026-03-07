package com.speakmaster.ai.service.impl;

import com.speakmaster.ai.dto.ChatRequest;
import com.speakmaster.ai.dto.ChatResponse;
import com.speakmaster.ai.dto.ChatStreamEvent;
import com.speakmaster.ai.entity.ModelConfig;
import com.speakmaster.ai.provider.ModelProvider;
import com.speakmaster.ai.provider.ModelProviderFactory;
import com.speakmaster.ai.service.IChatService;
import com.speakmaster.ai.service.IMetricsService;
import com.speakmaster.ai.service.IRoutingService;
import com.speakmaster.common.enums.ErrorCode;
import com.speakmaster.common.exception.BusinessException;
import com.alibaba.csp.sentinel.annotation.SentinelResource;
import com.alibaba.csp.sentinel.slots.block.BlockException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 聊天服务实现类
 * 处理用户聊天请求，包括同步聊天、流式聊天和停止生成
 *
 * @author SpeakMaster
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ChatServiceImpl implements IChatService {

    private final IRoutingService routingService;
    private final ModelProviderFactory modelProviderFactory;
    private final IMetricsService metricsService;

    /** 会话ID -> SseEmitter映射，用于停止生成 */
    private final Map<String, SseEmitter> activeEmitters = new ConcurrentHashMap<>();

    @Override
    @SentinelResource(value = "chatService", blockHandler = "chatBlockHandler", fallback = "chatFallback")
    public ChatResponse chat(ChatRequest request) {
        ModelConfig model = routingService.selectModel(request);
        ModelProvider provider = modelProviderFactory.getProvider(model);

        log.info("同步聊天: model={}, sessionId={}", model.getName(), request.getSessionId());
        long startTime = System.currentTimeMillis();
        try {
            ChatResponse response = provider.chat(model, request);
            long responseTime = System.currentTimeMillis() - startTime;
            metricsService.recordCall(model.getId(), true, responseTime,
                    response.getTokenCount() != null ? response.getTokenCount() : 0);
            return response;
        } catch (Exception e) {
            long responseTime = System.currentTimeMillis() - startTime;
            metricsService.recordCall(model.getId(), false, responseTime, 0);
            throw e;
        }
    }

    @Override
    public SseEmitter chatStream(ChatRequest request) {
        ModelConfig model = routingService.selectModel(request);
        ModelProvider provider = modelProviderFactory.getProvider(model);

        // 创建SseEmitter，超时时间与模型配置一致
        long timeout = (model.getTimeout() != null ? model.getTimeout() : 60) * 1000L;
        SseEmitter emitter = new SseEmitter(timeout);

        String sessionId = request.getSessionId();
        if (sessionId != null) {
            activeEmitters.put(sessionId, emitter);
        }

        // 清理回调
        emitter.onCompletion(() -> {
            if (sessionId != null) activeEmitters.remove(sessionId);
            log.debug("流式聊天完成: sessionId={}", sessionId);
        });
        emitter.onTimeout(() -> {
            if (sessionId != null) activeEmitters.remove(sessionId);
            log.warn("流式聊天超时: sessionId={}", sessionId);
        });
        emitter.onError(e -> {
            if (sessionId != null) activeEmitters.remove(sessionId);
            log.error("流式聊天错误: sessionId={}", sessionId, e);
        });

        log.info("流式聊天: model={}, sessionId={}", model.getName(), sessionId);

        // 订阅流式事件并写入SseEmitter
        provider.chatStream(model, request)
                .subscribe(
                        event -> {
                            try {
                                emitter.send(SseEmitter.event()
                                        .name("message")
                                        .data(event));
                                if ("done".equals(event.getType()) || "error".equals(event.getType())) {
                                    emitter.complete();
                                }
                            } catch (IOException e) {
                                log.warn("发送SSE事件失败: sessionId={}", sessionId);
                                emitter.completeWithError(e);
                            }
                        },
                        error -> {
                            log.error("流式聊天异常: sessionId={}", sessionId, error);
                            try {
                                emitter.send(SseEmitter.event()
                                        .name("message")
                                        .data(new ChatStreamEvent("error", "AI服务异常", model.getId())));
                            } catch (IOException ignored) {}
                            emitter.completeWithError(error);
                        },
                        emitter::complete
                );

        return emitter;
    }

    @Override
    public void stopGeneration(String sessionId) {
        SseEmitter emitter = activeEmitters.remove(sessionId);
        if (emitter != null) {
            try {
                emitter.send(SseEmitter.event()
                        .name("message")
                        .data(new ChatStreamEvent("done", "", null)));
                emitter.complete();
            } catch (IOException e) {
                log.warn("停止生成时发送完成事件失败: sessionId={}", sessionId);
                emitter.completeWithError(e);
            }
            log.info("停止生成成功: sessionId={}", sessionId);
        } else {
            log.warn("未找到活跃的流式会话: sessionId={}", sessionId);
        }
    }

    /**
     * Sentinel限流降级处理
     */
    public ChatResponse chatBlockHandler(ChatRequest request, BlockException ex) {
        log.warn("聊天服务被Sentinel限流: {}", ex.getClass().getSimpleName());
        throw new BusinessException(ErrorCode.AI_SERVICE_ERROR);
    }

    /**
     * 熔断降级方法
     */
    private ChatResponse chatFallback(ChatRequest request, Throwable throwable) {
        log.warn("聊天服务熔断降级: error={}", throwable.getMessage());

        // 尝试使用其他模型
        try {
            ModelConfig fallbackModel = routingService.selectModel(request);
            ModelProvider provider = modelProviderFactory.getProvider(fallbackModel);
            return provider.chat(fallbackModel, request);
        } catch (Exception e) {
            log.error("降级后仍然失败", e);
            throw new BusinessException(ErrorCode.AI_SERVICE_ERROR);
        }
    }
}
