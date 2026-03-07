# Analysis Service (分析服务)

## 模块功能
- 学习数据分析（语法、发音、流利度评分）
- 发音分析（通过MFA，独立conda环境隔离）
- 用户排名（Redis Sorted Set）
- 报表导出（Excel/CSV）
- Kafka消费者（异步处理对话消息）
- 音频归档（MinIO）

## 端口
8085

## 技术栈
- FastAPI + SQLAlchemy
- Redis（排名 + 缓存）
- Kafka（消息消费/生产）
- MinIO（音频归档）
- MFA（发音对齐，独立环境）
- pandas + openpyxl（报表）

## API接口

### 用户端 (/user/analysis)
- GET /user/analysis/reports - 获取我的分析报告
- GET /user/analysis/reports/session/{id} - 获取会话报告
- GET /user/analysis/statistics - 获取我的统计
- GET /user/analysis/ranking - 获取排名榜
- GET /user/analysis/ranking/me - 获取我的排名
- GET /user/analysis/export - 导出报表

### 管理端 (/admin/analysis)
- GET /admin/analysis/reports - 获取所有报告
- GET /admin/analysis/reports/{id} - 报告详情
- DELETE /admin/analysis/reports/{id} - 删除报告
- GET /admin/analysis/overview - 统计概览
- GET /admin/analysis/user/{id}/statistics - 用户统计
- GET /admin/analysis/ranking - 排名榜
- GET /admin/analysis/statistics - 服务统计

### 健康检查
- GET /health - 健康检查

## 数据库表
- analysis_report - 分析报告
- learning_record - 学习记录（每日汇总）
- user_ranking - 用户排名快照

## 启动方式

### API服务
```bash
cd backend/analysis_service
pip install -r requirements.txt
python -m app.main
```

### Kafka Worker（独立进程）
```bash
python -m app.worker
```

## MFA环境配置（避免依赖冲突）

MFA需要独立的conda环境，不要安装在主服务环境中：

```bash
# 创建独立MFA环境
micromamba create -n mfa -c conda-forge python=3.10 montreal-forced-aligner -y

# 配置.env中的MFA_PYTHON_PATH指向该环境的python
# Windows: MFA_PYTHON_PATH=C:/Users/xxx/micromamba/envs/mfa/python.exe
# Linux: MFA_PYTHON_PATH=/home/xxx/micromamba/envs/mfa/bin/python
```

## Redis数据结构
- analysis:ranking:overall (Sorted Set) - 用户排名
- analysis:user:stats:{user_id} (String/JSON) - 用户统计缓存

## 注意事项
1. MFA依赖通过子进程调用独立环境，不会与主服务冲突
2. Worker进程需要单独启动，负责消费Kafka消息
3. Redis使用database 5
4. 音频文件先本地存储，分析后归档到MinIO
