package com.speakmaster.ai.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.speakmaster.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * AI模型配置实体
 * 
 * @author SpeakMaster
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("ai_model_config")
public class ModelConfig extends BaseEntity {

    /** 模型名称 */
    @TableField("name")
    private String name;

    /** 提供方类型 OLLAMA / REMOTE_API / LOCAL */
    @TableField("provider_type")
    private String providerType;

    /** 模型标识 (如 gpt-4o, llama3) */
    @TableField("model_id")
    private String modelId;

    /** 服务端点URL */
    @TableField("endpoint")
    private String endpoint;

    /** API密钥 (远程API使用) */
    @TableField("api_key")
    private String apiKey;

    /** 是否启用 */
    @TableField("enabled")
    private Boolean enabled = true;

    /** 是否推荐 */
    @TableField("recommended")
    private Boolean recommended = false;

    /** 路由权重 (默认1) */
    @TableField("weight")
    private Integer weight = 1;

    /** 优先级(数值越小优先级越高) */
    @TableField("priority")
    private Integer priority = 10;

    /** 最大Token数 */
    @TableField("max_tokens")
    private Integer maxTokens = 2048;

    /** 温度参数 */
    @TableField("temperature")
    private Double temperature = 0.7;

    /** 超时时间(秒) */
    @TableField("timeout")
    private Integer timeout = 30;

    /** 模型描述 */
    @TableField("description")
    private String description;

    /** 健康状态 */
    @TableField("healthy")
    private Boolean healthy = true;

    /** 是否支持多模态（音频/图片输入） */
    @TableField("multimodal")
    private Boolean multimodal = false;
}
