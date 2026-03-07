-- ============================================================
-- SpeakMaster 完整数据库初始化脚本
-- 包含所有微服务的库表创建 + 初始化数据
-- 执行方式: docker exec -i speakmaster-mysql mysql -uroot -proot --default-character-set=utf8mb4 < environment/scripts/init.sql/init.sql
-- ============================================================

-- 强制使用 utf8mb4 字符集，防止中文乱码
SET NAMES utf8mb4;
SET CHARACTER SET utf8mb4;
SET character_set_connection = utf8mb4;

-- ========================================
-- 1. 创建所有数据库
-- ========================================
CREATE DATABASE IF NOT EXISTS speakmaster_user DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE DATABASE IF NOT EXISTS speakmaster_practice DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE DATABASE IF NOT EXISTS speakmaster_meeting DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE DATABASE IF NOT EXISTS speakmaster_community DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE DATABASE IF NOT EXISTS speakmaster_analysis DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE DATABASE IF NOT EXISTS speakmaster_notification DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE DATABASE IF NOT EXISTS speakmaster_admin DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE DATABASE IF NOT EXISTS speakmaster_ai DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

-- ========================================
-- 2. 用户服务 (speakmaster_user)
-- ========================================
USE speakmaster_user;

-- 用户表
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
  KEY `idx_phone` (`phone`),
  KEY `idx_deleted` (`deleted`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户表';

-- 积分记录表
CREATE TABLE IF NOT EXISTS `points_record` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `user_id` bigint NOT NULL COMMENT '用户ID',
  `points_change` bigint NOT NULL COMMENT '积分变动',
  `points_after` bigint NOT NULL COMMENT '变动后积分',
  `reason` varchar(255) DEFAULT NULL COMMENT '变动原因',
  `business_type` int DEFAULT 0 COMMENT '业务类型(1-签到,2-练习,3-Meeting,4-社区,5-其他)',
  `business_id` bigint DEFAULT NULL COMMENT '关联业务ID',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  `deleted` int DEFAULT 0 COMMENT '删除标记',
  PRIMARY KEY (`id`),
  KEY `idx_user_id` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='积分记录表';

-- 签到记录表 (签到逻辑主要在Redis，此表做持久化备份)
CREATE TABLE IF NOT EXISTS `sign_in_record` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `user_id` bigint NOT NULL COMMENT '用户ID',
  `sign_date` varchar(20) NOT NULL COMMENT '签到日期',
  `consecutive_days` int DEFAULT 1 COMMENT '连续签到天数',
  `points_earned` bigint DEFAULT 0 COMMENT '获得积分',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  `deleted` int DEFAULT 0 COMMENT '删除标记',
  PRIMARY KEY (`id`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_sign_date` (`sign_date`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='签到记录表';

-- 徽章表
CREATE TABLE IF NOT EXISTS `badge` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `name` varchar(50) NOT NULL COMMENT '徽章名称',
  `description` varchar(255) DEFAULT NULL COMMENT '徽章描述',
  `icon` varchar(255) DEFAULT NULL COMMENT '徽章图标URL',
  `type` int DEFAULT 1 COMMENT '徽章类型(1-成就,2-等级,3-活动)',
  `condition_desc` varchar(255) DEFAULT NULL COMMENT '获取条件描述',
  `required_points` bigint DEFAULT 0 COMMENT '所需积分',
  `sort_order` int DEFAULT 0 COMMENT '排序',
  `status` int DEFAULT 1 COMMENT '状态(0-禁用,1-启用)',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  `deleted` int DEFAULT 0 COMMENT '删除标记',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='徽章表';

-- 用户徽章关联表
CREATE TABLE IF NOT EXISTS `user_badge` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `user_id` bigint NOT NULL COMMENT '用户ID',
  `badge_id` bigint NOT NULL COMMENT '徽章ID',
  `obtained_time` varchar(30) DEFAULT NULL COMMENT '获得时间',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  `deleted` int DEFAULT 0 COMMENT '删除标记',
  PRIMARY KEY (`id`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_badge_id` (`badge_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户徽章关联表';


-- ========================================
-- 3. 练习服务 (speakmaster_practice)
-- ========================================
USE speakmaster_practice;

-- 主题表
CREATE TABLE IF NOT EXISTS `theme` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `name` varchar(100) NOT NULL COMMENT '主题名称',
  `description` varchar(500) DEFAULT NULL COMMENT '主题描述',
  `cover` varchar(255) DEFAULT NULL COMMENT '封面图URL',
  `category` int DEFAULT 1 COMMENT '分类(1-日常,2-商务,3-旅行,4-考试)',
  `difficulty` int DEFAULT 1 COMMENT '难度(1-初级,2-中级,3-高级)',
  `tags` varchar(255) DEFAULT NULL COMMENT '标签(逗号分隔)',
  `use_count` bigint DEFAULT 0 COMMENT '使用次数',
  `sort_order` int DEFAULT 0 COMMENT '排序',
  `status` int DEFAULT 0 COMMENT '状态(0-草稿,1-发布)',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  `deleted` int DEFAULT 0 COMMENT '删除标记',
  PRIMARY KEY (`id`),
  KEY `idx_category` (`category`),
  KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='练习主题表';

-- 角色表
CREATE TABLE IF NOT EXISTS `role` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `name` varchar(100) NOT NULL COMMENT '角色名称',
  `description` varchar(500) DEFAULT NULL COMMENT '角色描述',
  `avatar` varchar(255) DEFAULT NULL COMMENT '角色头像URL',
  `type` int DEFAULT 1 COMMENT '类型(1-预制,2-自定义)',
  `creator_id` bigint DEFAULT NULL COMMENT '创建者ID(自定义角色)',
  `setting` text DEFAULT NULL COMMENT '角色设定(性格、背景等)',
  `use_count` bigint DEFAULT 0 COMMENT '使用次数',
  `status` int DEFAULT 1 COMMENT '状态(0-禁用,1-启用)',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  `deleted` int DEFAULT 0 COMMENT '删除标记',
  PRIMARY KEY (`id`),
  KEY `idx_type` (`type`),
  KEY `idx_creator_id` (`creator_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='练习角色表';

-- 会话表
CREATE TABLE IF NOT EXISTS `session` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `user_id` bigint NOT NULL COMMENT '用户ID',
  `theme_id` bigint DEFAULT NULL COMMENT '主题ID',
  `role_id` bigint DEFAULT NULL COMMENT '角色ID',
  `title` varchar(200) DEFAULT NULL COMMENT '会话标题',
  `start_time` varchar(30) DEFAULT NULL COMMENT '开始时间',
  `end_time` varchar(30) DEFAULT NULL COMMENT '结束时间',
  `message_count` int DEFAULT 0 COMMENT '消息数量',
  `total_score` double DEFAULT NULL COMMENT '总分数',
  `avg_score` double DEFAULT NULL COMMENT '平均分数',
  `status` int DEFAULT 0 COMMENT '状态(0-进行中,1-已结束)',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  `deleted` int DEFAULT 0 COMMENT '删除标记',
  PRIMARY KEY (`id`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_theme_id` (`theme_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='练习会话表';

-- 消息表
CREATE TABLE IF NOT EXISTS `message` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `session_id` bigint NOT NULL COMMENT '会话ID',
  `sender_type` int DEFAULT 1 COMMENT '发送者类型(1-用户,2-AI)',
  `content` text DEFAULT NULL COMMENT '消息内容',
  `audio_url` varchar(255) DEFAULT NULL COMMENT '音频URL',
  `score` int DEFAULT NULL COMMENT '评分(1-5)',
  `feedback` text DEFAULT NULL COMMENT '反馈建议',
  `example_audio_url` varchar(255) DEFAULT NULL COMMENT '示例音频URL',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  `deleted` int DEFAULT 0 COMMENT '删除标记',
  PRIMARY KEY (`id`),
  KEY `idx_session_id` (`session_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='练习消息表';


-- ========================================
-- 4. 会议服务 (speakmaster_meeting)
-- ========================================
USE speakmaster_meeting;

-- 会议房间表
CREATE TABLE IF NOT EXISTS `meeting` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `name` varchar(100) NOT NULL COMMENT '房间名称',
  `description` varchar(500) DEFAULT NULL COMMENT '房间描述',
  `creator_id` bigint NOT NULL COMMENT '创建者ID',
  `theme_id` bigint DEFAULT NULL COMMENT '主题ID',
  `max_participants` int DEFAULT 4 COMMENT '最大人数',
  `current_participants` int DEFAULT 0 COMMENT '当前人数',
  `status` int DEFAULT 0 COMMENT '状态(0-等待中,1-进行中,2-已结束)',
  `start_time` varchar(30) DEFAULT NULL COMMENT '开始时间',
  `end_time` varchar(30) DEFAULT NULL COMMENT '结束时间',
  `is_public` int DEFAULT 0 COMMENT '是否公开(0-私密,1-公开)',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  `deleted` int DEFAULT 0 COMMENT '删除标记',
  PRIMARY KEY (`id`),
  KEY `idx_creator_id` (`creator_id`),
  KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='会议房间表';

-- 会议参与者表
CREATE TABLE IF NOT EXISTS `meeting_participant` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `meeting_id` bigint NOT NULL COMMENT '会议ID',
  `user_id` bigint NOT NULL COMMENT '用户ID',
  `role` int DEFAULT 0 COMMENT '角色(0-参与者,1-主持人)',
  `join_time` varchar(30) DEFAULT NULL COMMENT '加入时间',
  `leave_time` varchar(30) DEFAULT NULL COMMENT '离开时间',
  `status` int DEFAULT 0 COMMENT '状态(0-在线,1-离线)',
  `ai_role_name` varchar(100) DEFAULT NULL COMMENT 'AI角色名称',
  `ai_role_setting` text DEFAULT NULL COMMENT 'AI角色设定(性格、背景等)',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  `deleted` int DEFAULT 0 COMMENT '删除标记',
  PRIMARY KEY (`id`),
  KEY `idx_meeting_id` (`meeting_id`),
  KEY `idx_user_id` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='会议参与者表';

-- 会议消息表
CREATE TABLE IF NOT EXISTS `meeting_message` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `meeting_id` bigint NOT NULL COMMENT '会议ID',
  `sender_id` bigint NOT NULL COMMENT '发送者ID',
  `message_type` int DEFAULT 0 COMMENT '消息类型(0-文本,1-音频,2-系统)',
  `content` text DEFAULT NULL COMMENT '消息内容',
  `audio_url` varchar(255) DEFAULT NULL COMMENT '音频URL',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  `deleted` int DEFAULT 0 COMMENT '删除标记',
  PRIMARY KEY (`id`),
  KEY `idx_meeting_id` (`meeting_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='会议消息表';

-- 好友表
CREATE TABLE IF NOT EXISTS `friend` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `user_id` bigint NOT NULL COMMENT '用户ID',
  `friend_id` bigint NOT NULL COMMENT '好友ID',
  `status` int DEFAULT 0 COMMENT '状态(0-待确认,1-已接受,2-已拒绝)',
  `remark` varchar(50) DEFAULT NULL COMMENT '备注名称',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  `deleted` int DEFAULT 0 COMMENT '删除标记',
  PRIMARY KEY (`id`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_friend_id` (`friend_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='好友表';


-- ========================================
-- 5. 社区服务 (speakmaster_community)
-- ========================================
USE speakmaster_community;

-- 帖子表
CREATE TABLE IF NOT EXISTS `post` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `title` varchar(200) NOT NULL COMMENT '标题',
  `content` text DEFAULT NULL COMMENT '内容',
  `author_id` bigint NOT NULL COMMENT '作者ID',
  `category` varchar(50) DEFAULT NULL COMMENT '分类',
  `tags` varchar(255) DEFAULT NULL COMMENT '标签',
  `cover_image` varchar(255) DEFAULT NULL COMMENT '封面图片URL',
  `like_count` int DEFAULT 0 COMMENT '点赞数',
  `comment_count` int DEFAULT 0 COMMENT '评论数',
  `view_count` int DEFAULT 0 COMMENT '浏览数',
  `favorite_count` int DEFAULT 0 COMMENT '收藏数',
  `is_pinned` int DEFAULT 0 COMMENT '是否置顶(0-否,1-是)',
  `is_hidden` int DEFAULT 0 COMMENT '是否隐藏(0-否,1-是)',
  `status` int DEFAULT 1 COMMENT '状态(0-草稿,1-已发布)',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  `deleted` int DEFAULT 0 COMMENT '删除标记',
  PRIMARY KEY (`id`),
  KEY `idx_author_id` (`author_id`),
  KEY `idx_category` (`category`),
  KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='帖子表';

-- 评论表
CREATE TABLE IF NOT EXISTS `comment` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `post_id` bigint NOT NULL COMMENT '帖子ID',
  `user_id` bigint NOT NULL COMMENT '评论者ID',
  `parent_id` bigint DEFAULT NULL COMMENT '父评论ID(回复功能)',
  `content` text NOT NULL COMMENT '评论内容',
  `like_count` int DEFAULT 0 COMMENT '点赞数',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  `deleted` int DEFAULT 0 COMMENT '删除标记',
  PRIMARY KEY (`id`),
  KEY `idx_post_id` (`post_id`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_parent_id` (`parent_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='评论表';

-- 帖子点赞表
CREATE TABLE IF NOT EXISTS `post_like` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `post_id` bigint NOT NULL COMMENT '帖子ID',
  `user_id` bigint NOT NULL COMMENT '用户ID',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  `deleted` int DEFAULT 0 COMMENT '删除标记',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_post_user` (`post_id`, `user_id`),
  KEY `idx_user_id` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='帖子点赞表';

-- 帖子收藏表
CREATE TABLE IF NOT EXISTS `post_favorite` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `post_id` bigint NOT NULL COMMENT '帖子ID',
  `user_id` bigint NOT NULL COMMENT '用户ID',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  `deleted` int DEFAULT 0 COMMENT '删除标记',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_post_user` (`post_id`, `user_id`),
  KEY `idx_user_id` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='帖子收藏表';


-- ========================================
-- 6. 通知服务 (speakmaster_notification)
-- ========================================
USE speakmaster_notification;

-- 通知表
CREATE TABLE IF NOT EXISTS `notification` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `receiver_id` bigint NOT NULL COMMENT '接收者ID',
  `sender_id` bigint DEFAULT NULL COMMENT '发送者ID(系统通知为null)',
  `type` int DEFAULT 0 COMMENT '通知类型(0-系统,1-点赞,2-评论,3-关注,4-私信)',
  `title` varchar(200) DEFAULT NULL COMMENT '标题',
  `content` text DEFAULT NULL COMMENT '内容',
  `related_id` bigint DEFAULT NULL COMMENT '关联ID(帖子ID、评论ID等)',
  `is_read` int DEFAULT 0 COMMENT '是否已读(0-未读,1-已读)',
  `read_time` varchar(30) DEFAULT NULL COMMENT '读取时间',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  `deleted` int DEFAULT 0 COMMENT '删除标记',
  PRIMARY KEY (`id`),
  KEY `idx_receiver_id` (`receiver_id`),
  KEY `idx_type` (`type`),
  KEY `idx_is_read` (`is_read`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='通知表';

-- ========================================
-- 7. 管理服务 (speakmaster_admin)
-- ========================================
USE speakmaster_admin;

-- 系统配置表
CREATE TABLE IF NOT EXISTS `system_config` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `config_key` varchar(100) NOT NULL COMMENT '配置键',
  `config_value` text DEFAULT NULL COMMENT '配置值',
  `description` varchar(255) DEFAULT NULL COMMENT '配置描述',
  `category` varchar(50) DEFAULT NULL COMMENT '配置分类',
  `is_enabled` int DEFAULT 1 COMMENT '是否启用(0-禁用,1-启用)',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  `deleted` int DEFAULT 0 COMMENT '删除标记',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_config_key` (`config_key`),
  KEY `idx_category` (`category`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='系统配置表';

-- 系统日志表
CREATE TABLE IF NOT EXISTS `system_log` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `user_id` bigint DEFAULT NULL COMMENT '操作用户ID',
  `module` varchar(50) DEFAULT NULL COMMENT '操作模块',
  `operation` varchar(50) DEFAULT NULL COMMENT '操作类型',
  `description` varchar(500) DEFAULT NULL COMMENT '操作描述',
  `method` varchar(200) DEFAULT NULL COMMENT '请求方法',
  `url` varchar(500) DEFAULT NULL COMMENT '请求URL',
  `params` text DEFAULT NULL COMMENT '请求参数',
  `ip` varchar(50) DEFAULT NULL COMMENT 'IP地址',
  `execution_time` bigint DEFAULT NULL COMMENT '执行时间(ms)',
  `status` int DEFAULT NULL COMMENT '状态(0-失败,1-成功)',
  `error_msg` text DEFAULT NULL COMMENT '错误信息',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  `deleted` int DEFAULT 0 COMMENT '删除标记',
  PRIMARY KEY (`id`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_module` (`module`),
  KEY `idx_create_time` (`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='系统日志表';


-- ========================================
-- 8. AI网关服务 (speakmaster_ai)
-- ========================================
USE speakmaster_ai;

-- AI模型配置表
CREATE TABLE IF NOT EXISTS `ai_model_config` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `name` varchar(100) NOT NULL COMMENT '模型名称',
  `provider_type` varchar(30) NOT NULL COMMENT '提供方类型(OLLAMA/REMOTE_API/LOCAL)',
  `model_id` varchar(100) DEFAULT NULL COMMENT '模型标识(如gpt-4o,llama3)',
  `endpoint` varchar(500) DEFAULT NULL COMMENT '服务端点URL',
  `api_key` varchar(255) DEFAULT NULL COMMENT 'API密钥',
  `enabled` tinyint(1) DEFAULT 1 COMMENT '是否启用',
  `recommended` tinyint(1) DEFAULT 0 COMMENT '是否推荐',
  `weight` int DEFAULT 1 COMMENT '路由权重',
  `priority` int DEFAULT 10 COMMENT '优先级(数值越小越高)',
  `max_tokens` int DEFAULT 2048 COMMENT '最大Token数',
  `temperature` double DEFAULT 0.7 COMMENT '温度参数',
  `timeout` int DEFAULT 30 COMMENT '超时时间(秒)',
  `description` varchar(500) DEFAULT NULL COMMENT '模型描述',
  `healthy` tinyint(1) DEFAULT 1 COMMENT '健康状态',
  `multimodal` tinyint(1) DEFAULT 0 COMMENT '是否支持多模态(音频/图片)',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  `deleted` int DEFAULT 0 COMMENT '删除标记',
  PRIMARY KEY (`id`),
  KEY `idx_provider_type` (`provider_type`),
  KEY `idx_enabled` (`enabled`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='AI模型配置表';

-- AI路由规则表
CREATE TABLE IF NOT EXISTS `ai_routing_rule` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `name` varchar(100) NOT NULL COMMENT '规则名称',
  `strategy` varchar(30) NOT NULL COMMENT '路由策略(WEIGHT/PRIORITY/ROUND_ROBIN)',
  `enabled` tinyint(1) DEFAULT 1 COMMENT '是否启用',
  `priority` int DEFAULT 10 COMMENT '规则优先级',
  `description` varchar(500) DEFAULT NULL COMMENT '规则描述',
  `model_ids` varchar(500) DEFAULT NULL COMMENT '关联模型ID列表(JSON数组)',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  `deleted` int DEFAULT 0 COMMENT '删除标记',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='AI路由规则表';

-- ========================================
-- 9. 分析服务 (speakmaster_analysis)
--    注: analysis_service 使用 SQLAlchemy 会自动建表，
--    此处提供SQL以便手动初始化或备用
-- ========================================
USE speakmaster_analysis;

-- 分析报告表
CREATE TABLE IF NOT EXISTS `analysis_report` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `session_id` bigint NOT NULL COMMENT '会话ID',
  `message_id` bigint DEFAULT NULL COMMENT '消息ID',
  `user_id` bigint NOT NULL COMMENT '用户ID',
  `grammar_score` float DEFAULT 0 COMMENT '语法评分',
  `pronunciation_score` float DEFAULT 0 COMMENT '发音评分',
  `fluency_score` float DEFAULT 0 COMMENT '流利度评分',
  `overall_score` float DEFAULT 0 COMMENT '综合评分',
  `native_expression_suggestion` text DEFAULT NULL COMMENT '地道表达建议',
  `detailed_feedback` json DEFAULT NULL COMMENT '详细反馈JSON',
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `deleted` tinyint(1) DEFAULT 0 COMMENT '逻辑删除',
  PRIMARY KEY (`id`),
  KEY `idx_session_id` (`session_id`),
  KEY `idx_user_id` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='分析报告表';

-- 学习记录表
CREATE TABLE IF NOT EXISTS `learning_record` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `user_id` bigint NOT NULL COMMENT '用户ID',
  `record_date` varchar(10) NOT NULL COMMENT '记录日期(yyyy-MM-dd)',
  `practice_count` int DEFAULT 0 COMMENT '练习次数',
  `practice_duration` int DEFAULT 0 COMMENT '练习时长(秒)',
  `avg_grammar_score` float DEFAULT 0 COMMENT '平均语法分',
  `avg_pronunciation_score` float DEFAULT 0 COMMENT '平均发音分',
  `avg_fluency_score` float DEFAULT 0 COMMENT '平均流利度分',
  `avg_overall_score` float DEFAULT 0 COMMENT '平均综合分',
  `top_errors` json DEFAULT NULL COMMENT '常见错误Top5',
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_user_date` (`user_id`, `record_date`),
  KEY `idx_user_id` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='学习记录表';

-- 用户排名表
CREATE TABLE IF NOT EXISTS `user_ranking` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `user_id` bigint NOT NULL COMMENT '用户ID',
  `total_practice_count` int DEFAULT 0 COMMENT '总练习次数',
  `total_practice_duration` int DEFAULT 0 COMMENT '总练习时长(秒)',
  `avg_overall_score` float DEFAULT 0 COMMENT '平均综合分',
  `ranking_score` float DEFAULT 0 COMMENT '排名分数(加权计算)',
  `current_rank` int DEFAULT 0 COMMENT '当前排名',
  `updated_at` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_user_id` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户排名表';


-- ============================================================
-- 10. 初始化数据
-- ============================================================

-- ---------- 用户服务初始化数据 ----------
USE speakmaster_user;

-- 默认管理员 (密码: admin123)
INSERT INTO `user` (`username`, `password`, `nickname`, `email`, `gender`, `points`, `status`, `create_time`, `update_time`, `deleted`)
VALUES ('admin', '$2a$10$vH5jYnsVAM4LyjRjrUyyEOH0yCDIhivhye82P.Qmi6N0nHym6Mh26', '系统管理员', 'admin@speakmaster.com', 0, 0, 0, NOW(), NOW(), 0)
ON DUPLICATE KEY UPDATE username = username;

-- 默认徽章
INSERT IGNORE INTO `badge` (`name`, `description`, `icon`, `type`, `condition_desc`, `required_points`, `sort_order`, `status`, `create_time`, `update_time`, `deleted`) VALUES
('新手上路', '完成首次练习', '/badges/beginner.png', 1, '完成1次口语练习', 0, 1, 1, NOW(), NOW(), 0),
('勤学苦练', '累计完成10次练习', '/badges/diligent.png', 1, '累计完成10次口语练习', 0, 2, 1, NOW(), NOW(), 0),
('口语达人', '累计完成50次练习', '/badges/master.png', 1, '累计完成50次口语练习', 0, 3, 1, NOW(), NOW(), 0),
('社交之星', '发布10篇帖子', '/badges/social.png', 1, '在社区发布10篇帖子', 0, 4, 1, NOW(), NOW(), 0),
('签到达人', '连续签到7天', '/badges/checkin.png', 1, '连续签到7天', 0, 5, 1, NOW(), NOW(), 0),
('积分富翁', '积分达到1000', '/badges/rich.png', 2, '累计积分达到1000', 1000, 6, 1, NOW(), NOW(), 0),
('学霸', '平均评分达到4.5', '/badges/scholar.png', 1, '练习平均评分达到4.5分', 0, 7, 1, NOW(), NOW(), 0),
('会议之王', '参加20次Meeting', '/badges/meeting.png', 1, '参加20次语音会议', 0, 8, 1, NOW(), NOW(), 0);

-- ---------- 练习服务初始化数据 ----------
USE speakmaster_practice;

-- 默认主题
INSERT IGNORE INTO `theme` (`name`, `description`, `cover`, `category`, `difficulty`, `tags`, `use_count`, `sort_order`, `status`, `create_time`, `update_time`, `deleted`) VALUES
('日常问候', '学习日常英语问候和寒暄用语', '/themes/greeting.png', 1, 1, '日常,问候,入门', 0, 1, 1, NOW(), NOW(), 0),
('餐厅点餐', '模拟在餐厅点餐的场景对话', '/themes/restaurant.png', 1, 1, '日常,餐厅,点餐', 0, 2, 1, NOW(), NOW(), 0),
('商务会议', '模拟商务会议中的英语交流', '/themes/business.png', 2, 2, '商务,会议,职场', 0, 3, 1, NOW(), NOW(), 0),
('求职面试', '模拟英语求职面试场景', '/themes/interview.png', 2, 3, '商务,面试,求职', 0, 4, 1, NOW(), NOW(), 0),
('机场出行', '模拟机场值机、安检、登机等场景', '/themes/airport.png', 3, 1, '旅行,机场,出行', 0, 5, 1, NOW(), NOW(), 0),
('酒店入住', '模拟酒店预订和入住场景', '/themes/hotel.png', 3, 1, '旅行,酒店,住宿', 0, 6, 1, NOW(), NOW(), 0),
('雅思口语Part1', '雅思口语考试Part1常见话题练习', '/themes/ielts1.png', 4, 2, '考试,雅思,口语', 0, 7, 1, NOW(), NOW(), 0),
('雅思口语Part2', '雅思口语考试Part2话题卡练习', '/themes/ielts2.png', 4, 3, '考试,雅思,口语', 0, 8, 1, NOW(), NOW(), 0),
('购物砍价', '模拟商场购物和讨价还价场景', '/themes/shopping.png', 1, 1, '日常,购物,砍价', 0, 9, 1, NOW(), NOW(), 0),
('看病就医', '模拟去医院看病的英语对话', '/themes/hospital.png', 1, 2, '日常,医院,就医', 0, 10, 1, NOW(), NOW(), 0);

-- 默认角色
INSERT IGNORE INTO `role` (`name`, `description`, `avatar`, `type`, `creator_id`, `setting`, `use_count`, `status`, `create_time`, `update_time`, `deleted`) VALUES
('Emily', '友善的美国女孩，擅长日常对话', '/roles/emily.png', 1, NULL, '你是Emily，一个来自纽约的25岁女孩，性格开朗友善，喜欢聊天。说话风格轻松自然，会使用一些美式口语表达。', 0, 1, NOW(), NOW(), 0),
('James', '专业的商务人士，擅长职场英语', '/roles/james.png', 1, NULL, '你是James，一个35岁的英国商务顾问，说话专业严谨，擅长商务英语和正式场合的表达。', 0, 1, NOW(), NOW(), 0),
('Sarah', '耐心的英语老师，擅长纠正发音', '/roles/sarah.png', 1, NULL, '你是Sarah，一个经验丰富的英语老师，说话清晰缓慢，会耐心纠正学生的语法和发音错误，给出详细的改进建议。', 0, 1, NOW(), NOW(), 0),
('Mike', '幽默的澳洲小伙，擅长旅行话题', '/roles/mike.png', 1, NULL, '你是Mike，一个28岁的澳大利亚背包客，性格幽默风趣，热爱旅行，会分享各种旅行经历和实用建议。', 0, 1, NOW(), NOW(), 0),
('Lisa', '严格的雅思考官，模拟考试场景', '/roles/lisa.png', 1, NULL, '你是Lisa，一位资深雅思口语考官，会按照雅思口语考试的标准流程提问，语速适中，态度专业。', 0, 1, NOW(), NOW(), 0);


-- ---------- 管理服务初始化数据 ----------
USE speakmaster_admin;

-- 默认系统配置
INSERT IGNORE INTO `system_config` (`config_key`, `config_value`, `description`, `category`, `is_enabled`, `create_time`, `update_time`, `deleted`) VALUES
('site.name', 'SpeakMaster', '站点名称', 'basic', 1, NOW(), NOW(), 0),
('site.description', '智能英语口语练习平台', '站点描述', 'basic', 1, NOW(), NOW(), 0),
('user.default_avatar', '/default/avatar.png', '用户默认头像', 'user', 1, NOW(), NOW(), 0),
('user.register_enabled', 'true', '是否开放注册', 'user', 1, NOW(), NOW(), 0),
('points.sign_in', '10', '每日签到积分', 'points', 1, NOW(), NOW(), 0),
('points.practice', '5', '每次练习积分', 'points', 1, NOW(), NOW(), 0),
('points.post', '3', '发帖积分', 'points', 1, NOW(), NOW(), 0),
('meeting.max_participants', '8', '会议最大参与人数', 'meeting', 1, NOW(), NOW(), 0),
('ai.default_model', 'ollama', '默认AI模型', 'ai', 1, NOW(), NOW(), 0),
('ai.max_tokens', '2048', 'AI最大Token数', 'ai', 1, NOW(), NOW(), 0);

-- ---------- AI网关初始化数据 ----------
USE speakmaster_ai;

-- 默认Ollama模型配置
INSERT IGNORE INTO `ai_model_config` (`name`, `provider_type`, `model_id`, `endpoint`, `api_key`, `enabled`, `recommended`, `weight`, `priority`, `max_tokens`, `temperature`, `timeout`, `description`, `healthy`, `create_time`, `update_time`, `deleted`) VALUES
('Ollama Llama3', 'OLLAMA', 'llama3', 'http://localhost:11434', NULL, 1, 1, 1, 1, 2048, 0.7, 60, '本地Ollama Llama3模型', 1, NOW(), NOW(), 0);

-- 默认路由规则
INSERT IGNORE INTO `ai_routing_rule` (`name`, `strategy`, `enabled`, `priority`, `description`, `model_ids`, `create_time`, `update_time`, `deleted`) VALUES
('默认路由', 'PRIORITY', 1, 1, '按优先级路由到可用模型', '[1]', NOW(), NOW(), 0);

-- ============================================================
-- 初始化完成
-- ============================================================


-- ==================== 三层记忆管理系统表 ====================
USE speakmaster_meeting;

-- 对话总结表(中期记忆)
CREATE TABLE IF NOT EXISTS conversation_summary (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL COMMENT '用户ID',
    session_id VARCHAR(100) NOT NULL COMMENT '会话ID',
    session_type VARCHAR(20) NOT NULL COMMENT '会话类型: meeting/practice',
    summary TEXT NOT NULL COMMENT '对话总结',
    keywords VARCHAR(500) COMMENT '关键词(逗号分隔)',
    start_time DATETIME COMMENT '对话开始时间',
    end_time DATETIME COMMENT '对话结束时间',
    message_count INT DEFAULT 0 COMMENT '消息数量',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted TINYINT DEFAULT 0 COMMENT '逻辑删除: 0-未删除, 1-已删除',
    INDEX idx_user (user_id),
    INDEX idx_session (session_id),
    INDEX idx_type (session_type),
    INDEX idx_create_time (create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='对话总结表(中期记忆)';

-- 用户画像表(长期记忆)
CREATE TABLE IF NOT EXISTS user_profile (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL UNIQUE COMMENT '用户ID',
    english_level VARCHAR(20) DEFAULT 'intermediate' COMMENT '英语水平: beginner/intermediate/advanced',
    learning_goals TEXT COMMENT '学习目标',
    interests TEXT COMMENT '兴趣话题(JSON数组)',
    common_mistakes TEXT COMMENT '常见错误类型(JSON数组)',
    pronunciation_weaknesses TEXT COMMENT '发音弱点(JSON数组)',
    learning_style VARCHAR(50) COMMENT '学习风格',
    total_practice_minutes INT DEFAULT 0 COMMENT '总练习时长(分钟)',
    total_conversations INT DEFAULT 0 COMMENT '总对话轮数',
    personalized_prompt TEXT COMMENT '个性化提示词',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted TINYINT DEFAULT 0 COMMENT '逻辑删除: 0-未删除, 1-已删除',
    INDEX idx_user (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户画像表(长期记忆)';

-- 插入测试用户画像
INSERT IGNORE INTO user_profile (user_id, english_level, learning_goals, interests, common_mistakes, pronunciation_weaknesses, learning_style, total_practice_minutes, total_conversations, personalized_prompt)
VALUES 
(1, 'intermediate', '提高英语口语流利度,准备雅思考试', '["旅游", "科技", "电影"]', '["时态错误", "冠词使用"]', '["th音", "r/l区分"]', 'interactive', 120, 15, '请多使用日常对话场景,注意纠正我的时态错误'),
(2, 'beginner', '学习基础英语对话', '["音乐", "美食"]', '["发音不准", "词汇量小"]', '["v/w音", "重音位置"]', 'structured', 60, 8, '请使用简单词汇,多重复关键句型');
