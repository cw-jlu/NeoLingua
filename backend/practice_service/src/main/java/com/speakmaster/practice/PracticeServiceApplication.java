package com.speakmaster.practice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.data.redis.RedisRepositoriesAutoConfiguration;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cloud.openfeign.EnableFeignClients;

/**
 * 练习服务启动类
 * 
 * @author SpeakMaster
 */
@SpringBootApplication(
    scanBasePackages = {"com.speakmaster.practice", "com.speakmaster.common"},
    exclude = {RedisRepositoriesAutoConfiguration.class}
)
@EnableCaching
@EnableFeignClients
public class PracticeServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(PracticeServiceApplication.class, args);
        System.out.println("========================================");
        System.out.println("Practice Service 启动成功!");
        System.out.println("========================================");
    }
}
