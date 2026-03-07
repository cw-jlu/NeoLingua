/** 分析管理API */
import request from '@/utils/request'

export const getAnalysisReports = (params) => request.get('/admin/analysis/reports', { params })
export const getAnalysisReportById = (id) => request.get(`/admin/analysis/reports/${id}`)
export const deleteAnalysisReport = (id) => request.delete(`/admin/analysis/reports/${id}`)
export const getAnalysisOverview = () => request.get('/admin/analysis/overview')
export const getUserStatistics = (userId) => request.get(`/admin/analysis/user/${userId}/statistics`)
export const getAnalysisRanking = (params) => request.get('/admin/analysis/ranking', { params })
export const getAnalysisStatistics = () => request.get('/admin/analysis/statistics')
