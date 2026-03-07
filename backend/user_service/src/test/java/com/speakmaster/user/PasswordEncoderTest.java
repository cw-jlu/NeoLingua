package com.speakmaster.user;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

/**
 * 密码加密工具 - 用于生成默认管理员密码
 */
public class PasswordEncoderTest {
    
    public static void main(String[] args) {
        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
        
        // 生成 admin123 的 BCrypt 哈希
        String password = "admin123";
        String encoded = encoder.encode(password);
        
        System.out.println("原始密码: " + password);
        System.out.println("BCrypt哈希: " + encoded);
        System.out.println();
        
        // 验证密码
        boolean matches = encoder.matches(password, encoded);
        System.out.println("密码验证: " + (matches ? "成功" : "失败"));
    }
}
