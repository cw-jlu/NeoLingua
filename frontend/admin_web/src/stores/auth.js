/**
 * 认证状态管理
 */
import { defineStore } from 'pinia'
import { ref } from 'vue'

export const useAuthStore = defineStore('auth', () => {
  const token = ref(localStorage.getItem('admin_token') || '')
  const userInfo = ref(JSON.parse(localStorage.getItem('admin_user') || 'null'))

  function setToken(t) {
    token.value = t
    localStorage.setItem('admin_token', t)
  }

  function setUserInfo(info) {
    userInfo.value = info
    localStorage.setItem('admin_user', JSON.stringify(info))
  }

  function logout() {
    token.value = ''
    userInfo.value = null
    localStorage.removeItem('admin_token')
    localStorage.removeItem('admin_user')
  }

  return { token, userInfo, setToken, setUserInfo, logout }
})
