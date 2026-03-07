/** 练习API */
import request from '@/utils/request'

export const getThemes = (params) => request.get('/user/practice/themes', { params })
export const getRoles = (params) => request.get('/user/practice/roles', { params })
export const createSession = (data) => request.post('/user/practice/sessions', data)
export const getSession = (id) => request.get(`/user/practice/sessions/${id}`)
export const getMySessions = (params) => request.get('/user/practice/sessions', { params })
export const sendMessage = (sessionId, data) => request.post(`/user/practice/sessions/${sessionId}/messages`, data)
export const getMessages = (sessionId) => request.get(`/user/practice/sessions/${sessionId}/messages`)
