package com.speakmaster.meeting;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.data.redis.RedisRepositoriesAutoConfiguration;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Meeting服务启动类
 * 
 * @author SpeakMaster
 */
@SpringBootApplication(
    scanBasePackages = {"com.speakmaster.meeting", "com.speakmaster.common"},
    exclude = {RedisRepositoriesAutoConfiguration.class}
)
@EnableFeignClients
@EnableCaching
@EnableAsync
@EnableScheduling
public class MeetingServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(MeetingServiceApplication.class, args);
        System.out.println("========================================");
        System.out.println("Meeting Service 启动成功!");
        System.out.println("========================================");
    }
}
