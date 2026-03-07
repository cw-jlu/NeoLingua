"""
LangGraph Agent服务
单节点Agent，集成工具调用、记忆检索
模型推理通过AI Gateway调用，由AI Gateway负责模型选择和路由
"""
import json
import asyncio
import time
from typing import AsyncGenerator, Optional, List, Dict, Any
from loguru import logger

from app.config import settings
from app.services import redis_service, memory_service
from app.services.conversation_strategy_service import conversation_strategy_service
from app.services.assessment_service import assessment_service
from app.services.realtime_interaction_service import realtime_interaction_service
from app.tools.base import tool_registry


# ==================== AI Gateway 客户端 ====================

async def _call_ai_gateway(messages: List[Dict[str, str]],
                           model_id: Optional[int] = None,
                           session_id: Optional[str] = None,
                           audio_url: Optional[str] = None,
                           temperature: Optional[float] = None,
                           max_tokens: Optional[int] = None) -> Dict[str, Any]:
    """
    通过AI Gateway调用模型（同步）
    AI Gateway会根据model_id或路由规则选择具体模型（Ollama/远程API/本地千问）
    """
    import httpx

    payload = {
        "sessionId": session_id,
        "messages": [{"role": m["role"], "content": m["content"]} for m in messages],
    }
    if model_id is not None:
        payload["modelId"] = model_id
    if audio_url is not None:
        payload["audioUrl"] = audio_url  # 多模态模型使用
    if temperature is not None:
        payload["temperature"] = temperature
    if max_tokens is not None:
        payload["maxTokens"] = max_tokens

    url = f"{settings.AI_GATEWAY_URL}{settings.AI_GATEWAY_CHAT_PATH}"

    async with httpx.AsyncClient(timeout=120.0) as client:
        response = await client.post(url, json=payload)
        response.raise_for_status()
        result = response.json()

        # AI Gateway返回格式: {"code": 200, "msg": "success", "data": {...}}
        if result.get("code") != 200:
            raise Exception(f"AI Gateway错误: {result.get('msg', '未知错误')}")

        data = result.get("data", {})
        return {
            "content": data.get("content", ""),
            "model_id": data.get("modelId"),
            "model_name": data.get("modelName"),
            "token_count": data.get("tokenCount", 0),
            "response_time": data.get("responseTime", 0)
        }


async def _stream_ai_gateway(messages: List[Dict[str, str]],
                              model_id: Optional[int] = None,
                              session_id: Optional[str] = None) -> AsyncGenerator[Dict[str, Any], None]:
    """
    通过AI Gateway流式调用模型（SSE）
    """
    import httpx

    payload = {
        "sessionId": session_id,
        "messages": [{"role": m["role"], "content": m["content"]} for m in messages],
    }
    if model_id is not None:
        payload["modelId"] = model_id

    url = f"{settings.AI_GATEWAY_URL}{settings.AI_GATEWAY_STREAM_PATH}"

    async with httpx.AsyncClient(timeout=120.0) as client:
        async with client.stream("POST", url, json=payload,
                                  headers={"Accept": "text/event-stream"}) as response:
            async for line in response.aiter_lines():
                if not line or not line.startswith("data:"):
                    continue
                try:
                    # SSE格式: data:{"type":"content","content":"...","modelId":1}
                    data_str = line[5:].strip()  # 去掉 "data:" 前缀
                    event = json.loads(data_str)
                    yield event
                except (json.JSONDecodeError, Exception):
                    continue


async def _get_available_models() -> List[Dict[str, Any]]:
    """从AI Gateway获取可用模型列表"""
    import httpx
    try:
        url = f"{settings.AI_GATEWAY_URL}{settings.AI_GATEWAY_MODELS_PATH}"
        async with httpx.AsyncClient(timeout=10.0) as client:
            response = await client.get(url)
            response.raise_for_status()
            result = response.json()
            if result.get("code") == 200:
                return result.get("data", [])
    except Exception as e:
        logger.warning(f"获取模型列表失败: {e}")
    return []


# ==================== 提示词构建 ====================

def _build_enhanced_system_prompt(role_prompt: Optional[str] = None,
                                theme: Optional[str] = None,
                                memories: Optional[List[Dict]] = None,
                                rag_docs: Optional[List[Dict]] = None,
                                role_name: Optional[str] = None,
                                advanced_analysis: Optional[Dict] = None,
                                user_profile: Optional[Dict] = None) -> str:
    """构建增强系统提示词，整合高级功能分析结果"""
    base_prompt = _build_system_prompt(role_prompt, theme, memories, rag_docs, role_name)
    
    if not advanced_analysis:
        return base_prompt
    
    # 添加高级功能指导
    enhanced_prompt = base_prompt + "\n\n=== 高级交互指导 ===\n"
    
    # 中式英语处理指导
    if advanced_analysis.get("chinglish", {}).get("has_chinglish"):
        chinglish_info = advanced_analysis["chinglish"]
        enhanced_prompt += f"⚠️ 检测到中式英语表达（严重度：{chinglish_info['severity']}）\n"
        enhanced_prompt += "请在回复中自然地提供正确的英语表达方式，但不要过于直接地指出错误。\n"
    
    # 情感状态处理指导
    emotional_state = advanced_analysis.get("emotional_state", "confident")
    if emotional_state == "anxious":
        enhanced_prompt += "🌟 用户显示焦虑情绪，请使用鼓励性语言，放慢节奏，提供支持。\n"
    elif emotional_state == "frustrated":
        enhanced_prompt += "🤝 用户可能感到沮丧，请表示理解，提供替代方法，保持耐心。\n"
    elif emotional_state == "confident":
        enhanced_prompt += "🎯 用户表现自信，可以适当增加挑战性，给予积极反馈。\n"
    
    # 即时反馈指导
    if advanced_analysis.get("should_immediate_feedback"):
        enhanced_prompt += "⚡ 建议提供即时反馈，但要以鼓励为主，纠错为辅。\n"
    
    # 用户水平适配
    if user_profile:
        user_level = user_profile.get("level", "intermediate")
        enhanced_prompt += f"📊 用户水平：{user_level}\n"
        if user_level == "beginner":
            enhanced_prompt += "请使用简单词汇，说话慢一些，多给予鼓励。\n"
        elif user_level == "advanced":
            enhanced_prompt += "可以使用复杂表达，提供更深入的讨论。\n"
    
    enhanced_prompt += "==================\n"
    
    return enhanced_prompt


def _build_system_prompt(role_prompt: Optional[str] = None,
                         theme: Optional[str] = None,
                         memories: Optional[List[Dict]] = None,
                         rag_docs: Optional[List[Dict]] = None,
                         role_name: Optional[str] = None) -> str:
    """构建系统提示词,整合角色设定、历史记忆、RAG知识"""
    base_prompt = (
        "你是SpeakMaster的AI英语口语练习助手。你的任务是帮助中国学生练习英语口语。\n"
        "请用英语与用户对话，但在给出反馈和纠正时可以使用中文解释。\n"
        "注意纠正中式英语(Chinglish)的常见错误，如th/r发音混淆等。\n"
        "保持对话自然、友好，鼓励用户多说英语。\n"
    )

    # 角色特定设定
    if role_name:
        base_prompt += f"\n🎭 当前角色: {role_name}\n"
        if role_prompt:
            base_prompt += f"角色设定: {role_prompt}\n"
        else:
            base_prompt += f"请扮演 {role_name} 这个角色与用户对话。\n"

    if theme:
        base_prompt += f"\n当前对话主题: {theme}\n"

    # 注入 RAG 知识库检索结果
    if rag_docs:
        # 分类显示用户角色知识和全局知识
        user_docs = [d for d in rag_docs if d.get("scope") == "user"]
        global_docs = [d for d in rag_docs if d.get("scope") == "global"]
        
        if user_docs:
            role_knowledge = "\n---\n".join([d.get("content", "") for d in user_docs[:3]])
            if role_knowledge:
                role_display = user_docs[0].get("role_name", role_name or "角色")
                base_prompt += f"\n=== {role_display} 专属知识库 ===\n{role_knowledge}\n========================\n"
                base_prompt += f"请基于 {role_display} 的专属知识回答用户问题，保持角色一致性。\n"
        
        if global_docs:
            global_knowledge = "\n---\n".join([d.get("content", "") for d in global_docs[:2]])
            if global_knowledge:
                base_prompt += f"\n=== 通用知识库 ===\n{global_knowledge}\n================\n"
                base_prompt += "以上是补充的通用知识，可作为参考。\n"

    # 注入用户历史记忆
    if memories:
        memory_text = "\n".join([f"- {m.get('content', '')}" for m in memories[:3]])
        if memory_text:
            base_prompt += f"\n=== 用户历史记忆 ===\n{memory_text}\n===================\n"

    # 告知Agent可用工具
    tools = tool_registry.list_tools()
    if tools:
        tool_desc = "\n".join([f"- {t['name']}: {t['description']}" for t in tools])
        base_prompt += f"\n你可以使用以下工具:\n{tool_desc}\n"
        base_prompt += (
            "\n当需要使用工具时，请在回复中使用以下格式:\n"
            "[TOOL_CALL: 工具名称(参数)]\n"
            "工具返回结果后，请基于结果继续回答用户。\n"
        )

    return base_prompt


# ==================== 工具调用解析与执行 ====================

def _parse_tool_calls(text: str) -> List[Dict[str, Any]]:
    """解析回复中的工具调用"""
    import re
    tool_calls = []
    pattern = r'\[TOOL_CALL:\s*(.+?)\((.+?)\)\]'
    matches = re.findall(pattern, text)
    for name, params in matches:
        tool_calls.append({"name": name.strip(), "parameters": params.strip()})
    return tool_calls


def _execute_tool_calls(tool_calls: List[Dict[str, Any]]) -> str:
    """执行工具调用并返回结果"""
    results = []
    tools = {t["name"]: t["tool_id"] for t in tool_registry.list_tools()}

    for call in tool_calls:
        tool_name = call["name"]
        tool_id = tools.get(tool_name)
        if tool_id:
            try:
                params = {}
                param_str = call.get("parameters", "")
                if param_str and param_str != "None":
                    for part in param_str.split(","):
                        if "=" in part:
                            k, v = part.split("=", 1)
                            params[k.strip()] = v.strip().strip("'\"")
                        else:
                            tool_info = tool_registry.get_tool_info(tool_id)
                            if tool_info and tool_info.get("parameters"):
                                first_param = list(tool_info["parameters"].keys())[0]
                                params[first_param] = param_str.strip().strip("'\"")

                result = tool_registry.execute(tool_id, params)
                results.append(f"[{tool_name}结果]: {result}")
            except Exception as e:
                results.append(f"[{tool_name}错误]: {str(e)}")
        else:
            results.append(f"[未知工具: {tool_name}]")

    return "\n".join(results)


# ==================== 核心Agent方法 ====================

async def chat(session_id: str, user_id: str, message: str,
               model_id: Optional[int] = None,
               audio_url: Optional[str] = None,
               role_prompt: Optional[str] = None,
               theme: Optional[str] = None,
               role_name: Optional[str] = None,
               history: Optional[List[Dict]] = None,
               user_profile: Optional[Dict] = None,
               enable_advanced_features: bool = True) -> Dict[str, Any]:
    """
    增强版同步聊天 - 集成高级功能
    新增功能：
    1. 智能纠错反馈策略
    2. 中式英语检测与处理
    3. 情感感知与共鸣
    4. 动态评估与进度追踪
    """
    # 1. 检索用户历史记忆(Milvus)
    memories = memory_service.retrieve_memory(user_id, message, top_k=3)

    # 2. RAG 检索知识库(Milvus) - 支持角色特定知识库
    rag_docs = memory_service.search_documents(
        query=message, 
        top_k=5,
        user_id=user_id,
        role_name=role_name,
        include_global=True
    )

    # 3. 获取短期上下文(Redis)
    context_messages = redis_service.get_context_messages(session_id)

    # 4. 高级功能处理
    advanced_analysis = {}
    if enable_advanced_features:
        # 中式英语检测
        chinglish_analysis = await conversation_strategy_service.detect_chinglish_patterns(message)
        advanced_analysis["chinglish"] = chinglish_analysis
        
        # 情感状态检测
        emotional_state = await conversation_strategy_service.detect_emotional_state(message, context_messages)
        advanced_analysis["emotional_state"] = emotional_state
        
        # 创建对话上下文
        from app.services.conversation_strategy_service import ConversationContext, ConversationState
        conversation_context = ConversationContext(
            session_id=session_id,
            user_id=user_id,
            current_state=ConversationState.LISTENING,
            user_level=user_profile.get("level", "intermediate") if user_profile else "intermediate",
            emotional_state=emotional_state,
            silence_duration=0.0,
            last_user_input_time=time.time()
        )
        
        # 判断是否需要即时反馈
        should_immediate_feedback = await conversation_strategy_service.should_provide_immediate_feedback(
            conversation_context, message
        )
        advanced_analysis["should_immediate_feedback"] = should_immediate_feedback

    # 5. 构建增强系统提示词
    system_prompt = _build_enhanced_system_prompt(
        role_prompt, theme, memories, rag_docs, role_name, advanced_analysis, user_profile
    )

    # 6. 构建消息列表
    messages = [{"role": "system", "content": system_prompt}]
    for msg in context_messages:
        messages.append(msg)
    messages.append({"role": "user", "content": message})

    # 7. 通过AI Gateway调用模型
    try:
        gateway_result = await _call_ai_gateway(
            messages=messages,
            model_id=model_id,
            session_id=session_id,
            audio_url=audio_url
        )
        reply = gateway_result["content"]
        token_count = gateway_result.get("token_count", 0)
        used_model_id = gateway_result.get("model_id")
        used_model_name = gateway_result.get("model_name")
    except Exception as e:
        logger.error(f"AI Gateway调用失败: {e}")
        reply = "I'm sorry, I'm having trouble connecting to the AI service right now. Please try again later."
        token_count = 0
        used_model_id = None
        used_model_name = None

    # 8. 解析并执行工具调用
    tool_calls = _parse_tool_calls(reply)
    tool_results = None
    if tool_calls:
        tool_result_text = _execute_tool_calls(tool_calls)
        tool_results = tool_calls

        messages.append({"role": "assistant", "content": reply})
        messages.append({"role": "user", "content": f"工具执行结果:\n{tool_result_text}\n请基于以上结果回答用户。"})

        try:
            gateway_result = await _call_ai_gateway(
                messages=messages,
                model_id=model_id or used_model_id,
                session_id=session_id
            )
            reply = gateway_result["content"]
            token_count += gateway_result.get("token_count", 0)
        except Exception as e:
            logger.error(f"工具结果后AI Gateway调用失败: {e}")

    # 9. 高级功能后处理
    if enable_advanced_features:
        # 生成情感共鸣回应
        if advanced_analysis.get("emotional_state") in ["anxious", "frustrated"]:
            empathetic_response = await conversation_strategy_service.generate_empathetic_response(
                conversation_context, message
            )
            advanced_analysis["empathetic_response"] = empathetic_response
        
        # 引导式补全（如果用户表达不完整）
        if len(message.split()) < 5 and "?" not in message:
            completion_help = await conversation_strategy_service.generate_guided_completion(
                conversation_context, message
            )
            advanced_analysis["completion_help"] = completion_help

    # 10. 保存到短期上下文（Redis）
    redis_service.save_message(session_id, "user", message)
    redis_service.save_message(session_id, "assistant", reply)

    # 11. 保存到长期记忆(Milvus)
    memory_service.save_memory(user_id, session_id, f"用户: {message}\n助手: {reply}",
                               session_type="chat")

    return {
        "session_id": session_id,
        "reply": reply,
        "tool_calls": tool_results,
        "token_count": token_count,
        "model_id": used_model_id,
        "model_name": used_model_name,
        "rag_sources": len(rag_docs),
        "role_name": role_name,
        "advanced_analysis": advanced_analysis if enable_advanced_features else None
    }


async def chat_stream(session_id: str, user_id: str, message: str,
                       model_id: Optional[int] = None,
                       role_prompt: Optional[str] = None,
                       theme: Optional[str] = None) -> AsyncGenerator[str, None]:
    """
    流式聊天 - 通过AI Gateway的SSE接口
    """
    # 检索记忆和上下文
    memories = memory_service.retrieve_memory(user_id, message, top_k=3)
    rag_docs = memory_service.search_documents(message, top_k=3)
    context_messages = redis_service.get_context_messages(session_id)
    system_prompt = _build_system_prompt(role_prompt, theme, memories, rag_docs)

    messages = [{"role": "system", "content": system_prompt}]
    for msg in context_messages:
        messages.append(msg)
    messages.append({"role": "user", "content": message})

    full_reply = ""

    try:
        async for event in _stream_ai_gateway(
            messages=messages,
            model_id=model_id,
            session_id=session_id
        ):
            event_type = event.get("type", "")
            content = event.get("content", "")

            if event_type == "content" and content:
                full_reply += content
                event_data = json.dumps({
                    "type": "content",
                    "content": content,
                    "session_id": session_id,
                    "model_id": event.get("modelId")
                }, ensure_ascii=False)
                yield f"data: {event_data}\n\n"

            elif event_type == "done":
                done_data = json.dumps({
                    "type": "done",
                    "content": "",
                    "session_id": session_id,
                    "model_id": event.get("modelId")
                }, ensure_ascii=False)
                yield f"data: {done_data}\n\n"

            elif event_type == "error":
                error_data = json.dumps({
                    "type": "error",
                    "content": content,
                    "session_id": session_id
                }, ensure_ascii=False)
                yield f"data: {error_data}\n\n"

    except Exception as e:
        logger.error(f"流式聊天失败: {e}")
        error_data = json.dumps({
            "type": "error",
            "content": str(e),
            "session_id": session_id
        }, ensure_ascii=False)
        yield f"data: {error_data}\n\n"

    # 保存上下文
    redis_service.save_message(session_id, "user", message)
    if full_reply:
        redis_service.save_message(session_id, "assistant", full_reply)
        memory_service.save_memory(user_id, session_id, f"用户: {message}\n助手: {full_reply}",
                                   session_type="chat")


# 活跃的流式会话（用于停止生成）
_active_streams: Dict[str, bool] = {}


async def stop_generation(session_id: str):
    """
    停止流式生成 - 转发到AI Gateway
    """
    import httpx
    _active_streams[session_id] = False
    try:
        url = f"{settings.AI_GATEWAY_URL}{settings.AI_GATEWAY_STOP_PATH}"
        async with httpx.AsyncClient(timeout=10.0) as client:
            await client.post(url, json={"sessionId": session_id})
        logger.info(f"停止生成请求已转发到AI Gateway: session_id={session_id}")
    except Exception as e:
        logger.warning(f"转发停止生成请求失败: {e}")


async def generate_feedback(user_id: str, session_id: str,
                            message: str, context: Optional[str] = None,
                            model_id: Optional[int] = None) -> Dict[str, Any]:
    """
    生成英语口语反馈
    通过AI Gateway调用模型，分析用户的英语表达
    """
    prompt = (
        "你是一个专业的英语口语教练。请分析以下学生的英语表达，给出详细反馈。\n"
        "请用以下JSON格式回复（不要包含其他内容）:\n"
        '{"score": 1-5的评分, "feedback": "总体反馈", '
        '"corrections": [{"original": "原文", "corrected": "纠正后", "explanation": "中文解释"}], '
        '"example_audio_text": "正确的示范发音文本"}\n\n'
        f"学生的表达: {message}\n"
    )
    if context:
        prompt += f"对话上下文: {context}\n"

    messages = [
        {"role": "system", "content": "你是英语口语教练，专门帮助中国学生改进英语发音和表达。请严格按JSON格式回复。"},
        {"role": "user", "content": prompt}
    ]

    try:
        gateway_result = await _call_ai_gateway(
            messages=messages,
            model_id=model_id,
            session_id=session_id
        )
        content = gateway_result["content"]

        try:
            feedback = json.loads(content)
        except json.JSONDecodeError:
            # 尝试从回复中提取JSON
            import re
            json_match = re.search(r'\{.*\}', content, re.DOTALL)
            if json_match:
                feedback = json.loads(json_match.group())
            else:
                feedback = {
                    "score": 3,
                    "feedback": content,
                    "corrections": [],
                    "example_audio_text": ""
                }

        return feedback
    except Exception as e:
        logger.error(f"生成反馈失败: {e}")
        return {
            "score": 0,
            "feedback": f"反馈生成失败: {str(e)}",
            "corrections": [],
            "example_audio_text": ""
        }


async def analyze_pronunciation(user_id: str, text: str,
                                 reference_text: Optional[str] = None,
                                 model_id: Optional[int] = None) -> Dict[str, Any]:
    """
    发音分析 - 通过AI Gateway调用模型
    """
    prompt = (
        "你是一个英语发音专家。请分析以下文本的发音要点，"
        "特别关注中国学生常见的发音问题（如th/r/l/v等）。\n"
        "请用JSON格式回复:\n"
        '{"analysis": "发音分析", "tips": ["建议1", "建议2"], '
        '"problem_sounds": [{"sound": "音素", "advice": "建议"}]}\n\n'
        f"用户文本: {text}\n"
    )
    if reference_text:
        prompt += f"参考文本: {reference_text}\n"

    messages = [
        {"role": "system", "content": "你是英语发音专家，请严格按JSON格式回复。"},
        {"role": "user", "content": prompt}
    ]

    try:
        gateway_result = await _call_ai_gateway(
            messages=messages,
            model_id=model_id
        )
        content = gateway_result["content"]

        try:
            return json.loads(content)
        except json.JSONDecodeError:
            import re
            json_match = re.search(r'\{.*\}', content, re.DOTALL)
            if json_match:
                return json.loads(json_match.group())
            return {"analysis": content, "tips": [], "problem_sounds": []}
    except Exception as e:
        logger.error(f"发音分析失败: {e}")
        return {"analysis": f"分析失败: {str(e)}", "tips": [], "problem_sounds": []}


async def get_models() -> List[Dict[str, Any]]:
    """获取可用模型列表（从AI Gateway）"""
    return await _get_available_models()


# ==================== 新增高级功能API ====================

async def assess_pronunciation_advanced(user_id: str, session_id: str, 
                                       audio_path: str, text: str,
                                       user_profile: Optional[Dict] = None) -> Dict[str, Any]:
    """
    高级发音评估 - 集成智能反馈策略
    """
    try:
        from pathlib import Path
        from app.services.pronunciation_service import pronunciation_service
        
        # 调用增强的发音分析服务
        result = pronunciation_service.analyze(
            Path(audio_path), text, user_profile
        )
        
        # 如果有评估结果，进行进一步的智能分析
        if result.get("pronunciation_score", 0) > 0:
            # 保存评估结果到记忆系统
            memory_service.save_memory(
                user_id, session_id,
                f"发音评估 - 得分: {result['pronunciation_score']}, 反馈: {result.get('feedback', '')}",
                session_type="pronunciation_assessment"
            )
        
        return result
    except Exception as e:
        logger.error(f"高级发音评估失败: {e}")
        return {"error": str(e), "pronunciation_score": 0}


async def start_realtime_conversation(user_id: str, session_id: str,
                                    mode: str = "half_duplex",
                                    user_profile: Optional[Dict] = None) -> Dict[str, Any]:
    """
    启动实时对话会话
    """
    try:
        from app.services.realtime_interaction_service import InteractionMode
        
        # 转换模式
        interaction_mode = InteractionMode(mode)
        
        result = await realtime_interaction_service.start_realtime_session(
            session_id, user_id, interaction_mode, user_profile
        )
        
        return result
    except Exception as e:
        logger.error(f"启动实时对话失败: {e}")
        return {"error": str(e)}


async def process_realtime_audio(session_id: str, audio_features: Dict,
                               transcribed_text: Optional[str] = None) -> Dict[str, Any]:
    """
    处理实时音频流
    """
    try:
        from app.services.realtime_interaction_service import AudioFeatures
        
        # 转换音频特征
        features = AudioFeatures(
            energy_level=audio_features.get("energy_level", 0.5),
            pitch_variance=audio_features.get("pitch_variance", 0.5),
            speech_rate=audio_features.get("speech_rate", 150),
            pause_duration=audio_features.get("pause_duration", 0.0),
            voice_activity=audio_features.get("voice_activity", True),
            confidence_score=audio_features.get("confidence_score", 0.8)
        )
        
        result = await realtime_interaction_service.process_audio_stream(
            session_id, features, transcribed_text
        )
        
        return result
    except Exception as e:
        logger.error(f"处理实时音频失败: {e}")
        return {"error": str(e)}


async def get_speaking_assessment(user_id: str, session_id: str,
                                user_message: str, audio_analysis: Optional[Dict] = None,
                                assessment_type: str = "ielts") -> Dict[str, Any]:
    """
    获取口语评估结果
    """
    try:
        # 获取对话上下文
        context_messages = redis_service.get_context_messages(session_id)
        
        result = await assessment_service.assess_speaking_performance(
            user_id, session_id, user_message, audio_analysis, 
            context_messages, assessment_type
        )
        
        # 转换为可序列化的格式
        return {
            "user_id": result.user_id,
            "session_id": result.session_id,
            "timestamp": result.timestamp.isoformat(),
            "overall_score": result.overall_score,
            "proficiency_level": result.proficiency_level.value,
            "dimension_scores": {k.value: v for k, v in result.dimension_scores.items()},
            "detailed_feedback": result.detailed_feedback,
            "improvement_suggestions": result.improvement_suggestions,
            "next_level_requirements": result.next_level_requirements
        }
    except Exception as e:
        logger.error(f"口语评估失败: {e}")
        return {"error": str(e)}


async def get_progress_tracking(user_id: str, days: int = 30) -> Dict[str, Any]:
    """
    获取学习进度追踪
    """
    try:
        result = await assessment_service.get_progress_tracking(user_id, days)
        return result
    except Exception as e:
        logger.error(f"获取进度追踪失败: {e}")
        return {"error": str(e)}


async def detect_chinglish(text: str) -> Dict[str, Any]:
    """
    检测中式英语表达
    """
    try:
        result = await conversation_strategy_service.detect_chinglish_patterns(text)
        return result
    except Exception as e:
        logger.error(f"中式英语检测失败: {e}")
        return {"error": str(e), "has_chinglish": False}


async def generate_completion_help(user_id: str, session_id: str, 
                                 incomplete_text: str,
                                 user_profile: Optional[Dict] = None) -> Dict[str, Any]:
    """
    生成引导式补全帮助
    """
    try:
        from app.services.conversation_strategy_service import ConversationContext, ConversationState
        import time
        
        # 创建对话上下文
        context = ConversationContext(
            session_id=session_id,
            user_id=user_id,
            current_state=ConversationState.LISTENING,
            user_level=user_profile.get("level", "intermediate") if user_profile else "intermediate",
            emotional_state="confident",
            silence_duration=0.0,
            last_user_input_time=time.time(),
            conversation_topic=user_profile.get("topic") if user_profile else None
        )
        
        result = await conversation_strategy_service.generate_guided_completion(
            context, incomplete_text
        )
        
        return result
    except Exception as e:
        logger.error(f"生成补全帮助失败: {e}")
        return {"error": str(e)}


async def end_realtime_session(session_id: str) -> Dict[str, Any]:
    """
    结束实时对话会话
    """
    try:
        result = await realtime_interaction_service.end_realtime_session(session_id)
        return result
    except Exception as e:
        logger.error(f"结束实时会话失败: {e}")
        return {"error": str(e)}
