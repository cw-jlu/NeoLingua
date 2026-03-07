package com.speakmaster.practice.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.speakmaster.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 角色实体类
 * 
 * @author SpeakMaster
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("role")
public class Role extends BaseEntity {

    /** 角色名称 */
    @TableField("name")
    private String name;

    /** 角色描述 */
    @TableField("description")
    private String description;

    /** 角色头像 */
    @TableField("avatar")
    private String avatar;

    /** 角色类型 (1-预制, 2-自定义) */
    @TableField("type")
    private Integer type;

    /** 创建者ID (自定义角色) */
    @TableField("creator_id")
    private Long creatorId;

    /** 角色设定 (性格、背景等) */
    @TableField("setting")
    private String setting;

    /** 使用次数 */
    @TableField("use_count")
    private Long useCount = 0L;

    /** 状态(0-禁用, 1-启用) */
    @TableField("status")
    private Integer status = 1;
}
