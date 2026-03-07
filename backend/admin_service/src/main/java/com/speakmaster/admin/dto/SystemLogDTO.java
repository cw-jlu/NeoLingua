package com.speakmaster.admin.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 系统日志DTO
 * 
 * @author SpeakMaster
 */
@Data
public class SystemLogDTO {

    /** 日志ID */
    private Long id;

    /** 操作用户ID */
    private Long userId;

    /** 操作模块 */
    private String module;

    /** 操作类型 */
    private String operation;

    /** 操作描述 */
    private String description;

    /** 请求方法 */
    private String method;

    /** 请求URL */
    private String url;

    /** 请求参数 */
    private String params;

    /** IP地址 */
    private String ip;

    /** 执行时间(ms) */
    private Long executionTime;

    /** 状�?(0-失败 1-成功) */
    private Integer status;

    /** 错误信息 */
    private String errorMsg;

    /** 创建时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;
}
