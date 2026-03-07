package com.speakmaster.meeting.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.speakmaster.common.enums.ErrorCode;
import com.speakmaster.common.exception.BusinessException;
import com.speakmaster.common.utils.RedisUtil;
import com.speakmaster.meeting.dto.MeetingDTO;
import com.speakmaster.meeting.entity.Meeting;
import com.speakmaster.meeting.entity.MeetingParticipant;
import com.speakmaster.meeting.mapper.MeetingMapper;
import com.speakmaster.meeting.mapper.MeetingParticipantMapper;
import com.speakmaster.meeting.service.IMeetingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Meeting服务实现
 * 
 * @author SpeakMaster
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MeetingServiceImpl implements IMeetingService {

    private final MeetingMapper meetingMapper;
    private final MeetingParticipantMapper participantMapper;
    private final RedisUtil redisUtil;
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @Override
    @Transactional(rollbackFor = Exception.class)
    public MeetingDTO createMeeting(MeetingDTO meetingDTO, Long userId) {
        Meeting meeting = new Meeting();
        // 如果name为空，生成默认名称
        String name = meetingDTO.getName();
        if (name == null || name.trim().isEmpty()) {
            name = "Meeting-" + System.currentTimeMillis();
        }
        meeting.setName(name);
        meeting.setDescription(meetingDTO.getDescription());
        meeting.setCreatorId(userId);
        meeting.setThemeId(meetingDTO.getThemeId());
        meeting.setMaxParticipants(meetingDTO.getMaxParticipants() != null ? meetingDTO.getMaxParticipants() : 4);
        meeting.setCurrentParticipants(1);
        meeting.setStatus(0);
        meeting.setIsPublic(meetingDTO.getIsPublic() != null ? meetingDTO.getIsPublic() : 0);

        meetingMapper.insert(meeting);

        // 创建者自动加入
        MeetingParticipant participant = new MeetingParticipant();
        participant.setMeetingId(meeting.getId());
        participant.setUserId(userId);
        participant.setRole(1);
        participant.setJoinTime(LocalDateTime.now().format(FORMATTER));
        participant.setStatus(0);
        participantMapper.insert(participant);

        // 缓存Meeting状态到Redis
        String key = "meeting:" + meeting.getId();
        redisUtil.set(key, meeting.getStatus().toString(), 3600);

        log.info("创建Meeting成功: meetingId={}, creatorId={}", meeting.getId(), userId);
        return convertToDTO(meeting);
    }

    @Override
    public Page<MeetingDTO> getMeetingList(Long userId, int page, int size) {
        Page<Meeting> meetingPage = new Page<>(page, size);
        LambdaQueryWrapper<Meeting> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Meeting::getCreatorId, userId)
                .eq(Meeting::getDeleted, 0)
                .orderByDesc(Meeting::getCreateTime);
        
        Page<Meeting> result = meetingMapper.selectPage(meetingPage, wrapper);
        return (Page<MeetingDTO>) result.convert(this::convertToDTO);
    }

    @Override
    public MeetingDTO getMeetingById(Long meetingId) {
        Meeting meeting = meetingMapper.selectById(meetingId);
        if (meeting == null || meeting.getDeleted() == 1) {
            throw new BusinessException(ErrorCode.MEETING_NOT_FOUND);
        }
        return convertToDTO(meeting);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void joinMeeting(Long meetingId, Long userId) {
        Meeting meeting = meetingMapper.selectById(meetingId);
        if (meeting == null || meeting.getDeleted() == 1) {
            throw new BusinessException(ErrorCode.MEETING_NOT_FOUND);
        }

        // 检查Meeting状态
        if (meeting.getStatus() == 2) {
            throw new BusinessException(ErrorCode.MEETING_ALREADY_ENDED);
        }

        // 检查是否已在Meeting中
        LambdaQueryWrapper<MeetingParticipant> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(MeetingParticipant::getMeetingId, meetingId)
                .eq(MeetingParticipant::getUserId, userId)
                .eq(MeetingParticipant::getDeleted, 0);
        
        if (participantMapper.selectCount(wrapper) > 0) {
            throw new BusinessException(ErrorCode.ALREADY_IN_MEETING);
        }

        // 检查人数限制
        if (meeting.getCurrentParticipants() >= meeting.getMaxParticipants()) {
            throw new BusinessException(ErrorCode.MEETING_FULL);
        }

        // 添加参与者
        MeetingParticipant participant = new MeetingParticipant();
        participant.setMeetingId(meetingId);
        participant.setUserId(userId);
        participant.setRole(0);
        participant.setJoinTime(LocalDateTime.now().format(FORMATTER));
        participant.setStatus(0);
        participantMapper.insert(participant);

        // 更新当前人数
        meeting.setCurrentParticipants(meeting.getCurrentParticipants() + 1);
        meetingMapper.updateById(meeting);

        log.info("用户加入Meeting: meetingId={}, userId={}", meetingId, userId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void leaveMeeting(Long meetingId, Long userId) {
        Meeting meeting = meetingMapper.selectById(meetingId);
        if (meeting == null || meeting.getDeleted() == 1) {
            throw new BusinessException(ErrorCode.MEETING_NOT_FOUND);
        }

        LambdaQueryWrapper<MeetingParticipant> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(MeetingParticipant::getMeetingId, meetingId)
                .eq(MeetingParticipant::getUserId, userId)
                .eq(MeetingParticipant::getDeleted, 0);
        
        MeetingParticipant participant = participantMapper.selectOne(wrapper);
        if (participant == null) {
            throw new BusinessException(ErrorCode.NOT_IN_MEETING);
        }

        // 更新离开时间和状态
        participant.setLeaveTime(LocalDateTime.now().format(FORMATTER));
        participant.setStatus(1);
        participantMapper.updateById(participant);

        // 更新当前人数
        meeting.setCurrentParticipants(meeting.getCurrentParticipants() - 1);
        meetingMapper.updateById(meeting);

        log.info("用户离开Meeting: meetingId={}, userId={}", meetingId, userId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public MeetingDTO startMeeting(Long meetingId, Long userId) {
        Meeting meeting = meetingMapper.selectById(meetingId);
        if (meeting == null || meeting.getDeleted() == 1) {
            throw new BusinessException(ErrorCode.MEETING_NOT_FOUND);
        }

        // 检查权限
        if (!meeting.getCreatorId().equals(userId)) {
            throw new BusinessException(ErrorCode.NO_PERMISSION);
        }

        // 检查状态
        if (meeting.getStatus() != 0) {
            throw new BusinessException(ErrorCode.MEETING_ALREADY_STARTED);
        }

        meeting.setStatus(1);
        meeting.setStartTime(LocalDateTime.now().format(FORMATTER));
        meetingMapper.updateById(meeting);

        // 更新Redis缓存
        String key = "meeting:" + meetingId;
        redisUtil.set(key, "1", 3600);

        log.info("开始Meeting: meetingId={}", meetingId);
        return convertToDTO(meeting);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public MeetingDTO endMeeting(Long meetingId, Long userId) {
        Meeting meeting = meetingMapper.selectById(meetingId);
        if (meeting == null || meeting.getDeleted() == 1) {
            throw new BusinessException(ErrorCode.MEETING_NOT_FOUND);
        }

        // 检查权限
        if (!meeting.getCreatorId().equals(userId)) {
            throw new BusinessException(ErrorCode.NO_PERMISSION);
        }

        meeting.setStatus(2);
        meeting.setEndTime(LocalDateTime.now().format(FORMATTER));
        meetingMapper.updateById(meeting);

        // 删除Redis缓存
        String key = "meeting:" + meetingId;
        redisUtil.delete(key);

        log.info("结束Meeting: meetingId={}", meetingId);
        return convertToDTO(meeting);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteMeeting(Long meetingId, Long userId) {
        Meeting meeting = meetingMapper.selectById(meetingId);
        if (meeting == null || meeting.getDeleted() == 1) {
            throw new BusinessException(ErrorCode.MEETING_NOT_FOUND);
        }

        // 检查权限
        if (!meeting.getCreatorId().equals(userId)) {
            throw new BusinessException(ErrorCode.NO_PERMISSION);
        }

        meeting.markDeleted();
        meetingMapper.updateById(meeting);
        log.info("删除Meeting: meetingId={}", meetingId);
    }

    @Override
    public Page<MeetingDTO> adminGetMeetingList(Integer status, int page, int size) {
        Page<Meeting> meetingPage = new Page<>(page, size);
        LambdaQueryWrapper<Meeting> wrapper = new LambdaQueryWrapper<>();
        
        if (status != null) {
            wrapper.eq(Meeting::getStatus, status);
        }
        wrapper.eq(Meeting::getDeleted, 0)
                .orderByDesc(Meeting::getCreateTime);
        
        Page<Meeting> result = meetingMapper.selectPage(meetingPage, wrapper);
        return (Page<MeetingDTO>) result.convert(this::convertToDTO);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void adminDeleteMeeting(Long meetingId) {
        Meeting meeting = meetingMapper.selectById(meetingId);
        if (meeting == null || meeting.getDeleted() == 1) {
            throw new BusinessException(ErrorCode.MEETING_NOT_FOUND);
        }
        meeting.markDeleted();
        meetingMapper.updateById(meeting);
        redisUtil.delete("meeting:" + meetingId);
        log.info("[管理端] 删除Meeting: meetingId={}", meetingId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public MeetingDTO adminCloseMeeting(Long meetingId) {
        Meeting meeting = meetingMapper.selectById(meetingId);
        if (meeting == null || meeting.getDeleted() == 1) {
            throw new BusinessException(ErrorCode.MEETING_NOT_FOUND);
        }
        meeting.setStatus(2);
        meeting.setEndTime(LocalDateTime.now().format(FORMATTER));
        meetingMapper.updateById(meeting);
        redisUtil.delete("meeting:" + meetingId);
        log.info("[管理端] 强制关闭Meeting: meetingId={}", meetingId);
        return convertToDTO(meeting);
    }

    @Override
    public Map<String, Object> adminGetMeetingStatistics() {
        Map<String, Object> stats = new HashMap<>();
        
        // 总数
        stats.put("totalMeetings", meetingMapper.selectCount(null));
        
        // 等待中
        LambdaQueryWrapper<Meeting> waitingWrapper = new LambdaQueryWrapper<>();
        waitingWrapper.eq(Meeting::getStatus, 0).eq(Meeting::getDeleted, 0);
        stats.put("waitingMeetings", meetingMapper.selectCount(waitingWrapper));
        
        // 进行中
        LambdaQueryWrapper<Meeting> activeWrapper = new LambdaQueryWrapper<>();
        activeWrapper.eq(Meeting::getStatus, 1).eq(Meeting::getDeleted, 0);
        stats.put("activeMeetings", meetingMapper.selectCount(activeWrapper));
        
        // 已结束
        LambdaQueryWrapper<Meeting> endedWrapper = new LambdaQueryWrapper<>();
        endedWrapper.eq(Meeting::getStatus, 2).eq(Meeting::getDeleted, 0);
        stats.put("endedMeetings", meetingMapper.selectCount(endedWrapper));
        
        return stats;
    }

    /**
     * 转换为DTO
     */
    private MeetingDTO convertToDTO(Meeting meeting) {
        MeetingDTO dto = new MeetingDTO();
        dto.setId(meeting.getId());
        dto.setName(meeting.getName());
        dto.setDescription(meeting.getDescription());
        dto.setCreatorId(meeting.getCreatorId());
        dto.setThemeId(meeting.getThemeId());
        dto.setMaxParticipants(meeting.getMaxParticipants());
        dto.setCurrentParticipants(meeting.getCurrentParticipants());
        dto.setStatus(meeting.getStatus());
        dto.setStartTime(meeting.getStartTime());
        dto.setEndTime(meeting.getEndTime());
        dto.setIsPublic(meeting.getIsPublic());
        
        // 查询参与者列表
        LambdaQueryWrapper<MeetingParticipant> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(MeetingParticipant::getMeetingId, meeting.getId())
                .eq(MeetingParticipant::getDeleted, 0);
        List<MeetingParticipant> participants = participantMapper.selectList(wrapper);
        
        // 转换为 ParticipantInfo
        List<MeetingDTO.ParticipantInfo> participantInfos = participants.stream()
                .map(p -> {
                    MeetingDTO.ParticipantInfo info = new MeetingDTO.ParticipantInfo();
                    info.setId(p.getId());
                    info.setUserId(p.getUserId());
                    // 如果userId为负数,表示AI参与者
                    if (p.getUserId() < 0) {
                        info.setUsername(p.getAiRoleName() != null ? p.getAiRoleName() : ("AI助手" + Math.abs(p.getUserId())));
                        info.setAiRoleName(p.getAiRoleName());
                        info.setAiRoleSetting(p.getAiRoleSetting());
                    } else {
                        info.setUsername("用户" + p.getUserId()); // TODO: 从用户服务获取真实用户名
                    }
                    info.setRole(p.getRole());
                    info.setJoinTime(p.getJoinTime());
                    return info;
                })
                .collect(Collectors.toList());
        dto.setParticipants(participantInfos);
        
        return dto;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void inviteFriend(Long meetingId, Long friendId, Long inviterId) {
        // 检查Meeting是否存在
        Meeting meeting = meetingMapper.selectById(meetingId);
        if (meeting == null || meeting.getDeleted() == 1) {
            throw new BusinessException(ErrorCode.MEETING_NOT_FOUND);
        }

        // 检查邀请者是否是创建者或已在Meeting中
        LambdaQueryWrapper<MeetingParticipant> inviterWrapper = new LambdaQueryWrapper<>();
        inviterWrapper.eq(MeetingParticipant::getMeetingId, meetingId)
                .eq(MeetingParticipant::getUserId, inviterId)
                .eq(MeetingParticipant::getDeleted, 0);
        
        if (participantMapper.selectCount(inviterWrapper) == 0) {
            throw new BusinessException(ErrorCode.NO_PERMISSION);
        }

        // 检查好友是否已在Meeting中
        LambdaQueryWrapper<MeetingParticipant> friendWrapper = new LambdaQueryWrapper<>();
        friendWrapper.eq(MeetingParticipant::getMeetingId, meetingId)
                .eq(MeetingParticipant::getUserId, friendId)
                .eq(MeetingParticipant::getDeleted, 0);
        
        if (participantMapper.selectCount(friendWrapper) > 0) {
            throw new BusinessException(ErrorCode.ALREADY_IN_MEETING);
        }

        // 检查人数限制
        if (meeting.getCurrentParticipants() >= meeting.getMaxParticipants()) {
            throw new BusinessException(ErrorCode.MEETING_FULL);
        }

        // 添加好友为参与者
        MeetingParticipant participant = new MeetingParticipant();
        participant.setMeetingId(meetingId);
        participant.setUserId(friendId);
        participant.setRole(0);
        participant.setJoinTime(LocalDateTime.now().format(FORMATTER));
        participant.setStatus(0);
        participantMapper.insert(participant);

        // 更新当前人数
        meeting.setCurrentParticipants(meeting.getCurrentParticipants() + 1);
        meetingMapper.updateById(meeting);

        log.info("邀请好友加入Meeting: meetingId={}, friendId={}, inviterId={}", meetingId, friendId, inviterId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void addAiParticipant(Long meetingId, Long roleId, String aiName, Long userId) {
        // 检查Meeting是否存在
        Meeting meeting = meetingMapper.selectById(meetingId);
        if (meeting == null || meeting.getDeleted() == 1) {
            throw new BusinessException(ErrorCode.MEETING_NOT_FOUND);
        }

        // 检查用户是否是创建者或已在Meeting中
        LambdaQueryWrapper<MeetingParticipant> userWrapper = new LambdaQueryWrapper<>();
        userWrapper.eq(MeetingParticipant::getMeetingId, meetingId)
                .eq(MeetingParticipant::getUserId, userId)
                .eq(MeetingParticipant::getDeleted, 0);
        
        if (participantMapper.selectCount(userWrapper) == 0) {
            throw new BusinessException(ErrorCode.NO_PERMISSION);
        }

        // 检查人数限制
        if (meeting.getCurrentParticipants() >= meeting.getMaxParticipants()) {
            throw new BusinessException(ErrorCode.MEETING_FULL);
        }

        // 使用负数ID表示AI参与者 (例如: -1, -2, -3...)
        // 查找当前最小的AI ID
        LambdaQueryWrapper<MeetingParticipant> aiWrapper = new LambdaQueryWrapper<>();
        aiWrapper.eq(MeetingParticipant::getMeetingId, meetingId)
                .lt(MeetingParticipant::getUserId, 0)
                .eq(MeetingParticipant::getDeleted, 0)
                .orderByAsc(MeetingParticipant::getUserId)
                .last("LIMIT 1");
        
        MeetingParticipant lastAi = participantMapper.selectOne(aiWrapper);
        Long aiUserId = (lastAi == null) ? -1L : lastAi.getUserId() - 1;

        // 如果没有提供角色设定,使用默认设定
        String roleSetting = (roleId != null && roleId > 0) ? 
            "You are " + aiName + ", a helpful English conversation partner." : 
            "You are " + aiName + ", a friendly AI assistant helping users practice English conversation.";

        // 添加AI参与者
        MeetingParticipant aiParticipant = new MeetingParticipant();
        aiParticipant.setMeetingId(meetingId);
        aiParticipant.setUserId(aiUserId);
        aiParticipant.setRole(0);
        aiParticipant.setJoinTime(LocalDateTime.now().format(FORMATTER));
        aiParticipant.setStatus(0);
        aiParticipant.setAiRoleName(aiName);
        aiParticipant.setAiRoleSetting(roleSetting);
        participantMapper.insert(aiParticipant);

        // 更新当前人数
        meeting.setCurrentParticipants(meeting.getCurrentParticipants() + 1);
        meetingMapper.updateById(meeting);

        log.info("添加AI参与者到Meeting: meetingId={}, aiName={}, aiUserId={}, userId={}", meetingId, aiName, aiUserId, userId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void removeParticipant(Long meetingId, Long participantId, Long userId) {
        // 检查Meeting是否存在
        Meeting meeting = meetingMapper.selectById(meetingId);
        if (meeting == null || meeting.getDeleted() == 1) {
            throw new BusinessException(ErrorCode.MEETING_NOT_FOUND);
        }

        // 检查用户权限(必须是创建者或参与者本人)
        LambdaQueryWrapper<MeetingParticipant> userWrapper = new LambdaQueryWrapper<>();
        userWrapper.eq(MeetingParticipant::getMeetingId, meetingId)
                .eq(MeetingParticipant::getUserId, userId)
                .eq(MeetingParticipant::getDeleted, 0);
        
        if (participantMapper.selectCount(userWrapper) == 0 && !meeting.getCreatorId().equals(userId)) {
            throw new BusinessException(ErrorCode.NO_PERMISSION);
        }

        // 获取要删除的参与者
        MeetingParticipant participant = participantMapper.selectById(participantId);
        if (participant == null || participant.getDeleted() == 1) {
            throw new BusinessException(ErrorCode.PARTICIPANT_NOT_FOUND);
        }

        // 不能删除创建者
        if (participant.getRole() == 1) {
            throw new BusinessException(ErrorCode.CANNOT_REMOVE_CREATOR);
        }

        // 删除参与者
        participant.markDeleted();
        participantMapper.updateById(participant);

        // 更新当前人数
        meeting.setCurrentParticipants(meeting.getCurrentParticipants() - 1);
        meetingMapper.updateById(meeting);

        log.info("移除参与者: meetingId={}, participantId={}, userId={}", meetingId, participantId, userId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateAiParticipant(Long meetingId, Long participantId, MeetingDTO.ParticipantInfo participantInfo, Long userId) {
        // 检查Meeting是否存在
        Meeting meeting = meetingMapper.selectById(meetingId);
        if (meeting == null || meeting.getDeleted() == 1) {
            throw new BusinessException(ErrorCode.MEETING_NOT_FOUND);
        }

        // 检查用户权限
        LambdaQueryWrapper<MeetingParticipant> userWrapper = new LambdaQueryWrapper<>();
        userWrapper.eq(MeetingParticipant::getMeetingId, meetingId)
                .eq(MeetingParticipant::getUserId, userId)
                .eq(MeetingParticipant::getDeleted, 0);
        
        if (participantMapper.selectCount(userWrapper) == 0) {
            throw new BusinessException(ErrorCode.NO_PERMISSION);
        }

        // 获取AI参与者
        MeetingParticipant aiParticipant = participantMapper.selectById(participantId);
        if (aiParticipant == null || aiParticipant.getDeleted() == 1) {
            throw new BusinessException(ErrorCode.PARTICIPANT_NOT_FOUND);
        }

        // 只能更新AI参与者(userId < 0)
        if (aiParticipant.getUserId() >= 0) {
            throw new BusinessException(ErrorCode.CANNOT_UPDATE_REAL_USER);
        }

        // 更新AI角色信息
        if (participantInfo.getAiRoleName() != null) {
            aiParticipant.setAiRoleName(participantInfo.getAiRoleName());
        }
        if (participantInfo.getAiRoleSetting() != null) {
            aiParticipant.setAiRoleSetting(participantInfo.getAiRoleSetting());
        }
        participantMapper.updateById(aiParticipant);

        log.info("更新AI参与者: meetingId={}, participantId={}, userId={}", meetingId, participantId, userId);
    }
}
