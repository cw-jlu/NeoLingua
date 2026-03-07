/** 用户API */
import request from '@/utils/request'

export const getProfile = () => request.get('/user/profile')
export const updateProfile = (data) => request.put('/user/profile', data)
export const getPoints = () => request.get('/user/points')
export const getSignInStatus = () => request.get('/user/signin/status')
export const signIn = () => request.post('/user/signin')
export const getSignInCalendar = (month) => request.get('/user/signin/calendar', { params: { month } })
export const getBadges = () => request.get('/user/badges')
