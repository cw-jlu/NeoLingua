import request from '@/utils/request'

// 上传文档到知识库
export function uploadDocument(formData) {
  return request({
    url: '/ai/rag/documents/upload',
    method: 'post',
    data: formData,
    headers: { 'Content-Type': 'multipart/form-data' },
  })
}

// 获取知识库文档列表
export function listDocuments() {
  return request({ url: '/ai/rag/documents', method: 'get' })
}

// 删除文档
export function deleteDocument(docId) {
  return request({ url: `/ai/rag/documents/${docId}`, method: 'delete' })
}

// 测试检索
export function searchDocuments(query, topK = 5) {
  return request({
    url: '/ai/rag/search',
    method: 'post',
    params: { query, top_k: topK },
  })
}
