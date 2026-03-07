import request from '@/utils/request'

// ==================== 用户端RAG接口 ====================

// 上传角色资料
export function uploadRoleDocument(formData) {
  return request({
    url: '/ai/rag/user/documents/upload',
    method: 'post',
    data: formData,
    headers: { 'Content-Type': 'multipart/form-data' },
  })
}

// 获取用户的角色列表
export function getUserRoles() {
  return request({ 
    url: '/ai/rag/user/roles', 
    method: 'get' 
  })
}

// 获取用户的文档列表
export function getUserDocuments(roleName = null) {
  const params = roleName ? { role_name: roleName } : {}
  return request({ 
    url: '/ai/rag/user/documents', 
    method: 'get',
    params 
  })
}

// 删除用户文档
export function deleteUserDocument(docId) {
  return request({ 
    url: `/ai/rag/user/documents/${docId}`, 
    method: 'delete' 
  })
}

// 删除用户角色
export function deleteUserRole(roleName) {
  return request({ 
    url: `/ai/rag/user/roles/${encodeURIComponent(roleName)}`, 
    method: 'delete' 
  })
}

// 用户端RAG检索测试
export function searchUserDocuments(query, roleName = null, topK = 5, includeGlobal = true) {
  return request({
    url: '/ai/rag/user/search',
    method: 'post',
    data: {
      query,
      role_name: roleName,
      top_k: topK,
      include_global: includeGlobal
    }
  })
}

// ==================== 角色对话接口 ====================

// 与特定角色对话
export function chatWithRole(sessionId, message, roleName, rolePrompt = null) {
  return request({
    url: '/ai/chat',
    method: 'post',
    data: {
      session_id: sessionId,
      message,
      role_name: roleName,
      role_prompt: rolePrompt || `You are ${roleName}. Please stay in character and use the knowledge from uploaded documents about this role.`
    }
  })
}

// 流式对话（SSE）
export function streamChatWithRole(sessionId, message, roleName, rolePrompt = null) {
  return new EventSource(`/api/ai/chat/stream`, {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
    },
    body: JSON.stringify({
      session_id: sessionId,
      message,
      role_name: roleName,
      role_prompt: rolePrompt || `You are ${roleName}. Please stay in character.`
    })
  })
}