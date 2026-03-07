<template>
  <div class="login-page">
    <div class="logo">
      <h1>SpeakMaster</h1>
      <p>AI英语口语练习</p>
    </div>
    <van-form @submit="handleLogin" class="login-form">
      <van-cell-group inset>
        <van-field v-model="form.username" label="用户名" placeholder="请输入用户名" :rules="[{ required: true }]" />
        <van-field v-model="form.password" type="password" label="密码" placeholder="请输入密码" :rules="[{ required: true }]" />
      </van-cell-group>
      <div style="margin: 24px 16px">
        <van-button round block type="primary" native-type="submit" :loading="loading">登录</van-button>
        <van-button round block plain style="margin-top: 12px" @click="$router.push('/register')">注册账号</van-button>
      </div>
    </van-form>
  </div>
</template>

<script setup>
import { ref, reactive } from 'vue'
import { useRouter } from 'vue-router'
import { showToast } from 'vant'
import { useAuthStore } from '@/stores/auth'
import { login } from '@/api/auth'
import { getProfile } from '@/api/user'

const router = useRouter()
const authStore = useAuthStore()
const loading = ref(false)
const form = reactive({ username: '', password: '' })

const handleLogin = async () => {
  loading.value = true
  try {
    const data = await login(form)
    authStore.setToken(data.token)
    const profile = await getProfile()
    authStore.setUserInfo(profile)
    showToast('登录成功')
    router.push('/home')
  } catch (e) {} finally { loading.value = false }
}
</script>

<style scoped>
.login-page { min-height: 100vh; display: flex; flex-direction: column; justify-content: center; background: linear-gradient(135deg, #667eea 0%, #764ba2 100%); }
.logo { text-align: center; color: #fff; margin-bottom: 40px; }
.logo h1 { font-size: 32px; margin-bottom: 8px; }
.logo p { font-size: 14px; opacity: 0.8; }
.login-form { margin: 0 16px; background: #fff; border-radius: 16px; padding: 24px 0; }
</style>
