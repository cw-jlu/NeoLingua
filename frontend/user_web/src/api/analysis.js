/** 分析API */
import request from '@/utils/request'

export const getMyReports = (params) => request.get('/user/analysis/reports', { params })
export const getSessionReports = (sessionId, params) => request.get(`/user/analysis/reports/session/${sessionId}`, { params })
export const getMyStatistics = (userId) => request.get('/user/analysis/statistics', { params: { user_id: userId } })
export const getRanking = (top = 50) => request.get('/user/analysis/ranking', { params: { top } })
export const getMyRank = (userId) => request.get('/user/analysis/ranking/me', { params: { user_id: userId } })
export const exportReport = (userId) => request.get('/user/analysis/export', { params: { user_id: userId }, responseType: 'blob' })
