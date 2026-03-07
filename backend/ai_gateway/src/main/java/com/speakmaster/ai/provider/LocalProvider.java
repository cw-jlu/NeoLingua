package com.speakmaster.ai.provider;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.speakmaster.ai.dto.*;
import com.speakmaster.ai.entity.ModelConfig;
import com.speakmaster.common.enums.ErrorCode;
import com.speakmaster.common.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;

import java.time.Duration;
import java.util.List;

/**
 * 本地Transformers模型提供方
 * 通过HTTP调用 ai_service 的本地推理接口（/ai/local/chat）
 * 支持文本模型和多模态模型（音频输入）
 *
 * @author SpeakMaster
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class LocalProvider implements ModelProvider {

    private final WebClient webClient;
    private final ObjectMapper objectMapper = new ObjectMapper();

    /** ai_service 地址，默认 localhost:8089 */
    @Value("${ai.local.service.url:http://localhost:8089}")
    private String aiServiceUrl;

    @Override
    public String getProviderType() {
        return "LOCAL";
    }

    @Override
    public ChatResponse chat(ModelConfig config, ChatRequest request) {
        String endpoint = aiServiceUrl + "/ai/local/chat";
        long startTime = System.currentTimeMillis();

        try {
            ObjectNode body = buildRequestBody(config, request);
            String responseBody = webClient.post()
                    .uri(endpoint)
                    .bodyValue(body)
                    .retrieve()
                    .bodyToMono(String.class)
                    .timeout(Duration.ofSeconds(config.getTimeout() != null ? config.getTimeout() : 120))
                    .block();

            long responseTime = System.currentTimeMillis() - startTime;
            return parseResponse(responseBody, config, responseTime);
        } catch (Exception e) {
            log.error("本地模型调用失败: model={}, error={}", config.getModelId(), e.getMessage());
            throw new BusinessException(ErrorCode.AI_SERVICE_ERROR);
        }
    }

    @Override
    public Flux<ChatStreamEvent> chatStream(ModelConfig config, ChatRequest request) {
        // 本地Transformers模型暂不支持流式，降级为同步调用后模拟流式
        return Flux.create(sink -> {
            try {
                ChatResponse response = chat(config, request);
                String content = response.getContent();
                // 按字符分块模拟流式输出
                int chunkSize = 5;
                for (int i = 0; i < content.length(); i += chunkSize) {
                    String chunk = content.substring(i, Math.min(i + chunkSize, content.length()));
                    sink.next(new ChatStreamEvent("content", chunk, config.getId()));
                }
                sink.next(new ChatStreamEvent("done", "", config.getId()));
                sink.complete();
            } catch (Exception e) {
                log.error("本地模型流式调用失败: model={}, error={}", config.getModelId(), e.getMessage());
                sink.next(new ChatStreamEvent("error", "本地模型异常: " + e.getMessage(), config.getId()));
                sink.complete();
            }
        });
    }

    @Override
    public void stopGeneration(ModelConfig config, String sessionId) {
        log.debug("本地模型停止生成: sessionId={}", sessionId);
    }

    @Override
    public boolean healthCheck(ModelConfig config) {
        try {
            String endpoint = aiServiceUrl + "/ai/local/health";
            String response = webClient.get()
                    .uri(endpoint)
                    .retrieve()
                    .bodyToMono(String.class)
                    .timeout(Duration.ofSeconds(5))
                    .block();
            return response != null;
        } catch (Exception e) {
            log.warn("本地模型健康检查失败: error={}", e.getMessage());
            return false;
        }
    }

    // ==================== 内部方法 ====================

    private ObjectNode buildRequestBody(ModelConfig config, ChatRequest request) {
        ObjectNode body = objectMapper.createObjectNode();
        // model_name 使用 config.getModelId()（存的是模型路径或HuggingFace名称）
        body.put("model_name", config.getModelId());
        body.put("multimodal", Boolean.TRUE.equals(config.getMultimodal()));

        // 消息列表
        ArrayNode messages = objectMapper.createArrayNode();
        if (request.getMessages() != null) {
            for (Message msg : request.getMessages()) {
                ObjectNode msgNode = objectMapper.createObjectNode();
                msgNode.put("role", msg.getRole());
                msgNode.put("content", msg.getContent());
                messages.add(msgNode);
            }
        }
        body.set("messages", messages);

        // 多模态：传音频URL，由 ai_service 负责从MinIO取音频
        if (Boolean.TRUE.equals(config.getMultimodal()) && request.getAudioUrl() != null) {
            body.put("audio_url", request.getAudioUrl());
        }

        Double temp = request.getTemperature() != null ? request.getTemperature() : config.getTemperature();
        if (temp != null) body.put("temperature", temp);
        Integer maxTokens = request.getMaxTokens() != null ? request.getMaxTokens() : config.getMaxTokens();
        if (maxTokens != null) body.put("max_tokens", maxTokens);

        return body;
    }

    private ChatResponse parseResponse(String responseBody, ModelConfig config, long responseTime) {
        try {
            JsonNode root = objectMapper.readTree(responseBody);
            ChatResponse response = new ChatResponse();
            response.setContent(root.path("content").asText(""));
            response.setModelId(config.getId());
            response.setModelName(config.getName());
            response.setResponseTime(responseTime);
            response.setTokenCount(root.path("token_count").asInt(0));
            return response;
        } catch (Exception e) {
            log.error("解析本地模型响应失败", e);
            throw new BusinessException(ErrorCode.AI_SERVICE_ERROR);
        }
    }
}
