package com.speakmaster.ai.provider;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.speakmaster.ai.dto.*;
import com.speakmaster.ai.entity.ModelConfig;
import com.speakmaster.common.constant.LogMessages;
import com.speakmaster.common.enums.ErrorCode;
import com.speakmaster.common.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;

import java.time.Duration;
import java.util.Map;
import java.util.List;
+
/**
 * Ollama模型提供者
 * 通过HTTP调用本地Ollama服务的API
 *
 * @author SpeakMaster
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OllamaProvider implements ModelProvider {

    private final WebClient webClient;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public String getProviderType() {
        return "OLLAMA";
    }

    @Override
    public ChatResponse chat(ModelConfig config, ChatRequest request) {
        String endpoint = config.getEndpoint() + "/api/chat";
        long startTime = System.currentTimeMillis();

        try {
            ObjectNode body = buildRequestBody(config, request, false);
            String responseBody = webClient.post()
                    .uri(endpoint)
                    .bodyValue(body)
                    .retrieve()
                    .bodyToMono(String.class)
                    .timeout(Duration.ofSeconds(config.getTimeout() != null ? config.getTimeout() : 30))
                    .block();

            long responseTime = System.currentTimeMillis() - startTime;
            return parseResponse(responseBody, config, responseTime);
        } catch (Exception e) {
            log.error("Ollama调用失败: model={}, error={}", config.getModelId(), e.getMessage());
            throw new BusinessException(ErrorCode.AI_SERVICE_ERROR);
        }
    }

    @Override
    public Flux<ChatStreamEvent> chatStream(ModelConfig config, ChatRequest request) {
        String endpoint = config.getEndpoint() + "/api/chat";
        ObjectNode body = buildRequestBody(config, request, true);

        return webClient.post()
                .uri(endpoint)
                .bodyValue(body)
                .retrieve()
                .bodyToFlux(String.class)
                .timeout(Duration.ofSeconds(config.getTimeout() != null ? config.getTimeout() : 60))
                .map(line -> parseStreamLine(line, config.getId()))
                .onErrorResume(e -> {
                    log.error("Ollama流式调用失败: model={}, error={}", config.getModelId(), e.getMessage());
                    return Flux.just(new ChatStreamEvent("error", "AI服务异常: " + e.getMessage(), config.getId()));
                });
    }

    @Override
    public void stopGeneration(ModelConfig config, String sessionId) {
        // Ollama不支持主动停止，由调用方关闭连接实现
        log.debug("Ollama停止生成: sessionId={}", sessionId);
    }

    @Override
    public boolean healthCheck(ModelConfig config) {
        try {
            String endpoint = config.getEndpoint() + "/api/tags";
            String response = webClient.get()
                    .uri(endpoint)
                    .retrieve()
                    .bodyToMono(String.class)
                    .timeout(Duration.ofSeconds(5))
                    .block();
            return response != null;
        } catch (Exception e) {
            log.warn("Ollama健康检查失败: endpoint={}, error={}", config.getEndpoint(), e.getMessage());
            return false;
        }
    }

    // ==================== 内部方法 ====================

    /**
     * 构建Ollama请求体
     */
    private ObjectNode buildRequestBody(ModelConfig config, ChatRequest request, boolean stream) {
        ObjectNode body = objectMapper.createObjectNode();
        body.put("model", config.getModelId());
        body.put("stream", stream);

        boolean isMultimodal = Boolean.TRUE.equals(config.getMultimodal());
        String audioUrl = request.getAudioUrl();

        // 构建消息列表
        ArrayNode messages = objectMapper.createArrayNode();
        if (request.getMessages() != null) {
            List<Message> msgList = request.getMessages();
            for (int i = 0; i < msgList.size(); i++) {
                Message msg = msgList.get(i);
                ObjectNode msgNode = objectMapper.createObjectNode();
                msgNode.put("role", msg.getRole());
                // 最后一条用户消息且模型支持多模态且有音频URL，附加音频信息
                if (isMultimodal && audioUrl != null && !audioUrl.isBlank()
                        && "user".equals(msg.getRole()) && i == msgList.size() - 1) {
                    msgNode.put("content", msg.getContent() + "\n[audio: " + audioUrl + "]");
                } else {
                    msgNode.put("content", msg.getContent());
                }
                messages.add(msgNode);
            }
        }
        body.set("messages", messages);

        // 设置参数
        ObjectNode options = objectMapper.createObjectNode();
        Double temp = request.getTemperature() != null ? request.getTemperature() : config.getTemperature();
        if (temp != null) options.put("temperature", temp);
        Integer maxTokens = request.getMaxTokens() != null ? request.getMaxTokens() : config.getMaxTokens();
        if (maxTokens != null) options.put("num_predict", maxTokens);
        body.set("options", options);

        return body;
    }

    /**
     * 解析Ollama同步响应
     */
    private ChatResponse parseResponse(String responseBody, ModelConfig config, long responseTime) {
        try {
            JsonNode root = objectMapper.readTree(responseBody);
            ChatResponse response = new ChatResponse();
            response.setContent(root.path("message").path("content").asText(""));
            response.setModelId(config.getId());
            response.setModelName(config.getName());
            response.setResponseTime(responseTime);
            // Ollama返回的eval_count作为token数
            response.setTokenCount(root.path("eval_count").asInt(0));
            return response;
        } catch (Exception e) {
            log.error("解析Ollama响应失败", e);
            throw new BusinessException(ErrorCode.AI_SERVICE_ERROR);
        }
    }

    /**
     * 解析Ollama流式响应行
     */
    private ChatStreamEvent parseStreamLine(String line, Long modelId) {
        try {
            JsonNode root = objectMapper.readTree(line);
            boolean done = root.path("done").asBoolean(false);
            if (done) {
                return new ChatStreamEvent("done", "", modelId);
            }
            String content = root.path("message").path("content").asText("");
            return new ChatStreamEvent("content", content, modelId);
        } catch (Exception e) {
            return new ChatStreamEvent("error", "解析响应失败", modelId);
        }
    }
}
