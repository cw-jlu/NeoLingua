package com.speakmaster.notification;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.data.redis.RedisRepositoriesAutoConfiguration;
import org.springframework.kafka.annotation.EnableKafka;

/**
 * 通知服务启动类
 * 
 * @author SpeakMaster
 */
@SpringBootApplication(
    scanBasePackages = {"com.speakmaster.notification", "com.speakmaster.common"},
    exclude = {RedisRepositoriesAutoConfiguration.class}
)
@EnableKafka
public class NotificationServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(NotificationServiceApplication.class, args);
        System.out.println("========================================");
        System.out.println("Notification Service 启动成功!");
        System.out.println("========================================");
    }
}
