package com.speakmaster.meeting.feign;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.Map;

/**
 * AI Gateway Feign客户端
 * 
 * @author SpeakMaster
 */
@FeignClient(name = "ai-gateway", url = "${ai.gateway.url:http://localhost:8088}")
public interface AiGatewayClient {

    /**
     * 调用AI Gateway聊天接口
     * 完整路径: /api/v1/user/ai/chat
     */
    @PostMapping("/api/v1/user/ai/chat")
    Map<String, Object> chat(@RequestBody Map<String, Object> request);
}
