/** 用户管理API */
import request from '@/utils/request'

// 用户列表
export const getUserList = (params) => request.get('/admin/users', { params })
// 用户详情
export const getUserById = (id) => request.get(`/admin/users/${id}`)
// 创建用户
export const createUser = (data) => request.post('/admin/users', data)
// 更新用户
export const updateUser = (id, data) => request.put(`/admin/users/${id}`, data)
// 删除用户
export const deleteUser = (id) => request.delete(`/admin/users/${id}`)
// 封禁用户
export const banUser = (id) => request.post(`/admin/users/${id}/ban`)
// 解封用户
export const unbanUser = (id) => request.post(`/admin/users/${id}/unban`)
// 重置密码
export const resetPassword = (id) => request.post(`/admin/users/${id}/reset-password`)
// 用户统计
export const getUserStatistics = () => request.get('/admin/users/statistics')

// 徽章管理
export const getBadgeList = (params) => request.get('/admin/badges', { params })
export const createBadge = (data) => request.post('/admin/badges', data)
export const updateBadge = (id, data) => request.put(`/admin/badges/${id}`, data)
export const deleteBadge = (id) => request.delete(`/admin/badges/${id}`)
