/** 认证API */
import request from '@/utils/request'

export const login = (data) => request.post('/user/auth/login', data)
export const register = (data) => request.post('/user/auth/register', data)
export const logout = () => request.post('/user/auth/logout')
export const refreshToken = () => request.get('/user/auth/refresh')
