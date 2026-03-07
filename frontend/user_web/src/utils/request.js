/**
 * Axios请求封装（用户端）
 */
import axios from 'axios'
import { showToast } from 'vant'
import { useAuthStore } from '@/stores/auth'
import router from '@/router'

const request = axios.create({
  baseURL: '/api',
  timeout: 30000,
})

// 请求拦截器
request.interceptors.request.use((config) => {
  const authStore = useAuthStore()
  if (authStore.token) {
    config.headers.Authorization = `Bearer ${authStore.token}`
    config.headers['X-User-Id'] = authStore.userInfo?.id || ''
  }
  return config
})

// 响应拦截器
request.interceptors.response.use(
  (response) => {
    const res = response.data
    if (res.code === 200) return res.data
    if (res.code === 401) {
      const authStore = useAuthStore()
      authStore.logout()
      router.push('/login')
      showToast('登录已过期')
      return Promise.reject(new Error(res.msg))
    }
    showToast(res.msg || '请求失败')
    return Promise.reject(new Error(res.msg))
  },
  (error) => {
    if (error.response?.status === 401) {
      const authStore = useAuthStore()
      authStore.logout()
      router.push('/login')
    }
    showToast(error.message || '网络错误')
    return Promise.reject(error)
  }
)

export default request
