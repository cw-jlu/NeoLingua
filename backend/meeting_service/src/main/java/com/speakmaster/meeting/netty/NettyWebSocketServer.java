package com.speakmaster.meeting.netty;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;

/**
 * Netty WebSocket 服务器
 * 用于处理 Meeting 实时通信，支持高并发和低延迟
 * 
 * @author SpeakMaster
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class NettyWebSocketServer {

    @Value("${netty.websocket.port:8090}")
    private int port;

    @Value("${netty.websocket.boss-threads:1}")
    private int bossThreads;

    @Value("${netty.websocket.worker-threads:0}")
    private int workerThreads;

    private final NettyWebSocketInitializer webSocketInitializer;

    private EventLoopGroup bossGroup;
    private EventLoopGroup workerGroup;
    private ChannelFuture channelFuture;

    /**
     * 启动 Netty WebSocket 服务器
     */
    @PostConstruct
    public void start() {
        new Thread(() -> {
            try {
                // Boss线程组：处理连接请求
                bossGroup = new NioEventLoopGroup(bossThreads);
                // Worker线程组：处理I/O操作，0表示使用默认线程数（CPU核心数*2）
                workerGroup = new NioEventLoopGroup(workerThreads);

                ServerBootstrap bootstrap = new ServerBootstrap();
                bootstrap.group(bossGroup, workerGroup)
                        .channel(NioServerSocketChannel.class)
                        .childHandler(webSocketInitializer)
                        .option(ChannelOption.SO_BACKLOG, 1024)
                        .childOption(ChannelOption.SO_KEEPALIVE, true)
                        .childOption(ChannelOption.TCP_NODELAY, true);

                channelFuture = bootstrap.bind(port).sync();
                log.info("Netty WebSocket 服务器启动成功，端口: {}", port);

                // 等待服务器关闭
                channelFuture.channel().closeFuture().sync();
            } catch (Exception e) {
                log.error("Netty WebSocket 服务器启动失败", e);
            } finally {
                shutdown();
            }
        }, "netty-websocket-server").start();
    }

    /**
     * 关闭 Netty WebSocket 服务器
     */
    @PreDestroy
    public void shutdown() {
        try {
            if (channelFuture != null) {
                channelFuture.channel().close().sync();
            }
        } catch (InterruptedException e) {
            log.error("关闭 Netty Channel 失败", e);
            Thread.currentThread().interrupt();
        } finally {
            if (workerGroup != null) {
                workerGroup.shutdownGracefully();
            }
            if (bossGroup != null) {
                bossGroup.shutdownGracefully();
            }
            log.info("Netty WebSocket 服务器已关闭");
        }
    }
}
