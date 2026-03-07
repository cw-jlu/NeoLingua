package com.speakmaster.meeting.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.speakmaster.meeting.dto.MeetingDTO;

import java.util.Map;

/**
 * Meeting服务接口
 * 
 * @author SpeakMaster
 */
public interface IMeetingService {

    /**
     * 创建Meeting
     */
    MeetingDTO createMeeting(MeetingDTO meetingDTO, Long userId);

    /**
     * 获取Meeting列表
     */
    Page<MeetingDTO> getMeetingList(Long userId, int page, int size);

    /**
     * 获取Meeting详情
     */
    MeetingDTO getMeetingById(Long meetingId);

    /**
     * 加入Meeting
     */
    void joinMeeting(Long meetingId, Long userId);

    /**
     * 离开Meeting
     */
    void leaveMeeting(Long meetingId, Long userId);

    /**
     * 开始Meeting
     */
    MeetingDTO startMeeting(Long meetingId, Long userId);

    /**
     * 结束Meeting
     */
    MeetingDTO endMeeting(Long meetingId, Long userId);

    /**
     * 删除Meeting
     */
    void deleteMeeting(Long meetingId, Long userId);

    /**
     * [管理端] 分页查询所有Meeting
     */
    Page<MeetingDTO> adminGetMeetingList(Integer status, int page, int size);

    /**
     * [管理端] 删除Meeting
     */
    void adminDeleteMeeting(Long meetingId);

    /**
     * [管理端] 强制关闭Meeting
     */
    MeetingDTO adminCloseMeeting(Long meetingId);

    /**
     * [管理端] Meeting统计
     */
    Map<String, Object> adminGetMeetingStatistics();

    /**
     * 邀请好友加入Meeting
     */
    void inviteFriend(Long meetingId, Long friendId, Long inviterId);

    /**
     * 添加AI参与者到Meeting
     */
    void addAiParticipant(Long meetingId, Long roleId, String aiName, Long userId);

    /**
     * 移除参与者(包括AI)
     */
    void removeParticipant(Long meetingId, Long participantId, Long userId);

    /**
     * 更新AI参与者设定
     */
    void updateAiParticipant(Long meetingId, Long participantId, MeetingDTO.ParticipantInfo participantInfo, Long userId);
}
