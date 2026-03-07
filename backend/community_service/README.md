# Community Service (社区服务)

## 模块功能
社区服务负责社区帖子、点赞、评论、收藏和搜索功能。

## 技术栈
- Spring Boot 3.2.0
- Spring Data JPA
- Spring Data Redis (HyperLogLog点赞去重)
- Spring Data Elasticsearch (全文搜索)
- Spring Cache
- Kafka (异步任务)
- MySQL 8.0

## 端口
8084

## 核心功能

### 1. 帖子管理
- 发布帖子
- 帖子列表查询 (分页、分类、排序)
- 帖子详情查询
- 热门帖子推荐
- 更新/删除帖子
- 浏览数统计

### 2. 点赞功能
- 点赞/取消点赞
- Redis HyperLogLog去重统计
- 点赞数查询

### 3. 评论功能
- 发表评论
- 评论列表查询
- 回复评论 (二级评论)
- 更新/删除评论
- 评论点赞

### 4. 收藏功能
- 收藏/取消收藏帖子
- 收藏列表查询

### 5. 搜索功能
- Elasticsearch全文搜索
- 按标签搜索
- 按分类搜索

## API接口

### 用户端接口

#### 帖子相关
- `POST /user/community/posts` - 发布帖子
- `GET /user/community/posts` - 获取帖子列表
- `GET /user/community/posts/{id}` - 获取帖子详情
- `PUT /user/community/posts/{id}` - 更新帖子
- `DELETE /user/community/posts/{id}` - 删除帖子
- `GET /user/community/posts/my` - 获取我的帖子
- `GET /user/community/posts/hot` - 获取热门帖子
- `GET /user/community/posts/recommended` - 获取推荐帖子
- `POST /user/community/posts/{id}/media` - 上传帖子媒体文件

#### 点赞相关
- `POST /user/community/posts/{id}/like` - 点赞
- `DELETE /user/community/posts/{id}/like` - 取消点赞
- `GET /user/community/posts/{id}/likes` - 获取点赞列表
- `GET /user/community/posts/{id}/likes/count` - 获取点赞数

#### 评论相关
- `POST /user/community/posts/{id}/comments` - 发表评论
- `GET /user/community/posts/{id}/comments` - 获取评论列表
- `GET /user/community/comments/{id}` - 获取评论详情
- `PUT /user/community/comments/{id}` - 更新评论
- `DELETE /user/community/comments/{id}` - 删除评论
- `POST /user/community/comments/{id}/like` - 点赞评论
- `DELETE /user/community/comments/{id}/like` - 取消点赞评论
- `POST /user/community/comments/{id}/reply` - 回复评论

#### 收藏相关
- `POST /user/community/posts/{id}/favorite` - 收藏帖子
- `DELETE /user/community/posts/{id}/favorite` - 取消收藏
- `GET /user/community/favorites` - 获取收藏列表

#### 搜索相关
- `GET /user/community/search` - 搜索帖子 (ES全文搜索)
- `GET /user/community/tags` - 获取标签列表
- `GET /user/community/tags/{tag}/posts` - 按标签获取帖子

### 管理端接口

#### 帖子管理
- `GET /admin/community/posts` - 获取所有帖子
- `GET /admin/community/posts/{id}` - 获取帖子详情
- `DELETE /admin/community/posts/{id}` - 删除帖子
- `POST /admin/community/posts/{id}/pin` - 置顶帖子
- `POST /admin/community/posts/{id}/unpin` - 取消置顶
- `POST /admin/community/posts/{id}/hide` - 隐藏帖子
- `POST /admin/community/posts/{id}/show` - 显示帖子

#### 评论管理
- `GET /admin/community/comments` - 获取所有评论
- `GET /admin/community/comments/{id}` - 获取评论详情
- `DELETE /admin/community/comments/{id}` - 删除评论

#### 统计分析
- `GET /admin/community/statistics` - 社区统计数据
- `GET /admin/community/posts/trending` - 趋势分析
- `GET /admin/community/users/active` - 活跃用户统计

## 数据库表结构

### post (帖子表)
| 字段 | 类型 | 说明 |
|------|------|------|
| id | BIGINT | 主键 |
| title | VARCHAR(200) | 标题 |
| content | TEXT | 内容 |
| author_id | BIGINT | 作者ID |
| category | VARCHAR(50) | 分类 |
| tags | VARCHAR(255) | 标签 |
| cover_image | VARCHAR(255) | 封面图片 |
| like_count | INT | 点赞数 |
| comment_count | INT | 评论数 |
| view_count | INT | 浏览数 |
| favorite_count | INT | 收藏数 |
| is_pinned | INT | 是否置顶 (0-否 1-是) |
| is_hidden | INT | 是否隐藏 (0-否 1-是) |
| status | INT | 状态 (0-草稿 1-已发布) |
| create_time | VARCHAR(20) | 创建时间 |
| update_time | VARCHAR(20) | 更新时间 |
| deleted | INT | 删除标记 |

### comment (评论表)
| 字段 | 类型 | 说明 |
|------|------|------|
| id | BIGINT | 主键 |
| post_id | BIGINT | 帖子ID |
| user_id | BIGINT | 评论者ID |
| parent_id | BIGINT | 父评论ID |
| content | TEXT | 评论内容 |
| like_count | INT | 点赞数 |
| create_time | VARCHAR(20) | 创建时间 |
| update_time | VARCHAR(20) | 更新时间 |
| deleted | INT | 删除标记 |

### post_like (帖子点赞表)
| 字段 | 类型 | 说明 |
|------|------|------|
| id | BIGINT | 主键 |
| post_id | BIGINT | 帖子ID |
| user_id | BIGINT | 用户ID |
| create_time | VARCHAR(20) | 创建时间 |
| update_time | VARCHAR(20) | 更新时间 |
| deleted | INT | 删除标记 |

### post_favorite (帖子收藏表)
| 字段 | 类型 | 说明 |
|------|------|------|
| id | BIGINT | 主键 |
| post_id | BIGINT | 帖子ID |
| user_id | BIGINT | 用户ID |
| create_time | VARCHAR(20) | 创建时间 |
| update_time | VARCHAR(20) | 更新时间 |
| deleted | INT | 删除标记 |

## Redis数据结构

### HyperLogLog (点赞去重)
- Key: `post:like:{postId}`
- 用途: 统计独立点赞用户数
- 优势: 内存占用极小，适合大量数据

### HyperLogLog (浏览去重)
- Key: `post:view:{postId}`
- 用途: 统计独立访客数
- 优势: 精确度99%，内存占用12KB

### 缓存
- Key: `post:{postId}`
- 用途: 缓存帖子详情
- 过期时间: 30分钟

## Elasticsearch索引

### post索引
```json
{
  "mappings": {
    "properties": {
      "id": {"type": "long"},
      "title": {"type": "text", "analyzer": "ik_max_word"},
      "content": {"type": "text", "analyzer": "ik_max_word"},
      "authorId": {"type": "long"},
      "category": {"type": "keyword"},
      "tags": {"type": "keyword"},
      "likeCount": {"type": "integer"},
      "commentCount": {"type": "integer"},
      "viewCount": {"type": "integer"},
      "createTime": {"type": "date"}
    }
  }
}
```

## Kafka Topic

### community-logs
用于记录社区行为日志

消息格式:
```json
{
  "action": "post_create",
  "userId": 123,
  "postId": 456,
  "timestamp": "2024-01-01 10:00:00"
}
```

## 配置说明

### application.yml
```yaml
server:
  port: 8084

spring:
  application:
    name: community-service
  datasource:
    url: jdbc:mysql://localhost:3307/speakmaster_community
    username: root
    password: root
  jpa:
    hibernate:
      ddl-auto: update
  data:
    redis:
      host: localhost
      port: 6379
      database: 4
  elasticsearch:
    uris: http://localhost:9200
    username: elastic
    password: elastic
  kafka:
    bootstrap-servers: localhost:9092
```

## 启动方式

### 前置条件
1. MySQL已启动
2. Redis已启动
3. Elasticsearch已启动
4. Kafka已启动

### 启动命令
```bash
cd backend/community_service
mvn spring-boot:run
```

## 依赖服务
- Common模块
- User Service (获取用户信息)
- MySQL
- Redis
- Elasticsearch
- Kafka

## 注意事项
1. 点赞使用Redis HyperLogLog去重，需要定时同步到MySQL
2. 浏览数使用HyperLogLog统计独立访客
3. 帖子内容需要同步到Elasticsearch索引
4. 评论支持二级回复，不支持更深层级
5. 删除帖子时需要同时删除相关的点赞、评论、收藏记录
6. 热门帖子按点赞数排序，需要定时更新
7. 搜索功能依赖Elasticsearch，需要保证数据同步

---

**开发者**: SpeakMaster Team  
**最后更新**: 2024-01-01
