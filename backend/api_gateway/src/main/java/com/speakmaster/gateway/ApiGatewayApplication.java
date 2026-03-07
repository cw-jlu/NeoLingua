package com.speakmaster.gateway;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * API网关启动�?
 * 
 * @author SpeakMaster
 */
@SpringBootApplication
public class ApiGatewayApplication {

    public static void main(String[] args) {
        SpringApplication.run(ApiGatewayApplication.class, args);
        System.out.println("========================================");
        System.out.println("API网关启动成功!");
        System.out.println("========================================");
    }
}
