# SpeakMaster 公共模块 (Common Module)

## 模块说明

公共模块包含了所有微服务共享的基础组件和工具类，包括：

- 统一响应格式
- 全局异常处理
- 公共实体基类
- 工具类集合

## 主要组件

### 1. 统一响应 (Result)

所有API返回值必须使用`Result`类封装，包含三个字段：

- `code`: 响应码 (Integer)
- `msg`: 响应消息 (String)
- `data`: 响应数据 (泛型T)

**使用示例:**

```java
// 成功响应
return Result.success(data);

// 失败响应
return Result.error("操作失败");

// 自定义错误码
return Result.error(ErrorCode.USER_NOT_FOUND);
```

### 2. 错误码枚举 (ErrorCode)

统一管理系统中的错误码和错误消息，按模块分类：

- 1xxx: 通用错误码
- 2xxx: 认证授权错误码
- 3xxx: 用户相关错误码
- 4xxx: 练习相关错误码
- 5xxx: Meeting相关错误码
- 6xxx: 社区相关错误码
- 7xxx: 分析相关错误码
- 8xxx: 通知相关错误码
- 9xxx: AI相关错误码
- 10xxx: 文件相关错误码

### 3. 业务异常 (BusinessException)

用于业务逻辑中抛出的异常，会被全局异常处理器捕获并返回统一格式。

**使用示例:**

```java
if (user == null) {
    throw new BusinessException(ErrorCode.USER_NOT_FOUND);
}
```

### 4. 全局异常处理器 (GlobalExceptionHandler)

自动捕获并处理系统中的各种异常：

- 业务异常 (BusinessException)
- 参数校验异常 (MethodArgumentNotValidException)
- 参数绑定异常 (BindException)
- 请求方法不支持异常 (HttpRequestMethodNotSupportedException)
- 其他未知异常 (Exception)

### 5. 公共实体基类 (BaseEntity)

所有实体类都应继承此类，包含公共字段：

- `id`: 主键ID (Long)
- `createTime`: 创建时间 (LocalDateTime)
- `updateTime`: 更新时间 (LocalDateTime)
- `deleted`: 逻辑删除标记 (Integer, 0-未删除, 1-已删除)

**使用示例:**

```java
@Entity
@Table(name = "user")
public class User extends BaseEntity {
    private String username;
    private String password;
    // ...
}
```

### 6. JWT工具类 (JwtUtil)

用于生成和解析JWT Token：

- `generateToken()`: 生成Token
- `parseToken()`: 解析Token
- `getUserId()`: 从Token中获取用户ID
- `getUsername()`: 从Token中获取用户名
- `validateToken()`: 验证Token是否有效
- `refreshToken()`: 刷新Token

**使用示例:**

```java
// 生成Token
String token = JwtUtil.generateToken(userId, username);

// 解析Token
Long userId = JwtUtil.getUserId(token);

// 验证Token
boolean valid = JwtUtil.validateToken(token);
```

### 7. Redis工具类 (RedisUtil)

封装常用的Redis操作：

- String操作: `get()`, `set()`, `incr()`, `decr()`
- Hash操作: `hget()`, `hset()`, `hmget()`, `hmset()`
- Set操作: `sGet()`, `sSet()`, `sRemove()`
- List操作: `lGet()`, `lSet()`
- ZSet操作: `zAdd()`, `zRange()`, `zRank()`

**使用示例:**

```java
@Autowired
private RedisUtil redisUtil;

// 设置值
redisUtil.set("key", "value", 60, TimeUnit.SECONDS);

// 获取值
Object value = redisUtil.get("key");

// 有序集合排名
redisUtil.zAdd("ranking", userId, score);
long rank = redisUtil.zRank("ranking", userId);
```

### 8. 日期工具类 (DateUtil)

提供常用的日期时间操作：

- `format()`: 格式化日期时间
- `parse()`: 解析日期时间字符串
- `now()`: 获取当前日期时间
- `today()`: 获取当前日期
- `daysBetween()`: 计算两个日期之间的天数
- `isToday()`: 判断是否是今天

**使用示例:**

```java
// 格式化日期时间
String dateStr = DateUtil.format(LocalDateTime.now());

// 解析日期
LocalDate date = DateUtil.parseDate("2024-01-01");

// 计算天数差
long days = DateUtil.daysBetween(startDate, endDate);
```

## 依赖说明

本模块依赖以下组件：

- Spring Boot 3.2.0
- Lombok 1.18.30
- JWT (jjwt) 0.12.3
- FastJSON 2.0.43
- Spring Data JPA (provided)
- Spring Data Redis (provided)

## 使用方式

在其他微服务的`pom.xml`中添加依赖：

```xml
<dependency>
    <groupId>com.speakmaster</groupId>
    <artifactId>common</artifactId>
    <version>1.0.0</version>
</dependency>
```

## 注意事项

1. 所有代码注释必须使用中文
2. 所有API返回值必须使用`Result`类封装
3. 业务异常必须使用`BusinessException`抛出
4. 实体类必须继承`BaseEntity`
5. JWT密钥应从环境变量`JWT_SECRET`获取
