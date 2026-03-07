/** AI网关管理API */
import request from '@/utils/request'

// 模型管理
export const getModelList = (params) => request.get('/admin/ai/models', { params })
export const getModelById = (id) => request.get(`/admin/ai/models/${id}`)
export const createModel = (data) => request.post('/admin/ai/models', data)
export const updateModel = (id, data) => request.put(`/admin/ai/models/${id}`, data)
export const deleteModel = (id) => request.delete(`/admin/ai/models/${id}`)
export const enableModel = (id) => request.post(`/admin/ai/models/${id}/enable`)
export const disableModel = (id) => request.post(`/admin/ai/models/${id}/disable`)
export const testModel = (id) => request.post(`/admin/ai/models/${id}/test`)
export const getModelHealth = (id) => request.get(`/admin/ai/models/${id}/health`)
export const getModelMetrics = (id) => request.get(`/admin/ai/models/${id}/metrics`)
export const getModelStatistics = () => request.get('/admin/ai/models/statistics')

// 路由规则
export const getRoutingRules = () => request.get('/admin/ai/routing/rules')
export const createRoutingRule = (data) => request.post('/admin/ai/routing/rules', data)
export const updateRoutingRule = (id, data) => request.put(`/admin/ai/routing/rules/${id}`, data)
export const deleteRoutingRule = (id) => request.delete(`/admin/ai/routing/rules/${id}`)
