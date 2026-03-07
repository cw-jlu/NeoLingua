package com.speakmaster.user.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.speakmaster.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 徽章实体类
 * 
 * @author SpeakMaster
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("badge")
public class Badge extends BaseEntity {

    /**
     * 徽章名称
     */
    @TableField("name")
    private String name;

    /**
     * 徽章描述
     */
    @TableField("description")
    private String description;

    /**
     * 徽章图标URL
     */
    @TableField("icon")
    private String icon;

    /**
     * 徽章类型 (1-成就, 2-等级, 3-活动)
     */
    @TableField("type")
    private Integer type;

    /**
     * 获取条件
     */
    @TableField("condition_desc")
    private String conditionDesc;

    /**
     * 所需积分
     */
    @TableField("required_points")
    private Long requiredPoints;

    /**
     * 排序
     */
    @TableField("sort_order")
    private Integer sortOrder = 0;

    /**
     * 状态(0-禁用, 1-启用)
     */
    @TableField("status")
    private Integer status = 1;
}
