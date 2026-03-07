package com.speakmaster.meeting.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.speakmaster.common.enums.ErrorCode;
import com.speakmaster.common.exception.BusinessException;
import com.speakmaster.meeting.dto.MeetingMessageDTO;
import com.speakmaster.meeting.entity.MeetingMessage;
import com.speakmaster.meeting.entity.MeetingParticipant;
import com.speakmaster.meeting.feign.AiGatewayClient;
import com.speakmaster.meeting.mapper.MeetingMessageMapper;
import com.speakmaster.meeting.mapper.MeetingParticipantMapper;
import com.speakmaster.meeting.netty.MeetingChannelHandler;
import com.speakmaster.meeting.service.IMeetingMessageService;
import com.speakmaster.meeting.service.IMemoryService;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

/**
 * Meeting消息服务实现
 * 
 * @author SpeakMaster
 */
@Slf4j
@Service
public class MeetingMessageServiceImpl implements IMeetingMessageService {

    private final MeetingMessageMapper messageMapper;
    private final MeetingParticipantMapper participantMapper;
    private final AiGatewayClient aiGatewayClient;
    private final MeetingChannelHandler channelHandler;
    private final IMemoryService memoryService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Autowired
    public MeetingMessageServiceImpl(
            MeetingMessageMapper messageMapper,
            MeetingParticipantMapper participantMapper,
            AiGatewayClient aiGatewayClient,
            @Lazy MeetingChannelHandler channelHandler,
            IMemoryService memoryService) {
        this.messageMapper = messageMapper;
        this.participantMapper = participantMapper;
        this.aiGatewayClient = aiGatewayClient;
        this.channelHandler = channelHandler;
        this.memoryService = memoryService;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public MeetingMessageDTO sendMessage(Long meetingId, MeetingMessageDTO messageDTO, Long userId) {
        // 检查用户是否在Meeting中
        LambdaQueryWrapper<MeetingParticipant> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(MeetingParticipant::getMeetingId, meetingId)
                .eq(MeetingParticipant::getUserId, userId)
                .eq(MeetingParticipant::getDeleted, 0);
        
        if (participantMapper.selectCount(wrapper) == 0) {
            throw new BusinessException(ErrorCode.NOT_IN_MEETING);
        }

        MeetingMessage message = new MeetingMessage();
        message.setMeetingId(meetingId);
        message.setSenderId(userId);
        message.setMessageType(messageDTO.getMessageType() != null ? messageDTO.getMessageType() : 0);
        message.setContent(messageDTO.getContent());
        message.setAudioUrl(messageDTO.getAudioUrl());

        messageMapper.insert(message);
        log.info("发送Meeting消息: meetingId={}, senderId={}", meetingId, userId);
        
        MeetingMessageDTO savedMessage = convertToDTO(message);
        
        // 异步触发AI回复
        triggerAiResponses(meetingId, savedMessage);
        
        return savedMessage;
    }

    /**
     * 触发AI参与者自动回复
     */
    @Async
    public void triggerAiResponses(Long meetingId, MeetingMessageDTO userMessage) {
        try {
            // 查找所有AI参与者
            LambdaQueryWrapper<MeetingParticipant> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(MeetingParticipant::getMeetingId, meetingId)
                    .lt(MeetingParticipant::getUserId, 0) // AI参与者userId < 0
                    .eq(MeetingParticipant::getDeleted, 0);
            
            List<MeetingParticipant> aiParticipants = participantMapper.selectList(wrapper);
            
            if (aiParticipants.isEmpty()) {
                return; // 没有AI参与者
            }
            
            // 获取最近的对话历史
            List<MeetingMessage> recentMessages = getRecentMessages(meetingId, 10);
            
            // 为每个AI生成回复
            for (MeetingParticipant aiParticipant : aiParticipants) {
                try {
                    String aiResponse = callAiGateway(aiParticipant, recentMessages);
                    
                    if (aiResponse != null && !aiResponse.isEmpty()) {
                        // 保存AI回复
                        MeetingMessage aiMessage = new MeetingMessage();
                        aiMessage.setMeetingId(meetingId);
                        aiMessage.setSenderId(aiParticipant.getUserId());
                        aiMessage.setMessageType(0); // 文本消息
                        aiMessage.setContent(aiResponse);
                        messageMapper.insert(aiMessage);
                        
                        // 通过WebSocket广播AI回复
                        MeetingMessageDTO aiMessageDTO = convertToDTO(aiMessage);
                        aiMessageDTO.setSenderName(aiParticipant.getAiRoleName());
                        broadcastMessage(meetingId, aiMessageDTO);
                        
                        log.info("AI回复成功: meetingId={}, aiName={}", meetingId, aiParticipant.getAiRoleName());
                    }
                } catch (Exception e) {
                    log.error("AI回复失败: meetingId={}, aiName={}", meetingId, aiParticipant.getAiRoleName(), e);
                }
            }
        } catch (Exception e) {
            log.error("触发AI回复失败: meetingId={}", meetingId, e);
        }
    }

    /**
     * 调用AI Gateway获取回复(使用三层记忆系统)
     */
    private String callAiGateway(MeetingParticipant aiParticipant, List<MeetingMessage> recentMessages) {
        try {
            // 获取最后一条用户消息
            String lastUserMessage = "";
            Long userId = null;
            for (int i = recentMessages.size() - 1; i >= 0; i--) {
                MeetingMessage msg = recentMessages.get(i);
                if (msg.getSenderId() > 0) { // 用户消息
                    lastUserMessage = msg.getContent();
                    userId = msg.getSenderId();
                    break;
                }
            }
            
            if (userId == null || lastUserMessage.isEmpty()) {
                log.warn("未找到用户消息,使用简单模式");
                return callAiGatewaySimple(aiParticipant, recentMessages);
            }
            
            // 使用三层记忆系统构建提示词
            String sessionId = "meeting-" + aiParticipant.getMeetingId();
            List<Map<String, String>> messages = memoryService.buildPromptWithMemory(
                userId, 
                sessionId, 
                "meeting", 
                lastUserMessage, 
                aiParticipant.getAiRoleSetting()
            );
            
            // 构建请求
            Map<String, Object> request = new HashMap<>();
            request.put("sessionId", sessionId);
            request.put("messages", messages);
            request.put("temperature", 0.7);
            request.put("maxTokens", 500);
            
            // 调用AI Gateway
            Map<String, Object> response = aiGatewayClient.chat(request);
            
            // 提取回复内容
            if (response != null && response.containsKey("content")) {
                return (String) response.get("content");
            }
            
            return null;
        } catch (Exception e) {
            log.error("调用AI Gateway失败", e);
            return null;
        }
    }
    
    /**
     * 简单模式调用AI(不使用记忆系统)
     */
    private String callAiGatewaySimple(MeetingParticipant aiParticipant, List<MeetingMessage> recentMessages) {
        try {
            List<Map<String, String>> messages = new ArrayList<>();
            
            // 添加系统提示(AI角色设定)
            if (aiParticipant.getAiRoleSetting() != null && !aiParticipant.getAiRoleSetting().isEmpty()) {
                Map<String, String> systemMsg = new HashMap<>();
                systemMsg.put("role", "system");
                systemMsg.put("content", aiParticipant.getAiRoleSetting());
                messages.add(systemMsg);
            }
            
            // 添加历史对话
            for (MeetingMessage msg : recentMessages) {
                Map<String, String> historyMsg = new HashMap<>();
                historyMsg.put("role", msg.getSenderId() < 0 ? "assistant" : "user");
                historyMsg.put("content", msg.getContent());
                messages.add(historyMsg);
            }
            
            Map<String, Object> request = new HashMap<>();
            request.put("sessionId", "meeting-" + aiParticipant.getMeetingId());
            request.put("messages", messages);
            request.put("temperature", 0.7);
            request.put("maxTokens", 500);
            
            Map<String, Object> response = aiGatewayClient.chat(request);
            
            if (response != null && response.containsKey("content")) {
                return (String) response.get("content");
            }
            
            return null;
        } catch (Exception e) {
            log.error("简单模式调用AI Gateway失败", e);
            return null;
        }
    }

    /**
     * 获取最近的消息历史
     */
    private List<MeetingMessage> getRecentMessages(Long meetingId, int limit) {
        LambdaQueryWrapper<MeetingMessage> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(MeetingMessage::getMeetingId, meetingId)
                .eq(MeetingMessage::getDeleted, 0)
                .orderByDesc(MeetingMessage::getCreateTime)
                .last("LIMIT " + limit);
        
        List<MeetingMessage> messages = messageMapper.selectList(wrapper);
        Collections.reverse(messages); // 反转为时间正序
        return messages;
    }

    /**
     * 通过WebSocket广播消息
     */
    private void broadcastMessage(Long meetingId, MeetingMessageDTO message) {
        try {
            String payload = objectMapper.writeValueAsString(message);
            channelHandler.broadcastMessage(meetingId, payload);
            log.debug("广播AI消息: meetingId={}, content={}", meetingId, message.getContent());
        } catch (Exception e) {
            log.error("广播消息失败", e);
        }
    }

    @Override
    public Page<MeetingMessageDTO> getMessages(Long meetingId, Long userId, int page, int size) {
        // 检查用户是否在Meeting中
        LambdaQueryWrapper<MeetingParticipant> participantWrapper = new LambdaQueryWrapper<>();
        participantWrapper.eq(MeetingParticipant::getMeetingId, meetingId)
                .eq(MeetingParticipant::getUserId, userId)
                .eq(MeetingParticipant::getDeleted, 0);
        
        if (participantMapper.selectCount(participantWrapper) == 0) {
            throw new BusinessException(ErrorCode.NOT_IN_MEETING);
        }

        Page<MeetingMessage> messagePage = new Page<>(page, size);
        LambdaQueryWrapper<MeetingMessage> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(MeetingMessage::getMeetingId, meetingId)
                .eq(MeetingMessage::getDeleted, 0)
                .orderByAsc(MeetingMessage::getCreateTime);
        
        Page<MeetingMessage> result = messageMapper.selectPage(messagePage, wrapper);
        return (Page<MeetingMessageDTO>) result.convert(this::convertToDTO);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteMessage(Long messageId, Long userId) {
        MeetingMessage message = messageMapper.selectById(messageId);
        if (message == null || message.getDeleted() == 1) {
            throw new BusinessException(ErrorCode.MESSAGE_NOT_FOUND);
        }

        // 检查权限
        if (!message.getSenderId().equals(userId)) {
            throw new BusinessException(ErrorCode.NO_PERMISSION);
        }

        message.markDeleted();
        messageMapper.updateById(message);
        log.info("删除Meeting消息: messageId={}", messageId);
    }

    /**
     * 转换为DTO
     */
    private MeetingMessageDTO convertToDTO(MeetingMessage message) {
        MeetingMessageDTO dto = new MeetingMessageDTO();
        dto.setId(message.getId());
        dto.setMeetingId(message.getMeetingId());
        dto.setSenderId(message.getSenderId());
        dto.setMessageType(message.getMessageType());
        dto.setContent(message.getContent());
        dto.setAudioUrl(message.getAudioUrl());
        dto.setCreateTime(message.getCreateTime() != null ? message.getCreateTime().toString() : null);
        return dto;
    }
}
