package com.speakmaster.admin.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.speakmaster.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 系统配置实体
 * 
 * @author SpeakMaster
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("system_config")
public class SystemConfig extends BaseEntity {

    /** 配置键 */
    @TableField("config_key")
    private String configKey;

    /** 配置值 */
    @TableField("config_value")
    private String configValue;

    /** 配置描述 */
    @TableField("description")
    private String description;

    /** 配置分类 */
    @TableField("category")
    private String category;

    /** 是否启用 (0-禁用 1-启用) */
    @TableField("is_enabled")
    private Integer isEnabled = 1;
}
