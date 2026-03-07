package com.speakmaster.meeting.service;

import java.util.List;
import java.util.Map;

/**
 * 三层记忆管理服务接口
 * 
 * @author SpeakMaster
 */
public interface IMemoryService {
    
    /**
     * 获取短期记忆(最近N条消息)
     * 
     * @param sessionId 会话ID
     * @param sessionType 会话类型: meeting/practice
     * @param limit 消息数量限制
     * @return 消息列表
     */
    List<Map<String, Object>> getShortTermMemory(String sessionId, String sessionType, int limit);
    
    /**
     * 获取中期记忆(ES中的对话总结)
     * 
     * @param userId 用户ID
     * @param query 查询关键词
     * @param limit 返回数量
     * @return 对话总结列表
     */
    List<Map<String, Object>> getMidTermMemory(Long userId, String query, int limit);
    
    /**
     * 获取长期记忆(用户画像)
     * 
     * @param userId 用户ID
     * @return 用户画像信息
     */
    Map<String, Object> getLongTermMemory(Long userId);
    
    /**
     * 构建完整的AI提示词(包含三层记忆)
     * 
     * @param userId 用户ID
     * @param sessionId 会话ID
     * @param sessionType 会话类型
     * @param currentMessage 当前用户消息
     * @param rolePrompt AI角色设定
     * @return 完整的消息列表
     */
    List<Map<String, String>> buildPromptWithMemory(Long userId, String sessionId, 
                                                     String sessionType, String currentMessage, 
                                                     String rolePrompt);
    
    /**
     * 保存对话总结到ES(中期记忆)
     * 
     * @param userId 用户ID
     * @param sessionId 会话ID
     * @param sessionType 会话类型
     * @param summary 对话总结
     * @param keywords 关键词
     * @param messageCount 消息数量
     */
    void saveMidTermMemory(Long userId, String sessionId, String sessionType, 
                          String summary, String keywords, int messageCount);
    
    /**
     * 更新用户画像(长期记忆)
     * 
     * @param userId 用户ID
     * @param profileData 画像数据
     */
    void updateLongTermMemory(Long userId, Map<String, Object> profileData);
    
    /**
     * 生成对话总结(用于中期记忆)
     * 
     * @param sessionId 会话ID
     * @param sessionType 会话类型
     * @return 总结内容
     */
    String generateConversationSummary(String sessionId, String sessionType);
}
