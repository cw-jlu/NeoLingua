package com.speakmaster.practice.feign;

import com.speakmaster.common.dto.Result;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.Map;

/**
 * AI服务Feign客户�?
 * 
 * @author SpeakMaster
 */
@FeignClient(name = "ai-service", url = "${ai.service.url:http://localhost:8089}")
public interface AIServiceClient {

    /**
     * 发送消息到AI服务
     */
    @PostMapping("/ai/chat")
    Result<Map<String, Object>> chat(@RequestBody Map<String, Object> request);

    /**
     * 生成反馈
     */
    @PostMapping("/ai/feedback")
    Result<Map<String, Object>> generateFeedback(@RequestBody Map<String, Object> request);
}
