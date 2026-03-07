-- 插入默认管理员账号
-- 用户名: admin
-- 密码: admin123

USE speakmaster_user;

-- 创建用户表（如果不存在）
CREATE TABLE IF NOT EXISTS `user` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `username` varchar(50) NOT NULL COMMENT '用户名',
  `password` varchar(255) NOT NULL COMMENT '密码(加密)',
  `nickname` varchar(50) DEFAULT NULL COMMENT '昵称',
  `email` varchar(100) DEFAULT NULL COMMENT '邮箱',
  `phone` varchar(20) DEFAULT NULL COMMENT '手机号',
  `avatar` varchar(255) DEFAULT NULL COMMENT '头像URL',
  `gender` int DEFAULT 0 COMMENT '性别(0-未知,1-男,2-女)',
  `birthday` varchar(20) DEFAULT NULL COMMENT '生日',
  `bio` varchar(500) DEFAULT NULL COMMENT '个人简介',
  `points` bigint DEFAULT 0 COMMENT '积分',
  `status` int DEFAULT 0 COMMENT '状态(0-正常,1-禁用,2-锁定)',
  `last_login_time` varchar(30) DEFAULT NULL COMMENT '最后登录时间',
  `last_login_ip` varchar(50) DEFAULT NULL COMMENT '最后登录IP',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  `deleted` int DEFAULT 0 COMMENT '删除标记(0-未删除,1-已删除)',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_username` (`username`),
  KEY `idx_email` (`email`),
  KEY `idx_phone` (`phone`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户表';

-- 插入默认管理员账号
-- 密码使用 BCrypt 加密: admin123
-- BCrypt 哈希值是使用 BCryptPasswordEncoder 生成的
-- 注意: 每次运行BCrypt会生成不同的哈希值，但都能验证相同的密码

INSERT INTO `user` (
  `username`, 
  `password`, 
  `nickname`, 
  `email`, 
  `gender`,
  `points`, 
  `status`, 
  `create_time`, 
  `update_time`, 
  `deleted`
) VALUES (
  'admin',
  '$2a$10$N.zmdr9zkzoGtM2uvy1wDus2RkNfCFaR5X7drF2YGZRK6GZnGMnHm',
  '系统管理员',
  'admin@speakmaster.com',
  0,
  0,
  0,
  NOW(),
  NOW(),
  0
) ON DUPLICATE KEY UPDATE 
  `username` = 'admin';

-- 验证插入
SELECT id, username, nickname, email, status, create_time FROM `user` WHERE username = 'admin';

