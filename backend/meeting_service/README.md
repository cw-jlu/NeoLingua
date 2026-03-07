# Meeting Service (Meeting服务)

## 模块功能
Meeting服务负责多人协作练习功能，包括好友系统、Meeting房间管理、实时通信和Meeting回顾报告。

## 技术栈
- Spring Boot 3.2.0
- MyBatis-Plus 3.5.5
- Netty WebSocket (实时通信)
- Redis (房间状态缓存)
- Kafka (异步日志)
- MySQL 8.0

## 端口
- HTTP: 8083
- Netty WebSocket: 8090

## 核心功能

### 1. 好友系统
- 好友列表查询
- 发送好友请求
- 接受/拒绝好友请求
- 删除好友
- 好友搜索

### 2. Meeting房间管理
- 创建Meeting房间
- 加入/离开房间
- 开始/结束Meeting
- 房间列表查询
- 参与者管理

### 3. 实时通信
- WebSocket消息推送
- 文本消息
- 音频消息
- 系统消息

### 4. Meeting报告
- 会议回顾报告
- 参与时长统计
- 发言统计
- 报告分享

## API接口

### 用户端接口

#### 好友相关
- `GET /user/friends` - 获取好友列表
- `GET /user/friends/{id}` - 获取好友详情
- `DELETE /user/friends/{id}` - 删除好友
- `GET /user/friends/search` - 搜索用户
- `POST /user/friends/requests` - 发送好友请求
- `GET /user/friends/requests` - 获取好友请求列表
- `PUT /user/friends/requests/{id}/accept` - 接受好友请求
- `PUT /user/friends/requests/{id}/reject` - 拒绝好友请求
- `DELETE /user/friends/requests/{id}` - 删除好友请求

#### Meeting相关
- `POST /user/meetings` - 创建Meeting房间
- `GET /user/meetings` - 获取房间列表
- `GET /user/meetings/{id}` - 获取房间详情
- `PUT /user/meetings/{id}` - 更新房间配置
- `DELETE /user/meetings/{id}` - 删除房间
- `POST /user/meetings/{id}/join` - 加入房间
- `POST /user/meetings/{id}/leave` - 离开房间
- `POST /user/meetings/{id}/start` - 开始会议
- `POST /user/meetings/{id}/end` - 结束会议
- `GET /user/meetings/{id}/participants` - 获取参与者列表
- `POST /user/meetings/{id}/invite` - 邀请好友
- `DELETE /user/meetings/{id}/participants/{userId}` - 移除参与者

#### 消息相关
- `POST /user/meetings/{id}/messages` - 发送消息
- `GET /user/meetings/{id}/messages` - 获取消息列表
- `DELETE /user/meetings/{id}/messages/{messageId}` - 删除消息
- `POST /user/meetings/{id}/audio` - 上传音频

#### 报告相关
- `GET /user/meetings/{id}/report` - 获取会议回顾报告
- `POST /user/meetings/{id}/report/share` - 分享报告
- `GET /user/meetings/history` - 获取历史Meeting

### 管理端接口

#### Meeting管理
- `GET /admin/meetings` - 获取所有Meeting
- `GET /admin/meetings/{id}` - 获取Meeting详情
- `DELETE /admin/meetings/{id}` - 删除Meeting
- `POST /admin/meetings/{id}/close` - 强制关闭Meeting

#### 好友关系管理
- `GET /admin/friends/relationships` - 获取好友关系列表
- `DELETE /admin/friends/relationships/{id}` - 删除好友关系

#### 统计分析
- `GET /admin/meetings/statistics` - Meeting统计数据
- `GET /admin/meetings/active` - 活跃Meeting统计
- `GET /admin/meetings/duration` - Meeting时长统计

## 数据库表结构

### friend (好友表)
| 字段 | 类型 | 说明 |
|------|------|------|
| id | BIGINT | 主键 |
| user_id | BIGINT | 用户ID |
| friend_id | BIGINT | 好友ID |
| status | INT | 状态 (0-待确认 1-已接受 2-已拒绝) |
| remark | VARCHAR(50) | 备注名称 |
| create_time | VARCHAR(20) | 创建时间 |
| update_time | VARCHAR(20) | 更新时间 |
| deleted | INT | 删除标记 |

### meeting (Meeting表)
| 字段 | 类型 | 说明 |
|------|------|------|
| id | BIGINT | 主键 |
| name | VARCHAR(100) | 房间名称 |
| description | TEXT | 房间描述 |
| creator_id | BIGINT | 创建者ID |
| theme_id | BIGINT | 主题ID |
| max_participants | INT | 最大人数 |
| current_participants | INT | 当前人数 |
| status | INT | 状态 (0-等待中 1-进行中 2-已结束) |
| start_time | VARCHAR(20) | 开始时间 |
| end_time | VARCHAR(20) | 结束时间 |
| is_public | INT | 是否公开 (0-私密 1-公开) |
| create_time | VARCHAR(20) | 创建时间 |
| update_time | VARCHAR(20) | 更新时间 |
| deleted | INT | 删除标记 |

### meeting_participant (Meeting参与者表)
| 字段 | 类型 | 说明 |
|------|------|------|
| id | BIGINT | 主键 |
| meeting_id | BIGINT | Meeting ID |
| user_id | BIGINT | 用户ID |
| role | INT | 角色 (0-参与者 1-主持人) |
| join_time | VARCHAR(20) | 加入时间 |
| leave_time | VARCHAR(20) | 离开时间 |
| status | INT | 状态 (0-在线 1-离线) |
| create_time | VARCHAR(20) | 创建时间 |
| update_time | VARCHAR(20) | 更新时间 |
| deleted | INT | 删除标记 |

### meeting_message (Meeting消息表)
| 字段 | 类型 | 说明 |
|------|------|------|
| id | BIGINT | 主键 |
| meeting_id | BIGINT | Meeting ID |
| sender_id | BIGINT | 发送者ID |
| message_type | INT | 消息类型 (0-文本 1-音频 2-系统) |
| content | TEXT | 消息内容 |
| audio_url | VARCHAR(255) | 音频URL |
| create_time | VARCHAR(20) | 创建时间 |
| update_time | VARCHAR(20) | 更新时间 |
| deleted | INT | 删除标记 |

## Redis缓存策略

### 缓存Key设计
- `meeting:{meetingId}` - Meeting状态缓存
- `meeting:participants:{meetingId}` - 参与者列表缓存
- `meeting:online:{meetingId}` - 在线用户集合

### 缓存过期时间
- Meeting状态: 1小时
- 参与者列表: 30分钟
- 在线用户: 实时更新

## WebSocket通信

### Netty WebSocket

#### 连接地址
```
ws://localhost:8090/ws/meeting?userId={userId}&meetingId={meetingId}
```

#### 连接参数
- `userId`: 用户ID（必填）
- `meetingId`: Meeting房间ID（必填）

#### 消息格式

**发送消息**:
```json
{
  "messageType": 0,
  "content": "Hello",
  "audioUrl": ""
}
```

**接收消息**:
```json
{
  "id": 1,
  "meetingId": 456,
  "senderId": 123,
  "messageType": 0,
  "content": "Hello",
  "audioUrl": "",
  "createTime": "2026-03-02T10:00:00"
}
```

#### 消息类型
- `0` - 文本消息
- `1` - 音频消息
- `2` - 系统消息
- `-1` - 错误消息

#### 特性
- 高并发支持（~50,000 连接）
- 低延迟（P99 < 10ms）
- 自动心跳检测（60秒超时）
- 消息广播
- 异常自动重连

详细文档请参考: [NETTY_MIGRATION.md](./NETTY_MIGRATION.md)

## Kafka Topic

### meeting-logs
用于记录Meeting日志，供分析服务消费

消息格式:
```json
{
  "meetingId": 123,
  "creatorId": 456,
  "themeId": 789,
  "participantCount": 4,
  "startTime": "2024-01-01 10:00:00",
  "endTime": "2024-01-01 11:00:00",
  "duration": 3600,
  "messageCount": 150
}
```

## 配置说明

### application.yml
```yaml
server:
  port: 8083

spring:
  application:
    name: meeting-service
  datasource:
    url: jdbc:mysql://localhost:3307/speakmaster_meeting
    username: root
    password: root
  data:
    redis:
      host: localhost
      port: 6379
      database: 3
  kafka:
    bootstrap-servers: localhost:9092

# Netty WebSocket 配置
netty:
  websocket:
    port: 8090
    boss-threads: 1
    worker-threads: 0
    max-frame-size: 65536
    read-timeout: 60

# MyBatis-Plus 配置
mybatis-plus:
  configuration:
    map-underscore-to-camel-case: true
  global-config:
    db-config:
      logic-delete-field: deleted
      logic-delete-value: 1
      logic-not-delete-value: 0

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

### 启动命令
```bash
cd backend/meeting_service
mvn spring-boot:run
```

## 依赖服务
- Common模块
- User Service (通过Feign调用获取用户信息)
- AI Service (AI参与者功能)
- MySQL
- Redis
- Kafka

## 注意事项
1. 所有API需要JWT认证
2. Netty WebSocket连接需要在URL参数中携带userId和meetingId
3. Meeting状态需要实时同步到Redis
4. 参与者离线需要及时更新状态
5. Meeting结束后需要生成回顾报告
6. 好友关系是双向的，需要创建两条记录
7. 房间人数限制需要严格控制
8. 消息需要实时推送给所有在线参与者
9. Netty WebSocket 端口 8090，可通过配置修改
10. 建议客户端实现心跳机制，避免连接超时

## 性能优化

### Netty WebSocket 性能
- 并发连接数: ~50,000
- 消息延迟 (P99): ~10ms
- CPU 使用率: 比 Spring WebSocket 降低 30%
- 内存使用: 比 Spring WebSocket 降低 40%

### 优化建议
1. 根据服务器配置调整 Netty worker 线程数
2. 根据消息大小调整 max-frame-size
3. 启用消息压缩（大消息场景）
4. 使用 Redis Pub/Sub 实现集群部署

---

**开发者**: SpeakMaster Team  
**最后更新**: 2026-03-02
