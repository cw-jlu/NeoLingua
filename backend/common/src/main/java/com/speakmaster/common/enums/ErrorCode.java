package com.speakmaster.common.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 错误码枚�?
 * 统一管理系统中的错误码和错误消息
 * 
 * @author SpeakMaster
 */
@Getter
@AllArgsConstructor
public enum ErrorCode {

    // ========== 通用错误码 1xxx ==========
    SUCCESS(200, "操作成功"),
    SYSTEM_ERROR(1000, "系统异常，请稍后重试"),
    PARAM_ERROR(1001, "参数错误"),
    PARAM_MISSING(1002, "缺少必要参数"),
    PARAM_INVALID(1003, "参数格式不正确"),
    REQUEST_METHOD_ERROR(1004, "请求方法不支持"),
    REQUEST_TIMEOUT(1005, "请求超时"),
    TOO_MANY_REQUESTS(1006, "请求过于频繁，请稍后再试"),

    // ========== 认证授权错误码 2xxx ==========
    UNAUTHORIZED(2000, "未登录或登录已过期"),
    TOKEN_INVALID(2001, "Token无效"),
    TOKEN_EXPIRED(2002, "Token已过期"),
    PERMISSION_DENIED(2003, "权限不足"),
    ACCOUNT_DISABLED(2004, "账号已被禁用"),
    ACCOUNT_LOCKED(2005, "账号已被锁定"),

    // ========== 用户相关错误码 3xxx ==========
    USER_NOT_FOUND(3000, "用户不存在"),
    USER_ALREADY_EXISTS(3001, "用户已存在"),
    USERNAME_OR_PASSWORD_ERROR(3002, "用户名或密码错误"),
    PASSWORD_ERROR(3003, "密码错误"),
    OLD_PASSWORD_ERROR(3004, "原密码错误"),
    USER_PROFILE_NOT_FOUND(3005, "用户信息不存在"),
    USER_ALREADY_SIGNED_IN(3006, "今日已签到"),
    INSUFFICIENT_POINTS(3007, "积分不足"),
    BADGE_NOT_FOUND(3008, "徽章不存在"),
    BADGE_ALREADY_CLAIMED(3009, "徽章已领取"),

    // ========== 练习相关错误码 4xxx ==========
    THEME_NOT_FOUND(4000, "主题不存在"),
    ROLE_NOT_FOUND(4001, "角色不存在"),
    SESSION_NOT_FOUND(4002, "会话不存在"),
    SESSION_ALREADY_ENDED(4003, "会话已结束"),
    SESSION_NOT_STARTED(4004, "会话未开始"),
    MESSAGE_SEND_FAILED(4005, "消息发送失败"),

    // ========== 权限相关 ==========
    NO_PERMISSION(2010, "没有操作权限"),

    // ========== Meeting相关错误码 5xxx ==========
    FRIEND_NOT_FOUND(5000, "好友不存在"),
    FRIEND_REQUEST_NOT_FOUND(5001, "好友请求不存在"),
    ALREADY_FRIENDS(5002, "已经是好友关系"),
    FRIEND_REQUEST_ALREADY_SENT(5003, "好友请求已发送"),
    MEETING_NOT_FOUND(5004, "Meeting不存在"),
    MEETING_FULL(5005, "Meeting人数已满"),
    MEETING_ALREADY_STARTED(5006, "Meeting已开始"),
    MEETING_NOT_STARTED(5007, "Meeting未开始"),
    NOT_MEETING_PARTICIPANT(5008, "不是Meeting参与者"),
    NOT_MEETING_OWNER(5009, "不是Meeting创建者"),
    MEETING_ALREADY_ENDED(5010, "Meeting已结束"),
    ALREADY_IN_MEETING(5011, "已在Meeting中"),
    NOT_IN_MEETING(5012, "不在Meeting中"),
    FRIEND_REQUEST_EXISTS(5013, "好友请求已存在"),
    FRIEND_REQUEST_ALREADY_HANDLED(5014, "好友请求已处理"),
    MESSAGE_NOT_FOUND(5015, "消息不存在"),
    PARTICIPANT_NOT_FOUND(5016, "参与者不存在"),
    CANNOT_REMOVE_CREATOR(5017, "不能移除创建者"),
    CANNOT_UPDATE_REAL_USER(5018, "不能修改真实用户信息"),

    // ========== 社区相关错误码 6xxx ==========
    POST_NOT_FOUND(6000, "帖子不存在"),
    COMMENT_NOT_FOUND(6001, "评论不存在"),
    ALREADY_LIKED(6002, "已经点赞"),
    NOT_LIKED(6003, "未点赞"),
    ALREADY_FAVORITED(6004, "已经收藏"),
    NOT_FAVORITED(6005, "未收藏"),
    TAG_NOT_FOUND(6006, "标签不存在"),
    REPORT_NOT_FOUND(6007, "举报不存在"),

    // ========== 分析相关错误码 7xxx ==========
    REPORT_GENERATE_FAILED(7000, "报表生成失败"),
    EXPORT_FAILED(7001, "导出失败"),
    STATISTICS_NOT_FOUND(7002, "统计数据不存在"),

    // ========== 通知相关错误码 8xxx ==========
    NOTIFICATION_NOT_FOUND(8000, "通知不存在"),
    NOTIFICATION_SEND_FAILED(8001, "通知发送失败"),

    // ========== AI相关错误码 9xxx ==========
    AI_SERVICE_ERROR(9000, "AI服务异常"),
    AI_MODEL_NOT_FOUND(9001, "AI模型不存在"),
    AI_MODEL_UNAVAILABLE(9002, "AI模型不可用"),
    AI_RESPONSE_TIMEOUT(9003, "AI响应超时"),
    MEMORY_SAVE_FAILED(9004, "记忆保存失败"),
    MEMORY_RETRIEVE_FAILED(9005, "记忆检索失败"),

    // ========== 文件相关错误码 10xxx ==========
    FILE_UPLOAD_FAILED(10000, "文件上传失败"),
    FILE_NOT_FOUND(10001, "文件不存在"),
    FILE_TYPE_NOT_SUPPORTED(10002, "文件类型不支持"),
    FILE_SIZE_EXCEEDED(10003, "文件大小超出限制"),

    // ========== 管理相关错误码 11xxx ==========
    CONFIG_KEY_EXISTS(11000, "配置键已存在"),
    CONFIG_NOT_FOUND(11001, "配置不存在"),
    LOG_NOT_FOUND(11002, "日志不存在"),
    LOG_EXPORT_FAILED(11003, "日志导出失败");

    /**
     * 错误码
     */
    private final Integer code;

    /**
     * 错误消息
     */
    private final String msg;
}
