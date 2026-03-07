"""
请求/响应数据模型
统一使用Result封装返回值
"""
from pydantic import BaseModel, Field
from typing import Any, Optional, List, Dict
from datetime import datetime
from enum import Enum


# ==================== 统一响应 ====================

class Result(BaseModel):
    """统一响应格式，与Java端Result<T>保持一致"""
    code: int = 200
    msg: str = "success"
    data: Any = None

    @staticmethod
    def success(data: Any = None, msg: str = "success") -> "Result":
        return Result(code=200, msg=msg, data=data)

    @staticmethod
    def error(code: int = 500, msg: str = "error", data: Any = None) -> "Result":
        return Result(code=code, msg=msg, data=data)


# ==================== 聊天相关 ====================

class MessageRole(str, Enum):
    USER = "user"
    ASSISTANT = "assistant"
    SYSTEM = "system"


class Message(BaseModel):
    """聊天消息"""
    role: MessageRole
    content: str
    timestamp: Optional[datetime] = None


class ChatRequest(BaseModel):
    """聊天请求"""
    session_id: str = Field(..., description="会话ID")
    user_id: str = Field(..., description="用户ID")
    message: str = Field(..., description="用户消息（STT识别后的文字）")
    audio_url: Optional[str] = Field(None, description="音频文件URL（MinIO），多模态模型使用")
    model_id: Optional[int] = Field(None, description="指定模型ID（可选，不指定则由AI Gateway路由选择）")
    role_prompt: Optional[str] = Field(None, description="角色提示词")
    theme: Optional[str] = Field(None, description="主题")
    history: Optional[List[Message]] = Field(default=[], description="历史消息")


class ChatResponse(BaseModel):
    """聊天响应"""
    session_id: str
    reply: str
    tool_calls: Optional[List[Dict[str, Any]]] = None
    token_count: Optional[int] = None
    model_id: Optional[int] = Field(None, description="使用的模型ID")
    model_name: Optional[str] = Field(None, description="使用的模型名称")


class StopRequest(BaseModel):
    """停止生成请求"""
    session_id: str


# ==================== 反馈相关 ====================

class FeedbackRequest(BaseModel):
    """反馈生成请求"""
    session_id: str
    user_id: str
    message: str = Field(..., description="用户的英语消息")
    context: Optional[str] = Field(None, description="对话上下文")
    model_id: Optional[int] = Field(None, description="指定模型ID（可选）")


class FeedbackResponse(BaseModel):
    """反馈响应"""
    score: int = Field(..., ge=1, le=5, description="评分1-5")
    feedback: str = Field(..., description="反馈建议")
    corrections: Optional[List[Dict[str, str]]] = Field(None, description="纠正列表")
    example_audio_text: Optional[str] = Field(None, description="示例发音文本")


# ==================== 发音分析 ====================

class PronunciationRequest(BaseModel):
    """发音分析请求"""
    user_id: str
    text: str = Field(..., description="用户说的文本")
    reference_text: Optional[str] = Field(None, description="参考文本")
    model_id: Optional[int] = Field(None, description="指定模型ID（可选）")


# ==================== 记忆相关 ====================

class MemorySaveRequest(BaseModel):
    """保存记忆请求"""
    session_id: str
    user_id: str
    content: str
    metadata: Optional[Dict[str, Any]] = None


class MemoryRetrieveRequest(BaseModel):
    """检索记忆请求"""
    user_id: str
    query: str
    top_k: int = Field(default=5, ge=1, le=20)


# ==================== 工具相关 ====================

class ToolInfo(BaseModel):
    """工具信息"""
    tool_id: str
    name: str
    description: str
    parameters: Optional[Dict[str, Any]] = None


class ToolRegisterRequest(BaseModel):
    """注册工具请求"""
    name: str
    description: str
    parameters: Optional[Dict[str, Any]] = None


class ToolExecuteRequest(BaseModel):
    """执行工具请求"""
    parameters: Optional[Dict[str, Any]] = None


# ==================== 会话相关 ====================

class SessionCreateRequest(BaseModel):
    """创建会话请求"""
    user_id: str
    theme: Optional[str] = None
    role_prompt: Optional[str] = None


class SessionInfo(BaseModel):
    """会话信息"""
    session_id: str
    user_id: str
    theme: Optional[str] = None
    role_prompt: Optional[str] = None
    created_at: str
    message_count: int = 0
