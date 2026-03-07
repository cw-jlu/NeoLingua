package com.speakmaster.meeting.service.impl;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import co.elastic.clients.elasticsearch.core.search.Hit;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.speakmaster.meeting.entity.ConversationSummary;
import com.speakmaster.meeting.entity.MeetingMessage;
import com.speakmaster.meeting.entity.UserProfile;
import com.speakmaster.meeting.feign.AiGatewayClient;
import com.speakmaster.meeting.mapper.ConversationSummaryMapper;
import com.speakmaster.meeting.mapper.MeetingMessageMapper;
import com.speakmaster.meeting.mapper.UserProfileMapper;
import com.speakmaster.meeting.service.IMemoryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 三层记忆管理服务实现
 * 
 * @author SpeakMaster
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MemoryServiceImpl implements IMemoryService {

    private final MeetingMessageMapper meetingMessageMapper;
    private final ConversationSummaryMapper summaryMapper;
    private final UserProfileMapper profileMapper;
    private final ElasticsearchClient esClient;
    private final AiGatewayClient aiGatewayClient;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public List<Map<String, Object>> getShortTermMemory(String sessionId, String sessionType, int limit) {
        log.debug("获取短期记忆: sessionId={}, type={}, limit={}", sessionId, sessionType, limit);
        
        try {
            // 根据会话类型查询最近的消息
            Long meetingId = Long.parseLong(sessionId.replace("meeting-", "").replace("practice-", ""));
            
            LambdaQueryWrapper<MeetingMessage> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(MeetingMessage::getMeetingId, meetingId)
                    .eq(MeetingMessage::getDeleted, 0)
                    .orderByDesc(MeetingMessage::getCreateTime)
                    .last("LIMIT " + limit);
            
            List<MeetingMessage> messages = meetingMessageMapper.selectList(wrapper);
            Collections.reverse(messages); // 转为时间正序
            
            return messages.stream().map(msg -> {
                Map<String, Object> memory = new HashMap<>();
                memory.put("role", msg.getSenderId() < 0 ? "assistant" : "user");
                memory.put("content", msg.getContent());
                memory.put("timestamp", msg.getCreateTime());
                return memory;
            }).collect(Collectors.toList());
        } catch (Exception e) {
            log.error("获取短期记忆失败", e);
            return new ArrayList<>();
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public List<Map<String, Object>> getMidTermMemory(Long userId, String query, int limit) {
        log.debug("获取中期记忆: userId={}, query={}, limit={}", userId, query, limit);
        
        try {
            // 从ES中搜索相关的对话总结
            SearchResponse<Map> response = esClient.search(s -> s
                    .index("conversation_summaries")
                    .query(q -> q
                        .bool(b -> b
                            .must(m -> m.term(t -> t.field("userId").value(userId)))
                            .should(sh -> sh.match(mt -> mt.field("summary").query(query)))
                            .should(sh -> sh.match(mt -> mt.field("keywords").query(query)))
                        )
                    )
                    .size(limit),
                Map.class
            );
            
            List<Map<String, Object>> summaries = new ArrayList<>();
            for (Hit<Map> hit : response.hits().hits()) {
                Map<String, Object> source = hit.source();
                if (source != null) {
                    summaries.add(source);
                }
            }
            
            log.debug("从ES获取到{}条中期记忆", summaries.size());
            return summaries;
        } catch (Exception e) {
            log.warn("从ES获取中期记忆失败,降级到MySQL查询", e);
            
            // 降级方案: 从MySQL查询
            try {
                LambdaQueryWrapper<ConversationSummary> wrapper = new LambdaQueryWrapper<>();
                wrapper.eq(ConversationSummary::getUserId, userId)
                        .eq(ConversationSummary::getDeleted, 0)
                        .orderByDesc(ConversationSummary::getCreateTime)
                        .last("LIMIT " + limit);
                
                List<ConversationSummary> summaries = summaryMapper.selectList(wrapper);
                return summaries.stream().map(summary -> {
                    Map<String, Object> memory = new HashMap<>();
                    memory.put("summary", summary.getSummary());
                    memory.put("keywords", summary.getKeywords());
                    memory.put("sessionType", summary.getSessionType());
                    memory.put("messageCount", summary.getMessageCount());
                    memory.put("time", summary.getStartTime());
                    return memory;
                }).collect(Collectors.toList());
            } catch (Exception ex) {
                log.error("MySQL降级查询也失败", ex);
                return new ArrayList<>();
            }
        }
    }

    @Override
    public Map<String, Object> getLongTermMemory(Long userId) {
        log.debug("获取长期记忆: userId={}", userId);
        
        try {
            LambdaQueryWrapper<UserProfile> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(UserProfile::getUserId, userId)
                    .eq(UserProfile::getDeleted, 0);
            
            UserProfile profile = profileMapper.selectOne(wrapper);
            
            if (profile == null) {
                // 创建默认画像
                profile = new UserProfile();
                profile.setUserId(userId);
                profile.setEnglishLevel("intermediate");
                profile.setLearningGoals("提高英语口语能力");
                profile.setInterests("[]");
                profile.setCommonMistakes("[]");
                profile.setPronunciationWeaknesses("[]");
                profile.setLearningStyle("interactive");
                profile.setTotalPracticeMinutes(0);
                profile.setTotalConversations(0);
                profile.setPersonalizedPrompt("");
                profileMapper.insert(profile);
            }
            
            Map<String, Object> memory = new HashMap<>();
            memory.put("englishLevel", profile.getEnglishLevel());
            memory.put("learningGoals", profile.getLearningGoals());
            memory.put("interests", parseJson(profile.getInterests()));
            memory.put("commonMistakes", parseJson(profile.getCommonMistakes()));
            memory.put("pronunciationWeaknesses", parseJson(profile.getPronunciationWeaknesses()));
            memory.put("learningStyle", profile.getLearningStyle());
            memory.put("totalPracticeMinutes", profile.getTotalPracticeMinutes());
            memory.put("totalConversations", profile.getTotalConversations());
            memory.put("personalizedPrompt", profile.getPersonalizedPrompt());
            
            return memory;
        } catch (Exception e) {
            log.error("获取长期记忆失败", e);
            return new HashMap<>();
        }
    }

    @Override
    public List<Map<String, String>> buildPromptWithMemory(Long userId, String sessionId, 
                                                           String sessionType, String currentMessage, 
                                                           String rolePrompt) {
        log.debug("构建三层记忆提示词: userId={}, sessionId={}", userId, sessionId);
        
        List<Map<String, String>> messages = new ArrayList<>();
        
        // 1. 获取长期记忆(用户画像)
        Map<String, Object> longTermMemory = getLongTermMemory(userId);
        
        // 2. 获取中期记忆(对话总结)
        List<Map<String, Object>> midTermMemory = getMidTermMemory(userId, currentMessage, 3);
        
        // 3. 获取短期记忆(最近消息)
        List<Map<String, Object>> shortTermMemory = getShortTermMemory(sessionId, sessionType, 10);
        
        // 4. 构建系统提示词
        StringBuilder systemPrompt = new StringBuilder();
        
        // 基础角色设定
        if (rolePrompt != null && !rolePrompt.isEmpty()) {
            systemPrompt.append(rolePrompt).append("\n\n");
        } else {
            systemPrompt.append("你是一个专业的英语口语练习助手。\n\n");
        }
        
        // 添加长期记忆(用户画像)
        systemPrompt.append("=== 用户画像 ===\n");
        systemPrompt.append("英语水平: ").append(longTermMemory.get("englishLevel")).append("\n");
        systemPrompt.append("学习目标: ").append(longTermMemory.get("learningGoals")).append("\n");
        
        List<?> interests = (List<?>) longTermMemory.get("interests");
        if (interests != null && !interests.isEmpty()) {
            systemPrompt.append("兴趣话题: ").append(String.join(", ", 
                interests.stream().map(Object::toString).collect(Collectors.toList()))).append("\n");
        }
        
        List<?> mistakes = (List<?>) longTermMemory.get("commonMistakes");
        if (mistakes != null && !mistakes.isEmpty()) {
            systemPrompt.append("常见错误: ").append(String.join(", ", 
                mistakes.stream().map(Object::toString).collect(Collectors.toList()))).append("\n");
        }
        
        List<?> weaknesses = (List<?>) longTermMemory.get("pronunciationWeaknesses");
        if (weaknesses != null && !weaknesses.isEmpty()) {
            systemPrompt.append("发音弱点: ").append(String.join(", ", 
                weaknesses.stream().map(Object::toString).collect(Collectors.toList()))).append("\n");
        }
        
        String personalizedPrompt = (String) longTermMemory.get("personalizedPrompt");
        if (personalizedPrompt != null && !personalizedPrompt.isEmpty()) {
            systemPrompt.append("\n个性化指导: ").append(personalizedPrompt).append("\n");
        }
        
        // 添加中期记忆(历史对话总结)
        if (!midTermMemory.isEmpty()) {
            systemPrompt.append("\n=== 历史对话总结 ===\n");
            for (Map<String, Object> summary : midTermMemory) {
                systemPrompt.append("- ").append(summary.get("summary")).append("\n");
            }
        }
        
        systemPrompt.append("\n请根据以上用户画像和历史记录,提供个性化的英语口语指导。\n");
        
        // 添加系统消息
        Map<String, String> systemMsg = new HashMap<>();
        systemMsg.put("role", "system");
        systemMsg.put("content", systemPrompt.toString());
        messages.add(systemMsg);
        
        // 5. 添加短期记忆(最近对话)
        for (Map<String, Object> memory : shortTermMemory) {
            Map<String, String> msg = new HashMap<>();
            msg.put("role", (String) memory.get("role"));
            msg.put("content", (String) memory.get("content"));
            messages.add(msg);
        }
        
        // 6. 添加当前用户消息
        Map<String, String> userMsg = new HashMap<>();
        userMsg.put("role", "user");
        userMsg.put("content", currentMessage);
        messages.add(userMsg);
        
        log.debug("构建完成: 系统提示词长度={}, 历史消息数={}", 
                  systemPrompt.length(), shortTermMemory.size());
        
        return messages;
    }

    @Override
    public void saveMidTermMemory(Long userId, String sessionId, String sessionType, 
                                  String summary, String keywords, int messageCount) {
        log.debug("保存中期记忆: userId={}, sessionId={}", userId, sessionId);
        
        try {
            // 保存到MySQL
            ConversationSummary entity = new ConversationSummary();
            entity.setUserId(userId);
            entity.setSessionId(sessionId);
            entity.setSessionType(sessionType);
            entity.setSummary(summary);
            entity.setKeywords(keywords);
            entity.setMessageCount(messageCount);
            entity.setStartTime(LocalDateTime.now().minusMinutes(30)); // 估算
            entity.setEndTime(LocalDateTime.now());
            summaryMapper.insert(entity);
            
            // 同步到ES
            try {
                Map<String, Object> doc = new HashMap<>();
                doc.put("userId", userId);
                doc.put("sessionId", sessionId);
                doc.put("sessionType", sessionType);
                doc.put("summary", summary);
                doc.put("keywords", keywords);
                doc.put("messageCount", messageCount);
                doc.put("timestamp", LocalDateTime.now());
                
                esClient.index(i -> i
                    .index("conversation_summaries")
                    .id(entity.getId().toString())
                    .document(doc)
                );
                
                log.debug("中期记忆已同步到ES");
            } catch (Exception e) {
                log.warn("同步到ES失败,但MySQL已保存", e);
            }
        } catch (Exception e) {
            log.error("保存中期记忆失败", e);
        }
    }

    @Override
    public void updateLongTermMemory(Long userId, Map<String, Object> profileData) {
        log.debug("更新长期记忆: userId={}", userId);
        
        try {
            LambdaQueryWrapper<UserProfile> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(UserProfile::getUserId, userId)
                    .eq(UserProfile::getDeleted, 0);
            
            UserProfile profile = profileMapper.selectOne(wrapper);
            if (profile == null) {
                profile = new UserProfile();
                profile.setUserId(userId);
            }
            
            // 更新字段
            if (profileData.containsKey("englishLevel")) {
                profile.setEnglishLevel((String) profileData.get("englishLevel"));
            }
            if (profileData.containsKey("learningGoals")) {
                profile.setLearningGoals((String) profileData.get("learningGoals"));
            }
            if (profileData.containsKey("interests")) {
                profile.setInterests(toJson(profileData.get("interests")));
            }
            if (profileData.containsKey("commonMistakes")) {
                profile.setCommonMistakes(toJson(profileData.get("commonMistakes")));
            }
            if (profileData.containsKey("pronunciationWeaknesses")) {
                profile.setPronunciationWeaknesses(toJson(profileData.get("pronunciationWeaknesses")));
            }
            if (profileData.containsKey("learningStyle")) {
                profile.setLearningStyle((String) profileData.get("learningStyle"));
            }
            if (profileData.containsKey("personalizedPrompt")) {
                profile.setPersonalizedPrompt((String) profileData.get("personalizedPrompt"));
            }
            
            // 累加统计
            if (profileData.containsKey("practiceMinutes")) {
                int minutes = (Integer) profileData.get("practiceMinutes");
                profile.setTotalPracticeMinutes(
                    (profile.getTotalPracticeMinutes() != null ? profile.getTotalPracticeMinutes() : 0) + minutes
                );
            }
            if (profileData.containsKey("conversationCount")) {
                int count = (Integer) profileData.get("conversationCount");
                profile.setTotalConversations(
                    (profile.getTotalConversations() != null ? profile.getTotalConversations() : 0) + count
                );
            }
            
            if (profile.getId() == null) {
                profileMapper.insert(profile);
            } else {
                profileMapper.updateById(profile);
            }
            
            log.debug("长期记忆更新成功");
        } catch (Exception e) {
            log.error("更新长期记忆失败", e);
        }
    }

    @Override
    public String generateConversationSummary(String sessionId, String sessionType) {
        log.debug("生成对话总结: sessionId={}, type={}", sessionId, sessionType);
        
        try {
            // 获取会话的所有消息
            List<Map<String, Object>> messages = getShortTermMemory(sessionId, sessionType, 50);
            
            if (messages.isEmpty()) {
                return "无对话内容";
            }
            
            // 构建对话文本
            StringBuilder conversation = new StringBuilder();
            for (Map<String, Object> msg : messages) {
                String role = (String) msg.get("role");
                String content = (String) msg.get("content");
                conversation.append(role.equals("user") ? "用户: " : "AI: ")
                           .append(content).append("\n");
            }
            
            // 调用AI生成总结
            List<Map<String, String>> promptMessages = new ArrayList<>();
            Map<String, String> systemMsg = new HashMap<>();
            systemMsg.put("role", "system");
            systemMsg.put("content", "你是一个对话分析助手。请总结以下对话的主要内容、讨论的话题和用户的学习重点。用简洁的中文回复,不超过200字。");
            promptMessages.add(systemMsg);
            
            Map<String, String> userMsg = new HashMap<>();
            userMsg.put("role", "user");
            userMsg.put("content", "请总结以下对话:\n\n" + conversation.toString());
            promptMessages.add(userMsg);
            
            Map<String, Object> request = new HashMap<>();
            request.put("sessionId", sessionId + "-summary");
            request.put("messages", promptMessages);
            request.put("temperature", 0.3);
            request.put("maxTokens", 300);
            
            Map<String, Object> response = aiGatewayClient.chat(request);
            String summary = (String) response.get("content");
            
            log.debug("对话总结生成成功: {}", summary);
            return summary;
        } catch (Exception e) {
            log.error("生成对话总结失败", e);
            return "总结生成失败";
        }
    }

    /**
     * 解析JSON字符串
     */
    @SuppressWarnings("unchecked")
    private List<?> parseJson(String json) {
        try {
            if (json == null || json.isEmpty() || json.equals("[]")) {
                return new ArrayList<>();
            }
            return objectMapper.readValue(json, List.class);
        } catch (Exception e) {
            return new ArrayList<>();
        }
    }

    /**
     * 转换为JSON字符串
     */
    private String toJson(Object obj) {
        try {
            return objectMapper.writeValueAsString(obj);
        } catch (Exception e) {
            return "[]";
        }
    }
}
