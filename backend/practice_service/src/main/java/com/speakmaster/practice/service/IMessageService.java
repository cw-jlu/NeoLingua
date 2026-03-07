package com.speakmaster.practice.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.speakmaster.practice.dto.MessageDTO;
import com.speakmaster.practice.entity.Message;

/**
 * 消息服务接口
 * 
 * @author SpeakMaster
 */
public interface IMessageService extends IService<Message> {

    /**
     * 发送消息
     */
    MessageDTO sendMessage(Long sessionId, MessageDTO messageDTO, Long userId);

    /**
     * 获取会话消息列表
     */
    Page<MessageDTO> getSessionMessages(Long sessionId, Long userId, int page, int size);
}
