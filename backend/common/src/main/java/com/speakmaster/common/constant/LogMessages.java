package com.speakmaster.common.constant;

/**
 * 日志消息常量类
 * 统一管理系统中的日志消息
 * 
 * @author SpeakMaster
 */
public final class LogMessages {

    private LogMessages() {
        throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
    }

    // ========== 认证相关 ==========
    public static final String USER_REGISTER_REQUEST = "用户注册请求: username={}";
    public static final String USER_LOGIN_REQUEST = "用户登录请求: username={}, ip={}";
    public static final String USER_LOGOUT_REQUEST = "用户登出请求";
    public static final String TOKEN_REFRESH_SUCCESS = "Token刷新成功";
    public static final String USER_DELETE_ACCOUNT_REQUEST = "用户注销账号请求: userId={}";

    // ========== Token相关 ==========
    public static final String TOKEN_PARSE_FAILED = "Token解析失败: {}";
    public static final String TOKEN_VALIDATE_FAILED = "Token验证失败: {}";
    public static final String REQUEST_WITHOUT_TOKEN = "请求未携带Token: path={}";
    public static final String TOKEN_VALIDATION_FAILED = "Token验证失败: path={}, token={}";

    // ========== 用户上下文 ==========
    public static final String SET_USER_CONTEXT = "设置用户上下文: userId={}, username={}";
    public static final String CLEAR_USER_CONTEXT = "清除用户上下文";
    public static final String PARSE_USER_ID_FAILED = "解析用户ID失败: {}";

    // ========== 业务操作 ==========
    public static final String CREATE_ENTITY = "创建{}: {}";
    public static final String UPDATE_ENTITY = "更新{}: {}";
    public static final String DELETE_ENTITY = "删除{}: {}";
    public static final String QUERY_ENTITY = "查询{}: {}";

    // ========== 异常相关 ==========
    public static final String BUSINESS_EXCEPTION = "业务异常: {}";
    public static final String SYSTEM_EXCEPTION = "系统异常: {}";
    public static final String VALIDATION_EXCEPTION = "参数校验异常: {}";

    // ========== WebSocket相关 ==========
    public static final String WEBSOCKET_CONNECTION_ESTABLISHED = "WebSocket连接建立: sessionId={}";
    public static final String WEBSOCKET_CONNECTION_CLOSED = "WebSocket连接关闭: sessionId={}";
    public static final String WEBSOCKET_MESSAGE_RECEIVED = "WebSocket收到消息: sessionId={}, message={}";
    public static final String WEBSOCKET_MESSAGE_SENT = "WebSocket发送消息: sessionId={}, message={}";
    public static final String WEBSOCKET_ERROR = "WebSocket错误: sessionId={}, error={}";

    // ========== 缓存相关 ==========
    public static final String CACHE_HIT = "缓存命中: key={}";
    public static final String CACHE_MISS = "缓存未命中: key={}";
    public static final String CACHE_PUT = "写入缓存: key={}";
    public static final String CACHE_EVICT = "清除缓存: key={}";

    // ========== 数据库相关 ==========
    public static final String DB_QUERY_START = "数据库查询开始: {}";
    public static final String DB_QUERY_END = "数据库查询结束: {}, 耗时: {}ms";
    public static final String DB_UPDATE_START = "数据库更新开始: {}";
    public static final String DB_UPDATE_END = "数据库更新结束: {}, 影响行数: {}";

    // ========== AI相关 ==========
    public static final String AI_REQUEST_START = "AI请求开始: modelId={}, prompt={}";
    public static final String AI_REQUEST_END = "AI请求结束: modelId={}, 耗时: {}ms";
    public static final String AI_REQUEST_FAILED = "AI请求失败: modelId={}, error={}";
    public static final String AI_STREAM_START = "AI流式响应开始: modelId={}";
    public static final String AI_STREAM_END = "AI流式响应结束: modelId={}";

    // ========== 文件相关 ==========
    public static final String FILE_UPLOAD_START = "文件上传开始: filename={}";
    public static final String FILE_UPLOAD_SUCCESS = "文件上传成功: filename={}, path={}";
    public static final String FILE_UPLOAD_FAILED = "文件上传失败: filename={}, error={}";
    public static final String FILE_DELETE = "文件删除: path={}";

    // ========== 消息队列相关 ==========
    public static final String MQ_SEND_MESSAGE = "发送MQ消息: topic={}, message={}";
    public static final String MQ_RECEIVE_MESSAGE = "接收MQ消息: topic={}, message={}";
    public static final String MQ_CONSUME_SUCCESS = "MQ消息消费成功: topic={}, messageId={}";
    public static final String MQ_CONSUME_FAILED = "MQ消息消费失败: topic={}, messageId={}, error={}";

    // ========== 定时任务相关 ==========
    public static final String SCHEDULED_TASK_START = "定时任务开始: taskName={}";
    public static final String SCHEDULED_TASK_END = "定时任务结束: taskName={}, 耗时: {}ms";
    public static final String SCHEDULED_TASK_FAILED = "定时任务失败: taskName={}, error={}";

    // ========== 服务调用相关 ==========
    public static final String SERVICE_CALL_START = "服务调用开始: service={}, method={}";
    public static final String SERVICE_CALL_END = "服务调用结束: service={}, method={}, 耗时: {}ms";
    public static final String SERVICE_CALL_FAILED = "服务调用失败: service={}, method={}, error={}";

    // ========== 搜索相关 ==========
    public static final String SEARCH_START = "搜索开始: keyword={}";
    public static final String SEARCH_END = "搜索结束: keyword={}, 结果数: {}";
    public static final String INDEX_SYNC_START = "索引同步开始";
    public static final String INDEX_SYNC_END = "索引同步结束: 同步数量: {}";

    // ========== 通知相关 ==========
    public static final String NOTIFICATION_SEND = "发送通知: userId={}, type={}";
    public static final String NOTIFICATION_SEND_SUCCESS = "通知发送成功: notificationId={}";
    public static final String NOTIFICATION_SEND_FAILED = "通知发送失败: userId={}, error={}";

    // ========== 会话相关 ==========
    public static final String SESSION_CREATE = "创建会话: userId={}, sessionId={}";
    public static final String SESSION_END = "结束会话: sessionId={}, duration={}";
    public static final String SESSION_NOT_FOUND = "会话不存在: sessionId={}";

    // ========== Meeting相关 ==========
    public static final String MEETING_CREATE = "创建Meeting: userId={}, meetingId={}";
    public static final String MEETING_JOIN = "加入Meeting: userId={}, meetingId={}";
    public static final String MEETING_LEAVE = "离开Meeting: userId={}, meetingId={}";
    public static final String MEETING_START = "开始Meeting: meetingId={}";
    public static final String MEETING_END = "结束Meeting: meetingId={}";

    // ========== 社区相关 ==========
    public static final String POST_CREATE = "创建帖子: userId={}, postId={}";
    public static final String POST_UPDATE = "更新帖子: postId={}";
    public static final String POST_DELETE = "删除帖子: postId={}";
    public static final String COMMENT_CREATE = "创建评论: userId={}, postId={}, commentId={}";
    public static final String COMMENT_DELETE = "删除评论: commentId={}";

    // ========== 系统监控相关 ==========
    public static final String SYSTEM_STARTUP = "系统启动成功";
    public static final String SYSTEM_SHUTDOWN = "系统关闭";
    public static final String HEALTH_CHECK = "健康检查: status={}";
    public static final String METRICS_COLLECT = "指标收集: {}";
}
