# SpeakMaster API网关

## 模块说明

API网关是系统的统一入口，负责：

- 请求路由转发
- JWT认证
- 限流保护
- 熔断降级
- 跨域处理

## 主要功能

### 1. 路由配置

网关将请求路由到对应的微服务：

| 路径前缀 | 目标服务 | 端口 |
|---------|---------|------|
| `/user/**`, `/admin/users/**` | user-service | 8081 |
| `/user/practice/**`, `/admin/practice/**` | practice-service | 8082 |
| `/user/friends/**`, `/user/meetings/**`, `/admin/meetings/**` | meeting-service | 8083 |
| `/user/community/**`, `/admin/community/**` | community-service | 8084 |
| `/user/analysis/**`, `/admin/analysis/**` | analysis-service | 8085 |
| `/user/notifications/**`, `/admin/notifications/**` | notification-service | 8086 |
| `/admin/dashboard/**`, `/admin/system/**`, `/admin/logs/**` | admin-service | 8087 |
| `/user/ai/**`, `/admin/ai/**` | ai-gateway | 8088 |

### 2. JWT认证

所有请求(除白名单外)都需要携带有效的JWT Token：

**白名单路径:**
- `/user/auth/register` - 用户注册
- `/user/auth/login` - 用户登录
- `/actuator/**` - 健康检查
- `/fallback/**` - 熔断降级

**Token传递方式:**
1. Header: `Authorization: Bearer <token>`
2. Query参数: `?token=<token>`

**认证成功后:**
网关会将用户信息添加到请求头中：
- `X-User-Id`: 用户ID
- `X-Username`: 用户名

### 3. 限流保护

使用Redis实现分布式限流：

**限流策略:**
- 普通服务: 10 req/s (突发20)
- AI服务: 5 req/s (突发10)

**限流维度:**
- IP限流: 基于客户端IP
- 用户限流: 基于用户ID
- 路径限流: 基于请求路径

### 4. 熔断降级

使用Resilience4j实现熔断保护：

**熔断配置:**
- 滑动窗口大小: 10次调用
- 失败率阈值: 50%
- 慢调用阈值: 2秒
- 开启状态等待时间: 10秒
- 半开状态允许调用数: 3次

**降级响应:**
当服务不可用时，返回友好的错误信息。

### 5. 跨域处理

全局CORS配置，允许所有来源的跨域请求。

## 配置说明

### 环境变量

| 变量名 | 说明 | 默认值 |
|-------|------|--------|
| REDIS_HOST | Redis主机 | localhost |
| REDIS_PORT | Redis端口 | 6379 |
| REDIS_PASSWORD | Redis密码 | 空 |

### 端口配置

- 网关端口: 8080
- 各微服务端口: 8081-8088

## 启动方式

```bash
# 编译
mvn clean package

# 运行
java -jar target/api-gateway-1.0.0.jar

# 或使用Maven
mvn spring-boot:run
```

## 健康检查

访问 `http://localhost:8080/actuator/health` 查看网关健康状态。

## 监控指标

访问 `http://localhost:8080/actuator/metrics` 查看监控指标。

## 注意事项

1. 确保Redis服务已启动
2. 确保各微服务已启动并监听对应端口
3. JWT密钥需要与用户服务保持一致
4. 生产环境需要调整限流和熔断参数
