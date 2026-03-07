#  SpeakMaster - AI驱动的英语口语学习平台

##  项目简介

SpeakMaster是一个基于微服务架构的AI驱动英语口语学习平台,集成了语音分析、AI对话、社区互动等功能。

### 核心功能
-  **AI对话练习**: 基于LangGraph的智能对话系统
-  **发音分析**: MFA强制对齐技术进行精准发音评估
-  **实时Meeting**: WebSocket实时语音Meeting
-  **社区互动**: 帖子、评论、点赞、收藏
-  **学习分析**: 学习数据统计、排名、报表导出
-  **成就系统**: 签到、积分、徽章

---

##  技术架构

### 后端技术栈
- **Java微服务**: Spring Boot 3.2.0 + Spring Cloud
- **服务注册**: Nacos 2.2.3
- **API网关**: Spring Cloud Gateway
- **限流熔断**: Sentinel
- **链路追踪**: SkyWalking
- **数据库**: MySQL 8.0 + MyBatis-Plus
- **缓存**: Redis 6.2
- **消息队列**: Kafka
- **搜索引擎**: Elasticsearch 8.11
- **对象存储**: MinIO
- **向量数据库**: Milvus 2.3

### Python服务
- **AI Service**: FastAPI + LangGraph + Ollama
- **Analysis Service**: FastAPI + MFA + Pandas

### 前端技术栈
- **管理端**: Vue3 + Element Plus + Vite
- **用户端**: Vue3 + Vant4 + Vite

---

## 📁 项目结构

```
SpeakMaster/
├── backend/                    # 后端服务
│   ├── common/                # 公共模块
│   ├── api_gateway/           # API网关 (8080)
│   ├── user_service/          # 用户服务 (8081)
│   ├── practice_service/      # 练习服务 (8082)
│   ├── meeting_service/       # Meeting服务 (8083)
│   ├── community_service/     # 社区服务 (8084)
│   ├── analysis_service/      # 分析服务 (8085) Python
│   ├── notification_service/  # 通知服务 (8086)
│   ├── admin_service/         # 管理服务 (8087)
│   ├── ai_gateway/            # AI网关 (8088)
│   └── ai_service/            # AI服务 (8089) Python
├── frontend/                   # 前端
│   ├── admin_web/             # 管理端 (5173)
│   └── user_web/              # 用户端 (3001)
├── environment/                # Docker环境
│   ├── docker-compose.yml     # 中间件编排
│   └── scripts/               # 初始化脚本
├── scripts/                    # 启动脚本
│   ├── start-python-services.bat
│   ├── start-frontend.bat
│   └── check-services.bat
├── START_ALL.bat              # 一键启动
├── STOP_ALL.bat               # 一键停止
├── QUICK_START.md             # 快速启动指南
└── PRE_LAUNCH_CHECKLIST.md    # 启动检查清单
```

---

## 🚀 快速开始

### 环境要求
- JDK 17+
- Python 3.10+
- Node.js 18+
- Docker Desktop
- Maven 3.8+ (IDEA自带)

### 一键启动 (推荐)

```bash
# 双击运行
START_ALL.bat
```

这将自动:
1. 启动所有Docker中间件
2. 编译Common模块
3. 提示启动Java服务
4. 启动Python服务
5. 启动前端服务

### 手动启动

详细步骤请查看:
- [快速启动指南](QUICK_START.md)
- [启动检查清单](PRE_LAUNCH_CHECKLIST.md)
- [完整启动指南](STARTUP_GUIDE.md)

---

## 🔍 服务端口

| 端口 | 服务 | 访问地址 |
|------|------|----------|
| 8080 | API Gateway | http://localhost:8080 |
| 8081 | User Service | http://localhost:8081 |
| 8082 | Practice Service | http://localhost:8082 |
| 8083 | Meeting Service | http://localhost:8083 |
| 8084 | Community Service | http://localhost:8084 |
| 8085 | Analysis Service | http://localhost:8085 |
| 8086 | Notification Service | http://localhost:8086 |
| 8087 | Admin Service | http://localhost:8087 |
| 8088 | AI Gateway | http://localhost:8088 |
| 8089 | AI Service | http://localhost:8089 |
| 5173 | Admin Web | http://localhost:5173 |
| 3001 | User Web | http://localhost:3001 |
| 8848 | Nacos | http://localhost:8848/nacos |
| 8858 | Sentinel | http://localhost:8858 |
| 8868 | SkyWalking | http://localhost:8868 |
| 9001 | MinIO | http://localhost:9001 |

---

## 服务状态检查

```bash
# 运行健康检查脚本
scripts\check-services.bat

# 或手动检查
curl http://localhost:8080/actuator/health  # API Gateway
curl http://localhost:8081/actuator/health  # User Service
curl http://localhost:8089/health           # AI Service
curl http://localhost:8085/health           # Analysis Service
```

---

## 核心功能模块

### 1. 用户服务 (User Service)
- 用户注册、登录、认证
- 用户信息管理
- 签到系统 (Redis Bitmap)
- 积分系统
- 徽章系统

### 2. 练习服务 (Practice Service)
- 练习主题管理
- 角色扮演
- 练习会话
- AI对话集成

### 3. Meeting服务 (Meeting Service)
- 好友系统
- Meeting房间管理
- WebSocket实时通信
- 语音消息处理

### 4. 社区服务 (Community Service)
- 帖子发布与管理
- 评论系统 (二级评论)
- 点赞收藏 (HyperLogLog去重)
- Elasticsearch全文搜索

### 5. 分析服务 (Analysis Service)
- MFA发音分析
- 语法流利度评分
- 学习数据统计
- 排名系统 (Redis Sorted Set)
- 报表导出 (Excel/CSV)

### 6. 通知服务 (Notification Service)
- 系统通知
- WebSocket实时推送
- Kafka异步消息
- 未读数统计

### 7. AI网关 (AI Gateway)
- 模型管理 (Ollama/远程API/本地)
- 路由策略 (权重/优先级/轮询)
- 健康检查
- 指标统计

### 8. AI服务 (AI Service)
- LangGraph Agent
- Milvus向量记忆
- Redis短期上下文
- 工具系统 (可扩展)
- 英语口语反馈

### 9. 管理服务 (Admin Service)
- 系统仪表盘
- 配置管理
- 日志查询
- 系统监控

---

## 开发指南

### 代码规范
- 所有注释使用中文
- 实体类继承BaseEntity
- API返回值使用Result封装
- 业务异常使用BusinessException
- 用户端API前缀: `/user/`
- 管理端API前缀: `/admin/`

### 数据库规范
- 使用MyBatis-Plus
- 逻辑删除 (deleted字段)
- 自动填充 (createTime, updateTime)
- 统一字段命名 (驼峰转下划线)

### Redis Database分配
| DB | 服务 |
|----|------|
| 0 | User Service / API Gateway |
| 1 | Practice Service |
| 3 | Meeting Service |
| 4 | Community Service |
| 5 | Notification Service / Analysis Service |
| 6 | Admin Service |
| 7 | AI Gateway |
| 8 | AI Service |

---

## 文档索引
- [完整启动指南](document/启动部署指南.md) - 详细的启动说明
- [产品原型](document/产品原型.md) - 最初的功能设计
- [开发规范](document/开发基本rules.md) - 开发前制定的规范
- 其他文档：
- 1. document目录下有一些项目相关的解释说明
- 2. 有些服务（xx-service）的根目录下有单个服务的说明文档


## ⚠️ 常见问题

### Q: Docker启动失败?
**A**: 检查Docker Desktop是否运行,端口是否被占用

### Q: Java服务找不到common模块?
**A**: 确保已执行 `mvn install -pl common`

### Q: Python虚拟环境激活失败?
**A**: PowerShell需要先执行 `Set-ExecutionPolicy -ExecutionPolicy RemoteSigned -Scope CurrentUser`

### Q: Nacos注册失败?
**A**: 等待Nacos完全启动(约30秒),访问 http://localhost:8848/nacos 确认


---

## 👥团队

SpeakMaster Team

---

## 许可

本项目仅供学习和研究使用。

---

## 🔗 相关链接

- [Spring Boot](https://spring.io/projects/spring-boot)
- [Spring Cloud](https://spring.io/projects/spring-cloud)
- [Nacos](https://nacos.io/)
- [FastAPI](https://fastapi.tiangolo.com/)
- [LangGraph](https://github.com/langchain-ai/langgraph)
- [Vue.js](https://vuejs.org/)
- [Element Plus](https://element-plus.org/)
- [Vant](https://vant-ui.github.io/)

---

**最后更新**: 2026-03-06
