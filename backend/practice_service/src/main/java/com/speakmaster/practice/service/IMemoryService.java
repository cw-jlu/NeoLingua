package com.speakmaster.practice.service;

import java.util.List;
import java.util.Map;

/**
 * 三层记忆管理服务接口
 * 
 * @author SpeakMaster
 */
public interface IMemoryService {
    
    /**
     * 构建完整的AI提示词(包含三层记忆)
     * 
     * @param userId 用户ID
     * @param sessionId 会话ID
     * @param currentMessage 当前用户消息
     * @param rolePrompt AI角色设定
     * @return 完整的消息列表
     */
    List<Map<String, String>> buildPromptWithMemory(Long userId, Long sessionId, 
                                                     String currentMessage, String rolePrompt);
    
    /**
     * 生成对话总结(用于中期记忆)
     * 
     * @param sessionId 会话ID
     * @return 总结内容
     */
    String generateConversationSummary(Long sessionId);
    
    /**
     * 保存对话总结到ES(中期记忆)
     * 
     * @param userId 用户ID
     * @param sessionId 会话ID
     * @param summary 对话总结
     * @param keywords 关键词
     * @param messageCount 消息数量
     */
    void saveMidTermMemory(Long userId, Long sessionId, String summary, String keywords, int messageCount);
    
    /**
     * 更新用户画像(长期记忆)
     * 
     * @param userId 用户ID
     * @param profileData 画像数据
     */
    void updateLongTermMemory(Long userId, Map<String, Object> profileData);
}
