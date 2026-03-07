package com.speakmaster.admin;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.data.redis.RedisRepositoriesAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration;
import org.springframework.boot.autoconfigure.data.jpa.JpaRepositoriesAutoConfiguration;
import org.springframework.boot.autoconfigure.elasticsearch.ElasticsearchRestClientAutoConfiguration;

/**
 * 管理服务启动类
 * 
 * @author SpeakMaster
 */
@SpringBootApplication(
    scanBasePackages = {"com.speakmaster.admin", "com.speakmaster.common"},
    exclude = {
        RedisRepositoriesAutoConfiguration.class,
        JpaRepositoriesAutoConfiguration.class,
        HibernateJpaAutoConfiguration.class,
        ElasticsearchRestClientAutoConfiguration.class
    }
)
public class AdminServiceApplication {

    public static void main(String[] args) {
        System.out.println("========================================");
        System.out.println("正在启动 Admin Service...");
        System.out.println("========================================");
        SpringApplication.run(AdminServiceApplication.class, args);
        System.out.println("========================================");
        System.out.println("Admin Service 启动成功!");
        System.out.println("========================================");
    }
}
