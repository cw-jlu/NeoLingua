# 快速极限并发测试 - 接口覆盖清单

## 测试概述
- **测试文件**: `tests/quick_max_concurrency_test.py`
- **测试策略**: 从100开始，每次增加100并发，找出成功率>=90%的最高并发数（精确到百位）
- **测试范围**: 所有用户实际使用的接口（不包括health检查接口）

## 接口覆盖清单 (共21个接口)

### 一、AI聊天相关 (5个接口)

1. **AI聊天** - `POST /ai/chat`
   - 核心对话功能
   - 测试数据: 简单问候消息

2. **AI模型列表** - `GET /ai/models`
   - 获取可用模型列表
   - 从AI Gateway获取

3. **停止生成** - `POST /ai/chat/stop`
   - 停止AI生成响应
   - 测试会话控制

4. **生成反馈** - `POST /ai/feedback`
   - 生成纠错反馈
   - 测试语法纠正场景

5. **发音分析** - `POST /ai/pronunciation`
   - 分析发音质量
   - 测试基础发音评估

### 二、高级功能 (11个接口)

6. **中式英语检测** - `POST /advanced/chinglish/detect`
   - 识别中式英语表达
   - 测试常见错误模式

7. **实时交互启动** - `POST /advanced/realtime/start`
   - 启动实时对话会话
   - 测试多种交互模式

8. **实时音频处理** - `POST /advanced/realtime/audio`
   - 处理实时音频流
   - 测试音频特征分析

9. **会话状态** - `GET /advanced/session/status/{session_id}`
   - 获取会话实时状态
   - 测试状态查询

10. **更新交互模式** - `PUT /advanced/session/mode/{session_id}`
    - 动态切换交互模式
    - 测试模式更新

11. **结束会话** - `POST /advanced/realtime/end`
    - 结束实时对话
    - 测试会话清理

12. **口语评估** - `POST /advanced/assessment/speaking`
    - IELTS/TOEFL口语评分
    - 测试多维度评估

13. **补全帮助** - `POST /advanced/completion/help`
    - 智能补全建议
    - 测试引导式补全

14. **进度追踪** - `GET /advanced/progress/{user_id}`
    - 学习进度分析
    - 测试30天进度

15. **批量评估** - `POST /advanced/batch/assess`
    - 批量会话评估
    - 测试多会话处理

16. **分析概览** - `GET /advanced/analytics/overview/{user_id}`
    - 综合学习分析
    - 测试数据聚合

### 三、RAG知识库 (5个接口)

17. **列出全局文档** - `GET /ai/rag/documents`
    - 查看全局知识库
    - 管理端功能

18. **搜索文档** - `POST /ai/rag/search`
    - 全局知识库检索
    - 测试RAG搜索

19. **列出用户文档** - `GET /ai/rag/user/documents`
    - 查看个人知识库
    - 用户端功能

20. **列出用户角色** - `GET /ai/rag/user/roles`
    - 查看创建的角色
    - 角色管理功能

21. **用户搜索文档** - `POST /ai/rag/user/search`
    - 个人知识库检索
    - 支持全局+个人混合搜索

## 测试特点

### 1. 全面覆盖
- ✅ 覆盖所有用户实际使用的接口
- ✅ 包含GET、POST、PUT、DELETE多种HTTP方法
- ✅ 测试简单查询和复杂业务逻辑

### 2. 真实场景
- 使用真实的请求数据
- 模拟实际用户行为
- 测试完整的业务流程

### 3. 性能指标
- 极限并发数（成功率>=90%）
- QPS（每秒请求数）
- 响应时间

### 4. 排除项
- ❌ 不测试health检查接口（用户不使用）
- ❌ 不测试文件上传接口（需要特殊处理）
- ❌ 不测试流式接口（SSE需要特殊处理）

## 运行测试

```bash
# 确保服务已启动
cd tests
python quick_max_concurrency_test.py
```

## 测试结果

测试完成后会生成：
- **控制台输出**: 实时显示测试进度和结果
- **JSON文件**: `tests/quick_max_concurrency_results.json`
  - 包含每个接口的极限并发数
  - 按并发能力排序
  - 标注最高和最低并发接口

## 预期用途

1. **性能基准**: 了解各接口的并发处理能力
2. **瓶颈识别**: 找出性能最弱的接口
3. **容量规划**: 为生产环境提供参考数据
4. **优化指导**: 指导性能优化方向

## 注意事项

⚠️ **测试前准备**:
- 确保所有服务正常运行
- 确保数据库连接正常
- 确保Redis可用
- 确保Milvus可用（RAG功能）

⚠️ **测试影响**:
- 测试会产生大量请求
- 可能影响服务性能
- 建议在测试环境运行
- 避免在生产环境运行

⚠️ **测试时长**:
- 每个接口测试约1-3分钟
- 总测试时长约30-60分钟
- 取决于服务响应速度
