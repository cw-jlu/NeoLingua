package com.speakmaster.practice.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.speakmaster.common.enums.ErrorCode;
import com.speakmaster.common.exception.BusinessException;
import com.speakmaster.practice.dto.SessionDTO;
import com.speakmaster.practice.entity.Session;
import com.speakmaster.practice.mapper.SessionMapper;
import com.speakmaster.practice.service.ISessionService;
import com.speakmaster.practice.service.IThemeService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * 练习会话服务实现类
 * 
 * @author SpeakMaster
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SessionServiceImpl extends ServiceImpl<SessionMapper, Session> implements ISessionService {

    private final IThemeService themeService;
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @Override
    @Transactional(rollbackFor = Exception.class)
    public SessionDTO createSession(SessionDTO sessionDTO, Long userId) {
        // 验证主题是否存在
        themeService.getThemeById(sessionDTO.getThemeId());

        Session session = new Session();
        session.setUserId(userId);
        session.setThemeId(sessionDTO.getThemeId());
        session.setRoleId(sessionDTO.getRoleId());
        session.setStatus(0); // 进行中
        session.setStartTime(LocalDateTime.now().format(FORMATTER));

        this.save(session);
        
        // 增加主题使用次数
        themeService.incrementUseCount(sessionDTO.getThemeId());
        
        log.info("创建练习会话成功: sessionId={}, userId={}", session.getId(), userId);
        return convertToDTO(session);
    }

    @Override
    public Page<SessionDTO> getUserSessions(Long userId, int page, int size) {
        LambdaQueryWrapper<Session> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Session::getUserId, userId)
                .eq(Session::getDeleted, 0)
                .orderByDesc(Session::getCreateTime);
        
        Page<Session> pageRequest = new Page<>(page + 1, size);
        Page<Session> sessions = this.page(pageRequest, wrapper);
        
        return (Page<SessionDTO>) sessions.convert(this::convertToDTO);
    }

    @Override
    public SessionDTO getSessionById(Long sessionId, Long userId) {
        LambdaQueryWrapper<Session> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Session::getId, sessionId)
                .eq(Session::getDeleted, 0);
        Session session = this.getOne(wrapper);
        
        if (session == null) {
            throw new BusinessException(ErrorCode.SESSION_NOT_FOUND);
        }

        // 检查权限
        if (!session.getUserId().equals(userId)) {
            throw new BusinessException(ErrorCode.NO_PERMISSION);
        }

        return convertToDTO(session);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public SessionDTO endSession(Long sessionId, Long userId, Integer score, String feedback) {
        LambdaQueryWrapper<Session> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Session::getId, sessionId)
                .eq(Session::getDeleted, 0);
        Session session = this.getOne(wrapper);
        
        if (session == null) {
            throw new BusinessException(ErrorCode.SESSION_NOT_FOUND);
        }

        // 检查权限
        if (!session.getUserId().equals(userId)) {
            throw new BusinessException(ErrorCode.NO_PERMISSION);
        }

        // 检查状态
        if (session.getStatus() != 0) {
            throw new BusinessException(ErrorCode.SESSION_ALREADY_ENDED);
        }

        session.setStatus(1); // 已结束
        session.setEndTime(LocalDateTime.now().format(FORMATTER));

        this.updateById(session);
        log.info("结束练习会话: sessionId={}", sessionId);
        return convertToDTO(session);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteSession(Long sessionId, Long userId) {
        LambdaQueryWrapper<Session> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Session::getId, sessionId)
                .eq(Session::getDeleted, 0);
        Session session = this.getOne(wrapper);
        
        if (session == null) {
            throw new BusinessException(ErrorCode.SESSION_NOT_FOUND);
        }

        // 检查权限
        if (!session.getUserId().equals(userId)) {
            throw new BusinessException(ErrorCode.NO_PERMISSION);
        }

        session.markDeleted();
        this.updateById(session);
        log.info("删除练习会话: sessionId={}", sessionId);
    }

    /**
     * 转换为DTO
     */
    private SessionDTO convertToDTO(Session session) {
        SessionDTO dto = new SessionDTO();
        dto.setId(session.getId());
        dto.setUserId(session.getUserId());
        dto.setThemeId(session.getThemeId());
        dto.setRoleId(session.getRoleId());
        dto.setStatus(session.getStatus());
        dto.setStartTime(session.getStartTime());
        dto.setEndTime(session.getEndTime());
        // 查询主题名称
        if (session.getThemeId() != null) {
            try {
                var theme = themeService.getThemeById(session.getThemeId());
                dto.setThemeName(theme != null ? theme.getName() : null);
            } catch (Exception ignored) {}
        }
        // 创建时间
        if (session.getCreateTime() != null) {
            dto.setCreateTime(session.getCreateTime().format(FORMATTER));
        }
        return dto;
    }
}
