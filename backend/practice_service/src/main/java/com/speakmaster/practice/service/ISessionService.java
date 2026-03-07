package com.speakmaster.practice.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.speakmaster.practice.dto.SessionDTO;
import com.speakmaster.practice.entity.Session;

/**
 * 练习会话服务接口
 * 
 * @author SpeakMaster
 */
public interface ISessionService extends IService<Session> {

    /**
     * 创建练习会话
     */
    SessionDTO createSession(SessionDTO sessionDTO, Long userId);

    /**
     * 获取用户会话列表
     */
    Page<SessionDTO> getUserSessions(Long userId, int page, int size);

    /**
     * 获取会话详情
     */
    SessionDTO getSessionById(Long sessionId, Long userId);

    /**
     * 结束会话
     */
    SessionDTO endSession(Long sessionId, Long userId, Integer score, String feedback);

    /**
     * 删除会话
     */
    void deleteSession(Long sessionId, Long userId);
}
