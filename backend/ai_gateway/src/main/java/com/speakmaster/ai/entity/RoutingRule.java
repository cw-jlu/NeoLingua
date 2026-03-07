package com.speakmaster.ai.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.speakmaster.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * AI路由规则实体
 * 
 * @author SpeakMaster
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("ai_routing_rule")
public class RoutingRule extends BaseEntity {

    /** 规则名称 */
    @TableField("name")
    private String name;

    /** 路由策略: WEIGHT / PRIORITY / ROUND_ROBIN */
    @TableField("strategy")
    private String strategy;

    /** 是否启用 */
    @TableField("enabled")
    private Boolean enabled = true;

    /** 规则优先级 */
    @TableField("priority")
    private Integer priority = 10;

    /** 规则描述 */
    @TableField("description")
    private String description;

    /** 关联的模型ID列表 (JSON数组格式) */
    @TableField("model_ids")
    private String modelIds;
}
