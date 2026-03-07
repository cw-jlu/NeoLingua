# Practice Service (练习服务)

## 模块功能
练习服务负责单人AI对话练习功能，包括主题管理、角色管理、练习会话管理和消息处理。

## 技术栈
- Spring Boot 3.2.0
- Spring Data JPA
- Spring Cache + Redis
- OpenFeign (调用AI服务)
- Kafka (异步日志)
- MySQL 8.0

## 端口
8082

## 核心功能

### 1. 主题管理
- 主题列表查询 (分页、分类)
- 主题详情查询
- 热门主题推荐
- 主题创建/更新/删除 (管理端)

### 2. 角色管理
- 预制角色查询
- 自定义角色创建/更新/删除
- 角色详情查询

### 3. 练习会话
- 创建练习会话
- 会话列表查询
- 会话详情查询
- 结束会话 (评分、反馈)
- 删除会话

### 4. 消息处理
- 发送消息 (调用AI服务)
- 消息列表查询
- 音频消息支持

## API接口

### 用户端接口

#### 主题相关
- `GET /user/practice/themes` - 获取主题列表
- `GET /user/practice/themes/{id}` - 获取主题详情
- `GET /user/practice/themes/popular` - 获取热门主题

#### 角色相关
- `GET /user/practice/roles` - 获取角色列表
- `POST /user/practice/roles` - 创建自定义角色
- `GET /user/practice/roles/{id}` - 获取角色详情
- `PUT /user/practice/roles/{id}` - 更新自定义角色
- `DELETE /user/practice/roles/{id}` - 删除自定义角色

#### 会话相关
- `POST /user/practice/sessions` - 创建练习会话
- `GET /user/practice/sessions` - 获取会话列表
- `GET /user/practice/sessions/{id}` - 获取会话详情
- `POST /user/practice/sessions/{id}/end` - 结束会话
- `DELETE /user/practice/sessions/{id}` - 删除会话
- `POST /user/practice/sessions/{id}/messages` - 发送消息
- `GET /user/practice/sessions/{id}/messages` - 获取消息列表

### 管理端接口

#### 主题管理
- `POST /admin/practice/themes` - 创建主题
- `PUT /admin/practice/themes/{id}` - 更新主题
- `DELETE /admin/practice/themes/{id}` - 删除主题

#### 角色管理
- `POST /admin/practice/roles` - 创建预制角色
- `PUT /admin/practice/roles/{id}` - 更新角色
- `DELETE /admin/practice/roles/{id}` - 删除角色

## 数据库表结构

### theme (主题表)
| 字段 | 类型 | 说明 |
|------|------|------|
| id | BIGINT | 主键 |
| name | VARCHAR(100) | 主题名称 |
| description | TEXT | 主题描述 |
| cover | VARCHAR(255) | 封面图片 |
| category | VARCHAR(50) | 分类 |
| difficulty | INT | 难度等级 (1-5) |
| tags | VARCHAR(255) | 标签 |
| use_count | INT | 使用次数 |
| sort_order | INT | 排序顺序 |
| status | INT | 状态 (0-草稿 1-已发布) |
| create_time | VARCHAR(20) | 创建时间 |
| update_time | VARCHAR(20) | 更新时间 |
| deleted | INT | 删除标记 |

### role (角色表)
| 字段 | 类型 | 说明 |
|------|------|------|
| id | BIGINT | 主键 |
| name | VARCHAR(100) | 角色名称 |
| description | TEXT | 角色描述 |
| prompt | TEXT | 角色提示词 |
| avatar | VARCHAR(255) | 角色头像 |
| type | INT | 类型 (0-预制 1-自定义) |
| user_id | BIGINT | 创建者ID |
| create_time | VARCHAR(20) | 创建时间 |
| update_time | VARCHAR(20) | 更新时间 |
| deleted | INT | 删除标记 |

### session (练习会话表)
| 字段 | 类型 | 说明 |
|------|------|------|
| id | BIGINT | 主键 |
| user_id | BIGINT | 用户ID |
| theme_id | BIGINT | 主题ID |
| role_id | BIGINT | 角色ID |
| status | INT | 状态 (0-进行中 1-已结束) |
| start_time | VARCHAR(20) | 开始时间 |
| end_time | VARCHAR(20) | 结束时间 |
| score | INT | 评分 (0-100) |
| feedback | TEXT | 反馈内容 |
| create_time | VARCHAR(20) | 创建时间 |
| update_time | VARCHAR(20) | 更新时间 |
| deleted | INT | 删除标记 |

### message (消息表)
| 字段 | 类型 | 说明 |
|------|------|------|
| id | BIGINT | 主键 |
| session_id | BIGINT | 会话ID |
| sender | VARCHAR(20) | 发送者 (user/ai) |
| content | TEXT | 消息内容 |
| audio_url | VARCHAR(255) | 音频URL |
| create_time | VARCHAR(20) | 创建时间 |
| update_time | VARCHAR(20) | 更新时间 |
| deleted | INT | 删除标记 |

## Redis缓存策略

### 缓存Key设计
- `theme_list:{page}_{size}` - 主题列表缓存
- `theme:{themeId}` - 主题详情缓存
- `theme_popular:popular` - 热门主题缓存
- `role_list:{page}_{size}_{userId}` - 角色列表缓存
- `role:{roleId}` - 角色详情缓存

### 缓存过期时间
- 主题列表: 30分钟
- 主题详情: 1小时
- 热门主题: 10分钟
- 角色列表: 30分钟
- 角色详情: 1小时

## Kafka Topic

### practice-logs
用于记录练习日志，供分析服务消费

消息格式:
```json
{
  "userId": 123,
  "sessionId": 456,
  "themeId": 789,
  "roleId": 101,
  "startTime": "2024-01-01 10:00:00",
  "endTime": "2024-01-01 10:30:00",
  "score": 85,
  "messageCount": 20
}
```

## OpenFeign调用

### AI Service
- `POST /ai/chat` - 发送消息获取AI回复
- `POST /ai/feedback` - 获取会话反馈

## 配置说明

### application.yml
```yaml
server:
  port: 8082

spring:
  application:
    name: practice-service
  datasource:
    url: jdbc:mysql://localhost:3307/speakmaster_practice
    username: root
    password: root
  jpa:
    hibernate:
      ddl-auto: update
  data:
    redis:
      host: localhost
      port: 6379
      database: 2
  kafka:
    bootstrap-servers: localhost:9092
    producer:
      key-serializer: org.apache.kafka.common.serialization.StringSerializer
      value-serializer: org.apache.kafka.common.serialization.StringSerializer

feign:
  client:
    config:
      ai-service:
        url: http://localhost:8089
```

## 启动方式

### 前置条件
1. MySQL已启动
2. Redis已启动
3. Kafka已启动
4. AI Service已启动

### 启动命令
```bash
cd backend/practice_service
mvn spring-boot:run
```

## 依赖服务
- Common模块
- AI Service (通过Feign调用)
- MySQL
- Redis
- Kafka

## 注意事项
1. 所有API需要JWT认证 (除管理端接口外)
2. 会话状态管理需要严格控制
3. AI服务调用需要熔断降级保护
4. 消息记录需要异步写入Kafka
5. 主题使用次数需要实时更新
6. 自定义角色只能由创建者修改/删除

---

**开发者**: SpeakMaster Team  
**最后更新**: 2024-01-01
