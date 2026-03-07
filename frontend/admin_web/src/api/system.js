/** 系统管理API */
import request from '@/utils/request'

// 仪表盘
export const getDashboard = () => request.get('/admin/dashboard')
export const getDashboardOverview = () => request.get('/admin/dashboard/overview')

// 系统配置
export const getConfigList = (params) => request.get('/admin/system/config', { params })
export const createConfig = (data) => request.post('/admin/system/config', data)
export const updateConfig = (key, data) => request.put(`/admin/system/config/${key}`, data)
export const deleteConfig = (key) => request.delete(`/admin/system/config/${key}`)

// 系统日志
export const getLogList = (params) => request.get('/admin/logs', { params })
export const deleteLog = (id) => request.delete(`/admin/logs/${id}`)

// 系统监控
export const getMonitorHealth = () => request.get('/admin/monitor/health')
export const getMonitorMetrics = () => request.get('/admin/monitor/metrics')
export const getMonitorServices = () => request.get('/admin/monitor/services')
