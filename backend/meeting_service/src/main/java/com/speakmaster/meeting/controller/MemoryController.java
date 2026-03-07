package com.speakmaster.meeting.controller;

import com.speakmaster.common.context.UserContextHolder;
import com.speakmaster.common.dto.Result;
import com.speakmaster.meeting.service.IMemoryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 记忆管理控制器
 * 
 * @author SpeakMaster
 */
@Slf4j
@RestController
@RequestMapping("/user/memory")
@RequiredArgsConstructor
public class MemoryController {

    private final IMemoryService memoryService;

    /**
     * 获取用户画像(长期记忆)
     */
    @GetMapping("/profile")
    public Result<Map<String, Object>> getUserProfile() {
        Long userId = UserContextHolder.getCurrentUserId();
        Map<String, Object> profile = memoryService.getLongTermMemory(userId);
        return Result.success(profile);
    }

    /**
     * 更新用户画像
     */
    @PutMapping("/profile")
    public Result<Void> updateUserProfile(@RequestBody Map<String, Object> profileData) {
        Long userId = UserContextHolder.getCurrentUserId();
        memoryService.updateLongTermMemory(userId, profileData);
        log.info("用户画像更新成功: userId={}", userId);
        return Result.success();
    }

    /**
     * 获取历史对话总结(中期记忆)
     */
    @GetMapping("/summaries")
    public Result<List<Map<String, Object>>> getConversationSummaries(
            @RequestParam(required = false) String query,
            @RequestParam(defaultValue = "10") int limit) {
        Long userId = UserContextHolder.getCurrentUserId();
        List<Map<String, Object>> summaries = memoryService.getMidTermMemory(userId, query != null ? query : "", limit);
        return Result.success(summaries);
    }

    /**
     * 生成会话总结
     */
    @PostMapping("/summaries/generate")
    public Result<String> generateSummary(
            @RequestParam String sessionId,
            @RequestParam String sessionType) {
        String summary = memoryService.generateConversationSummary(sessionId, sessionType);

        Long userId = UserContextHolder.getCurrentUserId();
        memoryService.saveMidTermMemory(userId, sessionId, sessionType, summary, "", 0);

        log.info("对话总结生成成功: userId={}, sessionId={}", userId, sessionId);
        return Result.success(summary);
    }

    /**
     * 获取短期记忆(最近对话)
     */
    @GetMapping("/recent")
    public Result<List<Map<String, Object>>> getRecentMemory(
            @RequestParam String sessionId,
            @RequestParam String sessionType,
            @RequestParam(defaultValue = "10") int limit) {
        List<Map<String, Object>> memories = memoryService.getShortTermMemory(sessionId, sessionType, limit);
        return Result.success(memories);
    }
}
