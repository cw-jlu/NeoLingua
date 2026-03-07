/** 通知管理API */
import request from '@/utils/request'

export const getNotificationList = (params) => request.get('/admin/notifications', { params })
export const deleteNotification = (id) => request.delete(`/admin/notifications/${id}`)
export const broadcast = (data) => request.post('/admin/notifications/broadcast', data)
export const sendTargeted = (data) => request.post('/admin/notifications/targeted', data)
export const getNotificationStatistics = () => request.get('/admin/notifications/statistics')
