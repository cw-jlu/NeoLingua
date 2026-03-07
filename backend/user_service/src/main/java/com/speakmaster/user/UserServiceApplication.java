package com.speakmaster.user;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.data.redis.RedisRepositoriesAutoConfiguration;

/**
 * 用户服务启动类
 * 
 * @author SpeakMaster
 */
@SpringBootApplication(
    scanBasePackages = {"com.speakmaster.user", "com.speakmaster.common"},
    exclude = {RedisRepositoriesAutoConfiguration.class}
)
public class UserServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(UserServiceApplication.class, args);
        System.out.println("========================================");
        System.out.println("User Service 启动成功!");
        System.out.println("========================================");
    }
}
