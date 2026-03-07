package com.speakmaster.ai.service;

import com.speakmaster.ai.dto.ChatRequest;
import com.speakmaster.ai.dto.ChatResponse;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

/**
 * 聊天服务接口
 */
public interface IChatService {

    /**
     * 同步聊天
     */
    ChatResponse chat(ChatRequest request);

    /**
     * 流式聊天
     */
    SseEmitter chatStream(ChatRequest request);

    /**
     * 停止生成
     */
    void stopGeneration(String sessionId);
}
