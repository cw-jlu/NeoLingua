# SpeakMaster 用户服务

## 模块说明

用户服务负责用户认证、注册、登录、签到、用户画像等功能。

## 主要功能

### 1. 用户认证

- 用户注册
- 用户登录 (JWT)
- Token刷新
- 用户登出
- 账号注销

### 2. 用户信息管理

- 获取用户信息
- 更新用户信息
- 删除用户
- 积分查询

### 3. 签到功能

使用Redis Bitmap实现高效的签到系统：

- 每日签到 (获得5积分)
- 签到状态查询
- 签到日历
- 签到统计 (连续签到天数、本月签到天数、总签到天数)

### 4. 徽章系统

- 获取徽章列表
- 获取徽章详情
- 领取徽章

### 5. 积分系统

- 积分查询
- 积分增加
- 积分扣除
- 积分记录

## API接口

### 认证接口

| 方法 | 路径 | 说明 |
|------|------|------|
| POST | `/user/auth/register` | 用户注册 |
| POST | `/user/auth/login` | 用户登录 |
| POST | `/user/auth/logout` | 用户登出 |
| GET | `/user/auth/refresh` | 刷新Token |
| DELETE | `/user/auth/account` | 注销账号 |

### 用户信息接口

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/user/profile` | 获取用户信息 |
| PUT | `/user/profile` | 更新用户信息 |
| DELETE | `/user/profile` | 删除用户 |
| GET | `/user/points` | 获取积分信息 |

### 签到接口

| 方法 | 路径 | 说明 |
|------|------|------|
| POST | `/user/sign-in` | 每日签到 |
| GET | `/user/sign-in/status` | 获取签到状态 |
| GET | `/user/sign-in/calendar` | 获取签到日历 |
| GET | `/user/sign-in/statistics` | 获取签到统计 |

### 徽章接口

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/user/badges` | 获取徽章列表 |
| GET | `/user/badges/{id}` | 获取徽章详情 |
| POST | `/user/badges/{id}/claim` | 领取徽章 |

## 数据库表

### user (用户表)

| 字段 | 类型 | 说明 |
|------|------|------|
| id | BIGINT | 主键 |
| username | VARCHAR(50) | 用户名 |
| password | VARCHAR(255) | 密码(加密) |
| nickname | VARCHAR(50) | 昵称 |
| email | VARCHAR(100) | 邮箱 |
| phone | VARCHAR(20) | 手机号 |
| avatar | VARCHAR(500) | 头像URL |
| gender | INT | 性别 |
| birthday | VARCHAR(20) | 生日 |
| bio | VARCHAR(500) | 个人简介 |
| points | BIGINT | 积分 |
| status | INT | 状态 |
| last_login_time | VARCHAR(20) | 最后登录时间 |
| last_login_ip | VARCHAR(50) | 最后登录IP |
| create_time | DATETIME | 创建时间 |
| update_time | DATETIME | 更新时间 |
| deleted | INT | 删除标记 |

### badge (徽章表)

| 字段 | 类型 | 说明 |
|------|------|------|
| id | BIGINT | 主键 |
| name | VARCHAR(100) | 徽章名称 |
| description | VARCHAR(500) | 徽章描述 |
| icon | VARCHAR(500) | 徽章图标 |
| type | INT | 徽章类型 |
| condition_desc | VARCHAR(500) | 获取条件 |
| required_points | BIGINT | 所需积分 |
| sort_order | INT | 排序 |
| status | INT | 状态 |
| create_time | DATETIME | 创建时间 |
| update_time | DATETIME | 更新时间 |
| deleted | INT | 删除标记 |

### user_badge (用户徽章关联表)

| 字段 | 类型 | 说明 |
|------|------|------|
| id | BIGINT | 主键 |
| user_id | BIGINT | 用户ID |
| badge_id | BIGINT | 徽章ID |
| obtained_time | VARCHAR(20) | 获得时间 |
| create_time | DATETIME | 创建时间 |
| update_time | DATETIME | 更新时间 |
| deleted | INT | 删除标记 |

### points_record (积分记录表)

| 字段 | 类型 | 说明 |
|------|------|------|
| id | BIGINT | 主键 |
| user_id | BIGINT | 用户ID |
| points_change | BIGINT | 积分变化 |
| points_after | BIGINT | 变化后积分 |
| reason | VARCHAR(200) | 变化原因 |
| business_type | INT | 业务类型 |
| business_id | BIGINT | 业务ID |
| create_time | DATETIME | 创建时间 |
| update_time | DATETIME | 更新时间 |
| deleted | INT | 删除标记 |

## Redis数据结构

### 签到数据

- Key: `sign_in:{userId}:{yyyyMM}`
- Type: Bitmap
- 说明: 使用Bitmap存储每月的签到数据，每个bit代表一天

### 用户缓存

- Key: `user:{userId}`
- Type: String (JSON)
- TTL: 1小时
- 说明: 缓存用户信息

## 配置说明

### 环境变量

| 变量名 | 说明 | 默认值 |
|-------|------|--------|
| DB_HOST | MySQL主机 | localhost |
| DB_PORT | MySQL端口 | 3307 |
| DB_NAME | 数据库名 | speaking_robot |
| DB_USER | 数据库用户 | speaking_robot_user |
| DB_PASSWORD | 数据库密码 | 123456 |
| REDIS_HOST | Redis主机 | localhost |
| REDIS_PORT | Redis端口 | 7001 |
| ES_HOST | Elasticsearch主机 | localhost |
| ES_PORT | Elasticsearch端口 | 9200 |
| KAFKA_BROKERS | Kafka地址 | localhost:9092 |

## 启动方式

```bash
# 编译
mvn clean package

# 运行
java -jar target/user-service-1.0.0.jar

# 或使用Maven
mvn spring-boot:run
```

## 注意事项

1. 确保MySQL、Redis、Elasticsearch、Kafka已启动
2. 首次启动会自动创建数据库表
3. JWT密钥需要与API网关保持一致
4. 密码使用BCrypt加密
5. 所有时间字段使用字符串存储，格式为 `yyyy-MM-dd HH:mm:ss`
