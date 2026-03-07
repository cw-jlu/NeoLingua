package com.speakmaster.ai.controller;

import com.speakmaster.ai.dto.ChatRequest;
import com.speakmaster.ai.dto.ChatResponse;
import com.speakmaster.ai.dto.StopRequest;
import com.speakmaster.ai.service.IChatService;
import com.speakmaster.common.dto.Result;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

/**
 * 用户端聊天控制器
 * 提供同步聊天、流式聊天和停止生成接口
 *
 * @author SpeakMaster
 */
@RestController
@RequestMapping("/user/ai")
@RequiredArgsConstructor
public class UserChatController {

    private final IChatService chatService;

    /** 同步聊天 */
    @PostMapping("/chat")
    public Result<ChatResponse> chat(@RequestBody ChatRequest request) {
        return Result.success(chatService.chat(request));
    }

    /** 流式聊天 */
    @PostMapping(value = "/chat/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter chatStream(@RequestBody ChatRequest request) {
        return chatService.chatStream(request);
    }

    /** 停止生成 */
    @PostMapping("/chat/stop")
    public Result<Void> stopGeneration(@RequestBody StopRequest request) {
        chatService.stopGeneration(request.getSessionId());
        return Result.success();
    }
}
