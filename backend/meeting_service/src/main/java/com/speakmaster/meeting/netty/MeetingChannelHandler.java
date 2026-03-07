package com.speakmaster.meeting.netty;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.speakmaster.meeting.dto.MeetingMessageDTO;
import com.speakmaster.meeting.service.IMeetingMessageService;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.QueryStringDecoder;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import io.netty.handler.codec.http.websocketx.WebSocketFrame;
import io.netty.handler.timeout.IdleState;
import io.netty.handler.timeout.IdleStateEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Meeting 消息处理器
 * 处理 WebSocket 连接、消息收发和心跳检测
 * 
 * @author SpeakMaster
 */
@Slf4j
@Component
@ChannelHandler.Sharable
public class MeetingChannelHandler extends SimpleChannelInboundHandler<WebSocketFrame> {

    private final IMeetingMessageService messageService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    // 用 @Lazy 打破与 MeetingMessageServiceImpl 的循环依赖
    @Autowired
    public MeetingChannelHandler(@Lazy IMeetingMessageService messageService) {
        this.messageService = messageService;
    }

    /**
     * Meeting房间 -> 在线用户通道映射
     * key: meetingId, value: (userId -> Channel)
     */
    private static final Map<Long, Map<Long, Channel>> MEETING_CHANNELS = new ConcurrentHashMap<>();

    /**
     * Channel -> 用户信息映射
     * key: channelId, value: UserInfo(userId, meetingId)
     */
    private static final Map<String, UserInfo> CHANNEL_USER_MAP = new ConcurrentHashMap<>();

    /**
     * 连接建立时，从 HTTP 请求中提取用户信息
     */
    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        if (msg instanceof FullHttpRequest) {
            FullHttpRequest request = (FullHttpRequest) msg;
            String uri = request.uri();
            
            // 解析 URL 参数：/ws/meeting?userId=123&meetingId=456
            QueryStringDecoder decoder = new QueryStringDecoder(uri);
            Map<String, List<String>> params = decoder.parameters();
            
            try {
                Long userId = Long.parseLong(params.get("userId").get(0));
                Long meetingId = Long.parseLong(params.get("meetingId").get(0));
                
                // 保存用户信息
                String channelId = ctx.channel().id().asLongText();
                CHANNEL_USER_MAP.put(channelId, new UserInfo(userId, meetingId));
                
                log.debug("WebSocket 握手，提取用户信息: userId={}, meetingId={}", userId, meetingId);
            } catch (Exception e) {
                log.error("解析用户信息失败: uri={}", uri, e);
                ctx.close();
                return;
            }
        }
        super.channelRead(ctx, msg);
    }

    /**
     * WebSocket 连接激活
     */
    @Override
    public void channelActive(ChannelHandlerContext ctx) {
        String channelId = ctx.channel().id().asLongText();
        UserInfo userInfo = CHANNEL_USER_MAP.get(channelId);
        
        if (userInfo != null) {
            // 将用户加入 Meeting 房间
            MEETING_CHANNELS.computeIfAbsent(userInfo.meetingId, k -> new ConcurrentHashMap<>())
                    .put(userInfo.userId, ctx.channel());
            
            log.info("用户加入 Meeting: userId={}, meetingId={}, 当前在线人数={}",
                    userInfo.userId, userInfo.meetingId, 
                    MEETING_CHANNELS.get(userInfo.meetingId).size());
            
            // 广播系统消息：用户加入
            broadcastSystemMessage(userInfo.meetingId, userInfo.userId, "加入了Meeting");
        }
    }

    /**
     * 接收 WebSocket 消息
     */
    @Override
    protected void channelRead0(ChannelHandlerContext ctx, WebSocketFrame frame) {
        String channelId = ctx.channel().id().asLongText();
        UserInfo userInfo = CHANNEL_USER_MAP.get(channelId);
        
        if (userInfo == null) {
            log.warn("未找到用户信息，关闭连接");
            ctx.close();
            return;
        }

        // 只处理文本消息
        if (frame instanceof TextWebSocketFrame) {
            String payload = ((TextWebSocketFrame) frame).text();
            
            try {
                // 解析客户端消息
                MeetingMessageDTO messageDTO = objectMapper.readValue(payload, MeetingMessageDTO.class);
                
                // 持久化消息到数据库
                MeetingMessageDTO savedMessage = messageService.sendMessage(
                        userInfo.meetingId, messageDTO, userInfo.userId);
                
                // 广播消息给 Meeting 房间内所有用户
                String broadcastPayload = objectMapper.writeValueAsString(savedMessage);
                broadcastToMeeting(userInfo.meetingId, broadcastPayload);
                
            } catch (Exception e) {
                log.error("处理 Meeting 消息失败: userId={}, meetingId={}", 
                        userInfo.userId, userInfo.meetingId, e);
                sendErrorMessage(ctx.channel(), "消息发送失败: " + e.getMessage());
            }
        }
    }

    /**
     * 连接断开
     */
    @Override
    public void channelInactive(ChannelHandlerContext ctx) {
        String channelId = ctx.channel().id().asLongText();
        UserInfo userInfo = CHANNEL_USER_MAP.remove(channelId);
        
        if (userInfo != null) {
            // 从 Meeting 房间移除用户
            Map<Long, Channel> channels = MEETING_CHANNELS.get(userInfo.meetingId);
            if (channels != null) {
                channels.remove(userInfo.userId);
                if (channels.isEmpty()) {
                    MEETING_CHANNELS.remove(userInfo.meetingId);
                }
            }
            
            log.info("用户离开 Meeting: userId={}, meetingId={}", 
                    userInfo.userId, userInfo.meetingId);
            
            // 广播系统消息：用户离开
            broadcastSystemMessage(userInfo.meetingId, userInfo.userId, "离开了Meeting");
        }
    }

    /**
     * 心跳检测
     */
    @Override
    public void userEventTriggered(ChannelHandlerContext ctx, Object evt) {
        if (evt instanceof IdleStateEvent) {
            IdleStateEvent event = (IdleStateEvent) evt;
            if (event.state() == IdleState.READER_IDLE) {
                String channelId = ctx.channel().id().asLongText();
                UserInfo userInfo = CHANNEL_USER_MAP.get(channelId);
                log.warn("连接读超时，关闭连接: userId={}, meetingId={}", 
                        userInfo != null ? userInfo.userId : "unknown",
                        userInfo != null ? userInfo.meetingId : "unknown");
                ctx.close();
            }
        }
    }

    /**
     * 异常处理
     */
    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        String channelId = ctx.channel().id().asLongText();
        UserInfo userInfo = CHANNEL_USER_MAP.get(channelId);
        log.error("WebSocket 异常: userId={}, meetingId={}", 
                userInfo != null ? userInfo.userId : "unknown",
                userInfo != null ? userInfo.meetingId : "unknown", cause);
        ctx.close();
    }

    /**
     * 广播消息给 Meeting 房间内所有用户
     */
    private void broadcastToMeeting(Long meetingId, String payload) {
        Map<Long, Channel> channels = MEETING_CHANNELS.get(meetingId);
        if (channels == null || channels.isEmpty()) return;

        TextWebSocketFrame frame = new TextWebSocketFrame(payload);
        channels.values().forEach(channel -> {
            if (channel.isActive()) {
                channel.writeAndFlush(frame.retain());
            }
        });
        frame.release();
    }

    /**
     * 公共方法: 广播消息给指定Meeting(供Service层调用)
     */
    public void broadcastMessage(Long meetingId, String payload) {
        broadcastToMeeting(meetingId, payload);
    }

    /**
     * 广播系统消息
     */
    private void broadcastSystemMessage(Long meetingId, Long userId, String action) {
        try {
            Map<String, Object> systemMsg = new HashMap<>();
            systemMsg.put("messageType", 2); // 系统消息
            systemMsg.put("senderId", userId);
            systemMsg.put("content", "用户 " + userId + " " + action);
            systemMsg.put("meetingId", meetingId);
            systemMsg.put("createTime", System.currentTimeMillis());

            String payload = objectMapper.writeValueAsString(systemMsg);
            broadcastToMeeting(meetingId, payload);
        } catch (Exception e) {
            log.error("广播系统消息失败: meetingId={}", meetingId, e);
        }
    }

    /**
     * 发送错误消息给指定用户
     */
    private void sendErrorMessage(Channel channel, String errorMsg) {
        try {
            Map<String, Object> error = new HashMap<>();
            error.put("messageType", -1); // 错误消息
            error.put("content", errorMsg);
            error.put("timestamp", System.currentTimeMillis());
            
            String payload = objectMapper.writeValueAsString(error);
            channel.writeAndFlush(new TextWebSocketFrame(payload));
        } catch (Exception e) {
            log.error("发送错误消息失败", e);
        }
    }

    /**
     * 获取 Meeting 房间在线人数
     */
    public int getOnlineCount(Long meetingId) {
        Map<Long, Channel> channels = MEETING_CHANNELS.get(meetingId);
        return channels != null ? channels.size() : 0;
    }

    /**
     * 获取 Meeting 房间在线用户ID列表
     */
    public Set<Long> getOnlineUserIds(Long meetingId) {
        Map<Long, Channel> channels = MEETING_CHANNELS.get(meetingId);
        return channels != null ? channels.keySet() : Collections.emptySet();
    }

    /**
     * 用户信息内部类
     */
    private static class UserInfo {
        final Long userId;
        final Long meetingId;

        UserInfo(Long userId, Long meetingId) {
            this.userId = userId;
            this.meetingId = meetingId;
        }
    }
}
