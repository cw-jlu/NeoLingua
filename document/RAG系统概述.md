# RAG系统
### 包括三个：模型角色RAG，系统RAG，上下文RAG
## 系统RAG概述
### 系统RAG的目标是引入外部资料，减少幻觉
## 上下文RAG概述
### 主要是实现三级记忆
## 模型角色RAG概述
### 模型角色RAG主要是增强用户和系统的交互性个人化定制，增强趣味性，该功能又直接的prompt和RAG共同支持，RAG就是可以自动匹配需要的片段（但是应该有一个最低阈值）
##### 比如说用户现在可以通过上传资料来构建专属的AI角色伙伴！比如上传明日香的资料，创建一个了解明日香性格、背景的AI角色，进行更真实的角色扮演对话。


### 1. 角色资料上传
- **支持格式**: .txt, .md, .pdf
- **智能分块**: 自动将长文档分割成合适的知识片段
- **向量化存储**: 使用外部embedding API进行向量化
- **用户隔离**: 每个用户的角色库完全独立

### 2. 角色知识库管理
- **角色分类**: 按角色名称组织文档
- **文档管理**: 查看、删除角色相关文档
- **统计信息**: 显示每个角色的文档数量和知识片段数

### 3. 智能对话增强
- **角色一致性**: AI会基于上传的资料保持角色设定
- **知识检索**: 自动检索相关角色知识注入对话
- **混合知识库**: 结合用户角色知识和全局知识库

## 🚀 使用流程

### 步骤1: 创建角色知识库

1. **访问角色库页面**
   - 用户端: `/roles` 或通过个人资料页面进入
   - 点击"创建新角色"

2. **上传角色资料**
   ```
   角色名称: 明日香
   上传文件: asuka_character_profile.txt
   描述: 明日香的性格设定、背景故事、经典台词等
   ```

3. **系统处理**
   - 提取文本内容
   - 智能分块 (500字符/块，50字符重叠)
   - 调用embedding API向量化
   - 存储到Milvus向量数据库

### 步骤2: 与角色对话

1. **选择角色**
   - 新建对话时可选择"🎭 明日香"
   - 系统会自动加载角色专属知识库

2. **智能对话**
   - AI会基于上传的明日香资料进行角色扮演
   - 自动检索相关知识片段增强回复
   - 保持角色一致性和个性特征

### 步骤3: 管理角色库

1. **查看角色列表**
   - 显示所有创建的角色
   - 统计每个角色的文档数量

2. **添加更多资料**
   - 为现有角色上传更多文档
   - 丰富角色的知识背景

3. **测试对话**
   - 直接在角色库页面测试角色对话
   - 验证角色设定是否生效

## 📁 技术架构

### 后端API接口

#### 用户端RAG接口
```http
POST /ai/rag/user/documents/upload     # 上传角色资料
GET  /ai/rag/user/roles                # 获取用户角色列表
GET  /ai/rag/user/documents            # 获取角色文档列表
DELETE /ai/rag/user/documents/{docId}  # 删除文档
POST /ai/rag/user/search               # 测试角色知识检索
```

#### 对话接口增强
```http
POST /ai/chat                          # 支持role_name参数
```

### 数据存储结构

#### Milvus Collection: rag_documents
```python
{
    "doc_id": "uuid",
    "filename": "asuka_profile.txt", 
    "chunk_index": 0,
    "content": "明日香是一个...",
    "metadata": {
        "user_id": "user123",
        "role_name": "明日香",
        "scope": "user",  # user | global
        "description": "角色设定文档"
    },
    "embedding": [0.1, -0.2, ...]  # 768维向量
}
```

### 检索逻辑

```python
# 对话时的知识检索
def search_documents(query, user_id, role_name):
    # 1. 优先检索用户的角色知识库
    user_docs = search_user_role_docs(query, user_id, role_name)
    
    # 2. 补充全局知识库
    global_docs = search_global_docs(query)
    
    # 3. 合并结果，用户角色知识优先
    return user_docs + global_docs
```

##  使用场景示例

### 场景1: 动漫角色扮演

**上传资料**:
- `asuka_character.txt` - 明日香的性格特征
- `asuka_background.txt` - 明日香的背景故事  
- `asuka_quotes.txt` - 明日香的经典台词

**对话效果**:
```
用户: Hi Asuka, how are you today?
明日香: Hmph! I'm doing perfectly fine, obviously! 
      I'm the best Eva pilot, after all. 
      Why are you asking such an obvious question?
```

### 场景2: 历史人物对话

**上传资料**:
- `einstein_biography.pdf` - 爱因斯坦传记
- `einstein_theories.txt` - 相对论理论说明
- `einstein_quotes.md` - 爱因斯坦名言集

**对话效果**:
```
用户: What do you think about quantum mechanics?
爱因斯坦: Well, as I once said, "God does not play dice with the universe." 
         While quantum mechanics has proven to be remarkably successful...
```

### 场景3: 虚拟老师

**上传资料**:
- `english_grammar_rules.pdf` - 英语语法规则
- `common_mistakes.txt` - 中国学生常见错误
- `teaching_methods.md` - 教学方法论

**对话效果**:
```
用户: I have trouble with articles.
Professor Chen: That's a very common issue for Chinese learners! 
               Let me explain the difference between "a", "an", and "the"...
```

## 配置说明

### 环境变量配置

```bash
# backend/ai_service/.env

# Embedding配置 (必须)
EMBEDDING_PROVIDER=gemini
GEMINI_API_KEY=your_gemini_api_key_here

# Milvus配置
MILVUS_HOST=localhost
MILVUS_PORT=19530
EMBEDDING_DIM=768
```


## 系统优势

### 1. 个性化体验
- 每个用户可以创建专属的AI角色
- 基于真实资料的角色设定，更加生动
- 支持多个角色，满足不同场景需求

### 2. 智能知识融合
- 用户角色知识 + 全局知识库
- 自动相关性检索，提供准确信息
- 保持角色一致性的同时补充通用知识

### 3. 易用性设计
- 拖拽上传，支持多种文件格式
- 可视化角色管理界面
- 一键测试对话功能

### 4. 技术先进性
- 基于Milvus向量数据库的高效检索
- 支持多种embedding提供商
- 用户数据完全隔离，保护隐私

## 实际应用价值

### 英语学习场景
1. **角色扮演练习**: 与喜欢的角色用英语对话
2. **情景模拟**: 创建特定场景的虚拟角色
3. **个性化纠错**: 基于角色特点的学习建议

### 娱乐社交场景  
1. **粉丝互动**: 与喜爱的角色"真实"对话
2. **创意写作**: 角色协助进行故事创作
3. **情感陪伴**: 个性化的AI伙伴

### 教育培训场景
1. **专业导师**: 上传专业资料创建领域专家
2. **历史对话**: 与历史人物"面对面"交流
3. **语言练习**: 不同文化背景的对话伙伴

## 未来扩展

### 1. 多模态支持
- 支持图片、音频资料上传（目前不确定多模态大模型的RAG，比如音频的RAG，是经过ASR后做向量化还是直接进行向量化）
- 角色头像和语音合成
- 视频资料的文本提取

### 2. 角色市场
- 用户可以分享优质角色设定
- 角色模板库和推荐系统
- 社区评分和反馈机制

### 3. 高级功能
- 角色性格测试和分析
- 对话风格学习和模仿
- 多角色群聊功能

---

**文档更新**: 2026-03-05  
**技术栈**: Python + FastAPI + Milvus + Vue.js  
**核心价值**: 让每个用户都能拥有专属的AI角色伙伴！