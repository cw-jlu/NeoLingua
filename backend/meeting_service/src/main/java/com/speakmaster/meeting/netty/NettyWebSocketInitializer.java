package com.speakmaster.meeting.netty;

import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpServerCodec;
import io.netty.handler.codec.http.websocketx.WebSocketServerProtocolHandler;
import io.netty.handler.stream.ChunkedWriteHandler;
import io.netty.handler.timeout.IdleStateHandler;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

/**
 * Netty WebSocket 通道初始化器
 * 配置 WebSocket 协议处理器和业务处理器
 * 
 * @author SpeakMaster
 */
@Component
@RequiredArgsConstructor
public class NettyWebSocketInitializer extends ChannelInitializer<SocketChannel> {

    private final MeetingChannelHandler meetingChannelHandler;

    @Override
    protected void initChannel(SocketChannel ch) {
        ChannelPipeline pipeline = ch.pipeline();

        // HTTP 编解码器
        pipeline.addLast(new HttpServerCodec());
        
        // HTTP 消息聚合器，最大消息大小 64KB
        pipeline.addLast(new HttpObjectAggregator(65536));
        
        // 支持大文件传输
        pipeline.addLast(new ChunkedWriteHandler());
        
        // 心跳检测：读超时60秒，写超时0，读写超时0
        pipeline.addLast(new IdleStateHandler(60, 0, 0, TimeUnit.SECONDS));
        
        // WebSocket 协议处理器，路径为 /ws/meeting
        pipeline.addLast(new WebSocketServerProtocolHandler("/ws/meeting", null, true, 65536));
        
        // 自定义业务处理器
        pipeline.addLast(meetingChannelHandler);
    }
}
