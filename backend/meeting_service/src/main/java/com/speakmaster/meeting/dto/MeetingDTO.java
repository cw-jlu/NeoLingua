package com.speakmaster.meeting.dto;

import lombok.Data;
import java.util.List;

/**
 * Meeting DTO
 * 
 * @author SpeakMaster
 */
@Data
public class MeetingDTO {
    
    /** Meeting ID */
    private Long id;
    
    /** 房间名称 */
    private String name;
    
    /** 房间描述 */
    private String description;
    
    /** 创建者ID */
    private Long creatorId;
    
    /** 主题ID */
    private Long themeId;
    
    /** 最大人�?*/
    private Integer maxParticipants;
    
    /** 当前人数 */
    private Integer currentParticipants;
    
    /** 状�?(0-等待�?1-进行�?2-已结�? */
    private Integer status;
    
    /** 开始时�?*/
    private String startTime;
    
    /** 结束时间 */
    private String endTime;
    
    /** 是否公开 (0-私密 1-公开) */
    private Integer isPublic;
    
    /** 参与者列表 */
    private List<ParticipantInfo> participants;
    
    /**
     * 参与者信息
     */
    @Data
    public static class ParticipantInfo {
        private Long id;
        private Long userId;
        private String username;
        private Integer role; // 0-普通成员 1-创建者
        private String joinTime;
        private String aiRoleName; // AI角色名称
        private String aiRoleSetting; // AI角色设定
    }
}

