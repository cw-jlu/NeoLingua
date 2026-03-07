/** 通知API */
import request from '@/utils/request'

export const getNotifications = (params) => request.get('/user/notifications', { params })
export const markAsRead = (id) => request.put(`/user/notifications/${id}/read`)
export const markAllRead = () => request.put('/user/notifications/read-all')
export const getUnreadCount = () => request.get('/user/notifications/unread/count')
