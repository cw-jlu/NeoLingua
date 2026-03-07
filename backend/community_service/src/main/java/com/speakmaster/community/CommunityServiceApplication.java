package com.speakmaster.community;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.data.redis.RedisRepositoriesAutoConfiguration;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.data.elasticsearch.repository.config.EnableElasticsearchRepositories;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * 社区服务启动类
 * 
 * @author SpeakMaster
 */
@SpringBootApplication(
    scanBasePackages = {"com.speakmaster.community", "com.speakmaster.common"},
    exclude = {RedisRepositoriesAutoConfiguration.class}
)
@EnableCaching
@EnableScheduling
@EnableElasticsearchRepositories(basePackages = "com.speakmaster.community.repository")
public class CommunityServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(CommunityServiceApplication.class, args);
        System.out.println("========================================");
        System.out.println("Community Service 启动成功!");
        System.out.println("========================================");
    }
}
