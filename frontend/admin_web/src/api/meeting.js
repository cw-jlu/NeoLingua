/** Meeting管理API */
import request from '@/utils/request'

export const getMeetingList = (params) => request.get('/admin/meetings', { params })
export const getMeetingById = (id) => request.get(`/admin/meetings/${id}`)
export const deleteMeeting = (id) => request.delete(`/admin/meetings/${id}`)
export const closeMeeting = (id) => request.post(`/admin/meetings/${id}/close`)
export const getMeetingStatistics = () => request.get('/admin/meetings/statistics')

// 好友关系
export const getFriendList = (params) => request.get('/admin/friends/relationships', { params })
export const deleteFriend = (id) => request.delete(`/admin/friends/relationships/${id}`)
