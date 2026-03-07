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
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;

import java.time.Duration;
import java.util.List;

/**
 * 远程API模型提供者
 * 支持OpenAI兼容的API格式（/v1/chat/completions）
 *
 * @author SpeakMaster
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RemoteApiProvider implements ModelProvider {

    private final WebClient webClient;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public String getProviderType() {
        return "REMOTE_API";
    }

    @Override
    public ChatResponse chat(ModelConfig config, ChatRequest request) {
        String endpoint = config.getEndpoint() + "/v1/chat/completions";
        long startTime = System.currentTimeMillis();

        try {
            ObjectNode body = buildRequestBody(config, request, false);
            String responseBody = webClient.post()
                    .uri(endpoint)
                    .header("Authorization", "Bearer " + config.getApiKey())
                    .header("Content-Type", "application/json")
                    .bodyValue(body)
                    .retrieve()
                    .bodyToMono(String.class)
                    .timeout(Duration.ofSeconds(config.getTimeout() != null ? config.getTimeout() : 30))
                    .block();

            long responseTime = System.currentTimeMillis() - startTime;
            return parseResponse(responseBody, config, responseTime);
        } catch (Exception e) {
            log.error("远程API调用失败: model={}, error={}", config.getModelId(), e.getMessage());
            throw new BusinessException(ErrorCode.AI_SERVICE_ERROR);
        }
    }

    @Override
    public Flux<ChatStreamEvent> chatStream(ModelConfig config, ChatRequest request) {
        String endpoint = config.getEndpoint() + "/v1/chat/completions";
        ObjectNode body = buildRequestBody(config, request, true);

        return webClient.post()
                .uri(endpoint)
                .header("Authorization", "Bearer " + config.getApiKey())
                .accept(MediaType.TEXT_EVENT_STREAM)
                .bodyValue(body)
                .retrieve()
                .bodyToFlux(String.class)
                .timeout(Duration.ofSeconds(config.getTimeout() != null ? config.getTimeout() : 60))
                .filter(line -> !line.isBlank() && !line.equals("[DONE]"))
                .map(line -> parseStreamLine(line, config.getId()))
                .onErrorResume(e -> {
                    log.error("远程API流式调用失败: model={}, error={}", config.getModelId(), e.getMessage());
                    return Flux.just(new ChatStreamEvent("error", "AI服务异常: " + e.getMessage(), config.getId()));
                });
    }

    @Override
    public void stopGeneration(ModelConfig config, String sessionId) {
        // 远程API通过关闭连接实现停止
        log.debug("远程API停止生成: sessionId={}", sessionId);
    }

    @Override
    public boolean healthCheck(ModelConfig config) {
        try {
            String endpoint = config.getEndpoint() + "/v1/models";
            String response = webClient.get()
                    .uri(endpoint)
                    .header("Authorization", "Bearer " + config.getApiKey())
                    .retrieve()
                    .bodyToMono(String.class)
                    .timeout(Duration.ofSeconds(5))
                    .block();
            return response != null;
        } catch (Exception e) {
            log.warn("远程API健康检查失败: endpoint={}, error={}", config.getEndpoint(), e.getMessage());
            return false;
        }
    }

    // ==================== 内部方法 ====================

    /**
     * 构建OpenAI兼容请求体
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
                // 最后一条用户消息且模型支持多模态且有音频URL，使用多模态content格式
                if (isMultimodal && audioUrl != null && !audioUrl.isBlank()
                        && "user".equals(msg.getRole()) && i == msgList.size() - 1) {
                    ArrayNode contentArray = objectMapper.createArrayNode();
                    // 文本部分
                    ObjectNode textPart = objectMapper.createObjectNode();
                    textPart.put("type", "text");
                    textPart.put("text", msg.getContent());
                    contentArray.add(textPart);
                    // 音频URL部分（兼容支持audio_url的多模态API）
                    ObjectNode audioPart = objectMapper.createObjectNode();
                    audioPart.put("type", "audio_url");
                    ObjectNode audioObj = objectMapper.createObjectNode();
                    audioObj.put("url", audioUrl);
                    audioPart.set("audio_url", audioObj);
                    contentArray.add(audioPart);
                    msgNode.set("content", contentArray);
                } else {
                    msgNode.put("content", msg.getContent());
                }
                messages.add(msgNode);
            }
        }
        body.set("messages", messages);

        // 设置参数
        Double temp = request.getTemperature() != null ? request.getTemperature() : config.getTemperature();
        if (temp != null) body.put("temperature", temp);
        Integer maxTokens = request.getMaxTokens() != null ? request.getMaxTokens() : config.getMaxTokens();
        if (maxTokens != null) body.put("max_tokens", maxTokens);

        return body;
    }

    /**
     * 解析OpenAI格式同步响应
     */
    private ChatResponse parseResponse(String responseBody, ModelConfig config, long responseTime) {
        try {
            JsonNode root = objectMapper.readTree(responseBody);
            ChatResponse response = new ChatResponse();

            JsonNode choices = root.path("choices");
            if (choices.isArray() && !choices.isEmpty()) {
                response.setContent(choices.get(0).path("message").path("content").asText(""));
            }

            response.setModelId(config.getId());
            response.setModelName(config.getName());
            response.setResponseTime(responseTime);

            // 解析token使用量
            JsonNode usage = root.path("usage");
            response.setTokenCount(usage.path("total_tokens").asInt(0));

            return response;
        } catch (Exception e) {
            log.error("解析远程API响应失败", e);
            throw new BusinessException(ErrorCode.AI_SERVICE_ERROR);
        }
    }

    /**
     * 解析SSE流式响应行
     */
    private ChatStreamEvent parseStreamLine(String line, Long modelId) {
        try {
            // 去掉SSE前缀 "data: "
            String data = line.startsWith("data: ") ? line.substring(6) : line;
            if (data.equals("[DONE]")) {
                return new ChatStreamEvent("done", "", modelId);
            }

            JsonNode root = objectMapper.readTree(data);
            JsonNode choices = root.path("choices");
            if (choices.isArray() && !choices.isEmpty()) {
                JsonNode delta = choices.get(0).path("delta");
                String finishReason = choices.get(0).path("finish_reason").asText(null);
                if ("stop".equals(finishReason)) {
                    return new ChatStreamEvent("done", "", modelId);
                }
                String content = delta.path("content").asText("");
                return new ChatStreamEvent("content", content, modelId);
            }
            return new ChatStreamEvent("content", "", modelId);
        } catch (Exception e) {
            return new ChatStreamEvent("error", "解析响应失败", modelId);
        }
    }
}
