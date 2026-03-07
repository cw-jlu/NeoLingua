package com.speakmaster.admin.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.speakmaster.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 系统日志实体
 * 
 * @author SpeakMaster
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("system_log")
public class SystemLog extends BaseEntity {

    /** 操作用户ID */
    @TableField("user_id")
    private Long userId;

    /** 操作模块 */
    @TableField("module")
    private String module;

    /** 操作类型 */
    @TableField("operation")
    private String operation;

    /** 操作描述 */
    @TableField("description")
    private String description;

    /** 请求方法 */
    @TableField("method")
    private String method;

    /** 请求URL */
    @TableField("url")
    private String url;

    /** 请求参数 */
    @TableField("params")
    private String params;

    /** IP地址 */
    @TableField("ip")
    private String ip;

    /** 执行时间(ms) */
    @TableField("execution_time")
    private Long executionTime;

    /** 状态(0-失败 1-成功) */
    @TableField("status")
    private Integer status;

    /** 错误信息 */
    @TableField("error_msg")
    private String errorMsg;
}
