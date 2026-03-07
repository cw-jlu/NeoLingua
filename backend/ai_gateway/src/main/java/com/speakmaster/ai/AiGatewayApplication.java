package com.speakmaster.ai;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.data.redis.RedisRepositoriesAutoConfiguration;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * AI网关启动类
 * 
 * @author SpeakMaster
 */
@SpringBootApplication(
    scanBasePackages = {"com.speakmaster.ai", "com.speakmaster.common"},
    exclude = {RedisRepositoriesAutoConfiguration.class}
)
@EnableScheduling
public class AiGatewayApplication {

    public static void main(String[] args) {
        SpringApplication.run(AiGatewayApplication.class, args);
        System.out.println("========================================");
        System.out.println("AI Gateway 启动成功!");
        System.out.println("========================================");
    }
}
