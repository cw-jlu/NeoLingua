package com.speakmaster.practice.dto;

import lombok.Data;

/**
 * 练习会话DTO
 *
 * @author SpeakMaster
 */
@Data
public class SessionDTO {

    /** 会话ID */
    private Long id;

    /** 用户ID */
    private Long userId;

    /** 主题ID */
    private Long themeId;

    /** 角色ID */
    private Long roleId;

    /** 状态 (0-进行中 1-已结束) */
    private Integer status;

    /** 开始时间 */
    private String startTime;

    /** 结束时间 */
    private String endTime;

    /** 评分 (0-100) */
    private Integer score;

    /** 反馈内容 */
    private String feedback;

    /** 主题名称（展示用） */
    private String themeName;

    /** 消息数量（展示用） */
    private Integer messageCount;

    /** 创建时间 */
    private String createTime;
}
