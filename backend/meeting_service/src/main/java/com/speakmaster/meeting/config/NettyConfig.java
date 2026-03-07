package com.speakmaster.meeting.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Netty 配置类
 * 
 * @author SpeakMaster
 */
@Configuration
@ConfigurationProperties(prefix = "netty.websocket")
public class NettyConfig {
    
    /** WebSocket 端口 */
    private int port = 8090;
    
    /** Boss 线程数 */
    private int bossThreads = 1;
    
    /** Worker 线程数，0表示使用默认值（CPU核心数*2） */
    private int workerThreads = 0;
    
    /** 最大消息大小（字节） */
    private int maxFrameSize = 65536;
    
    /** 读超时时间（秒） */
    private int readTimeout = 60;

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public int getBossThreads() {
        return bossThreads;
    }

    public void setBossThreads(int bossThreads) {
        this.bossThreads = bossThreads;
    }

    public int getWorkerThreads() {
        return workerThreads;
    }

    public void setWorkerThreads(int workerThreads) {
        this.workerThreads = workerThreads;
    }

    public int getMaxFrameSize() {
        return maxFrameSize;
    }

    public void setMaxFrameSize(int maxFrameSize) {
        this.maxFrameSize = maxFrameSize;
    }

    public int getReadTimeout() {
        return readTimeout;
    }

    public void setReadTimeout(int readTimeout) {
        this.readTimeout = readTimeout;
    }
}
