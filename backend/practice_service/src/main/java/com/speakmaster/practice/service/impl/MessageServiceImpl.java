package com.speakmaster.practice.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.speakmaster.common.enums.ErrorCode;
import com.speakmaster.common.exception.BusinessException;
import com.speakmaster.practice.dto.MessageDTO;
import com.speakmaster.practice.dto.RoleDTO;
import com.speakmaster.practice.dto.SessionDTO;
import com.speakmaster.practice.dto.ThemeDTO;
import com.speakmaster.practice.entity.Message;
import com.speakmaster.practice.entity.Session;
import com.speakmaster.practice.feign.AIServiceClient;
import com.speakmaster.practice.mapper.MessageMapper;
import com.speakmaster.practice.mapper.SessionMapper;
import com.speakmaster.practice.service.IMessageService;
import com.speakmaster.practice.service.IRoleService;
import com.speakmaster.practice.service.ISessionService;
import com.speakmaster.practice.service.IThemeService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 消息服务实现类
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MessageServiceImpl extends ServiceImpl<MessageMapper, Message> implements IMessageService {

    private final ISessionService sessionService;
    private final IRoleService roleService;
    private final IThemeService themeService;
    private final SessionMapper sessionMapper;
    private final AIServiceClient aiServiceClient;
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper = new ObjectMapper();

    private static final String KAFKA_TOPIC_DIALOGUE = "stream.dialogue.text";

    @Override
    @Transactional(rollbackFor = Exception.class)
    public MessageDTO sendMessage(Long sessionId, MessageDTO messageDTO, Long userId) {
        // 验证会话
        SessionDTO sessionDTO = sessionService.getSessionById(sessionId, userId);

        if (sessionDTO.getStatus() != 0) {
            throw new BusinessException(ErrorCode.SESSION_ALREADY_ENDED);
        }

        // 保存用户消息（content 是 STT 识别后的文字，audioUrl 是 MinIO 地址）
        Message userMessage = new Message();
        userMessage.setSessionId(sessionId);
        userMessage.setSenderType(1);
        userMessage.setContent(messageDTO.getContent());
        userMessage.setAudioUrl(messageDTO.getAudioUrl());
        this.save(userMessage);

        log.info("用户消息保存: messageId={}, sessionId={}, hasAudio={}",
                userMessage.getId(), sessionId, messageDTO.getAudioUrl() != null);

        // 异步发送消息到 Kafka，触发 analysis_service 做语法/发音分析
        try {
            java.util.Map<String, Object> kafkaMsg = new java.util.HashMap<>();
            kafkaMsg.put("session_id", sessionId);
            kafkaMsg.put("user_id", userId);
            kafkaMsg.put("text", messageDTO.getContent());
            kafkaMsg.put("role", "user");
            java.util.Map<String, Object> metadata = new java.util.HashMap<>();
            metadata.put("message_id", userMessage.getId());
            if (messageDTO.getAudioUrl() != null) {
                metadata.put("audio_url", messageDTO.getAudioUrl());
            }
            kafkaMsg.put("metadata", metadata);
            kafkaTemplate.send(KAFKA_TOPIC_DIALOGUE, String.valueOf(sessionId),
                    objectMapper.writeValueAsString(kafkaMsg));
            log.debug("Kafka消息已发送: topic={}, sessionId={}", KAFKA_TOPIC_DIALOGUE, sessionId);
        } catch (Exception e) {
            log.warn("Kafka消息发送失败（不影响主流程）: {}", e.getMessage());
        }

        // 查角色设定和主题名（用于 AI 上下文）
        String rolePrompt = null;
        String themeName = null;
        Session session = sessionMapper.selectById(sessionId);
        if (session != null) {
            if (session.getRoleId() != null) {
                try {
                    RoleDTO role = roleService.getRoleById(session.getRoleId());
                    rolePrompt = role.getPrompt();
                } catch (Exception e) {
                    log.warn("查询角色设定失败: roleId={}", session.getRoleId());
                }
            }
            if (session.getThemeId() != null) {
                try {
                    ThemeDTO theme = themeService.getThemeById(session.getThemeId());
                    themeName = theme != null ? theme.getName() : null;
                } catch (Exception e) {
                    log.warn("查询主题失败: themeId={}", session.getThemeId());
                }
            }
        }

        // 调用 AI 服务
        try {
            java.util.Map<String, Object> request = new java.util.HashMap<>();
            request.put("session_id", String.valueOf(sessionId));
            request.put("user_id", String.valueOf(userId));
            request.put("message", messageDTO.getContent());
            if (messageDTO.getAudioUrl() != null) {
                request.put("audio_url", messageDTO.getAudioUrl());
            }
            if (rolePrompt != null) {
                request.put("role_prompt", rolePrompt);
            }
            if (themeName != null) {
                request.put("theme", themeName);
            }

            com.speakmaster.common.dto.Result<java.util.Map<String, Object>> result = aiServiceClient.chat(request);
            String aiResponse = (String) result.getData().get("reply");

            // 保存 AI 回复
            Message aiMessage = new Message();
            aiMessage.setSessionId(sessionId);
            aiMessage.setSenderType(2);
            aiMessage.setContent(aiResponse);
            this.save(aiMessage);

            log.info("AI回复保存: messageId={}, sessionId={}", aiMessage.getId(), sessionId);
            return convertToDTO(aiMessage);
        } catch (Exception e) {
            log.error("调用AI服务失败: sessionId={}, error={}", sessionId, e.getMessage());
            throw new BusinessException(ErrorCode.AI_SERVICE_ERROR);
        }
    }

    @Override
    public Page<MessageDTO> getSessionMessages(Long sessionId, Long userId, int page, int size) {
        sessionService.getSessionById(sessionId, userId);

        LambdaQueryWrapper<Message> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Message::getSessionId, sessionId)
                .eq(Message::getDeleted, 0)
                .orderByAsc(Message::getCreateTime);

        Page<Message> pageRequest = new Page<>(page + 1, size);
        Page<Message> messages = this.page(pageRequest, wrapper);

        return (Page<MessageDTO>) messages.convert(this::convertToDTO);
    }

    private MessageDTO convertToDTO(Message message) {
        MessageDTO dto = new MessageDTO();
        dto.setId(message.getId());
        dto.setSessionId(message.getSessionId());
        dto.setSender(message.getSenderType() == 1 ? "user" : "ai");
        dto.setContent(message.getContent());
        dto.setAudioUrl(message.getAudioUrl());
        dto.setCreateTime(message.getCreateTime() != null ? message.getCreateTime().toString() : null);
        return dto;
    }
}
