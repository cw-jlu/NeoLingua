package com.speakmaster.meeting.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.speakmaster.meeting.dto.MeetingMessageDTO;

/**
 * Meeting消息服务接口
 * 
 * @author SpeakMaster
 */
public interface IMeetingMessageService {

    /**
     * 发送消息
     */
    MeetingMessageDTO sendMessage(Long meetingId, MeetingMessageDTO messageDTO, Long userId);

    /**
     * 获取消息列表
     */
    Page<MeetingMessageDTO> getMessages(Long meetingId, Long userId, int page, int size);

    /**
     * 删除消息
     */
    void deleteMessage(Long messageId, Long userId);
}
