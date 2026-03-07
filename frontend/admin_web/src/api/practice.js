/** 练习管理API */
import request from '@/utils/request'

// 主题管理
export const getThemeList = (params) => request.get('/admin/practice/themes', { params })
export const createTheme = (data) => request.post('/admin/practice/themes', data)
export const updateTheme = (id, data) => request.put(`/admin/practice/themes/${id}`, data)
export const deleteTheme = (id) => request.delete(`/admin/practice/themes/${id}`)

// 角色管理
export const getRoleList = (params) => request.get('/admin/practice/roles', { params })
export const createRole = (data) => request.post('/admin/practice/roles', data)
export const updateRole = (id, data) => request.put(`/admin/practice/roles/${id}`, data)
export const deleteRole = (id) => request.delete(`/admin/practice/roles/${id}`)

// 会话管理
export const getSessionList = (params) => request.get('/admin/practice/sessions', { params })
export const deleteSession = (id) => request.delete(`/admin/practice/sessions/${id}`)
export const getSessionStatistics = () => request.get('/admin/practice/sessions/statistics')
