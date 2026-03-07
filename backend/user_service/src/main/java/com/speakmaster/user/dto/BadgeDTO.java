package com.speakmaster.user.dto;

import lombok.Data;

/**
 * 徽章DTO
 * 
 * @author SpeakMaster
 */
@Data
public class BadgeDTO {

    /**
     * 徽章ID
     */
    private Long id;

    /**
     * 徽章名称
     */
    private String name;

    /**
     * 徽章描述
     */
    private String description;

    /**
     * 徽章图标URL
     */
    private String icon;

    /**
     * 徽章类型 (1-成就, 2-等级, 3-活动)
     */
    private Integer type;

    /**
     * 获取条件
     */
    private String conditionDesc;

    /**
     * 所需积分
     */
    private Long requiredPoints;

    /**
     * 是否已获�?
     */
    private Boolean obtained;

    /**
     * 获得时间
     */
    private String obtainedTime;
}
