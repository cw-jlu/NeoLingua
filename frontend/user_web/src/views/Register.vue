<template>
  <div class="login-page">
    <div class="logo">
      <h1>SpeakMaster</h1>
      <p>创建你的账号</p>
    </div>
    <van-form @submit="handleRegister" class="login-form">
      <van-cell-group inset>
        <van-field v-model="form.username" label="用户名" placeholder="请输入用户名" :rules="[{ required: true }]" />
        <van-field v-model="form.email" label="邮箱" placeholder="请输入邮箱" :rules="[{ required: true }]" />
        <van-field v-model="form.password" type="password" label="密码" placeholder="请输入密码" :rules="[{ required: true }]" />
        <van-field v-model="confirmPwd" type="password" label="确认密码" placeholder="再次输入密码" :rules="[{ required: true }]" />
      </van-cell-group>
      <div style="margin: 24px 16px">
        <van-button round block type="primary" native-type="submit" :loading="loading">注册</van-button>
        <van-button round block plain style="margin-top: 12px" @click="$router.push('/login')">已有账号？登录</van-button>
      </div>
    </van-form>
  </div>
</template>

<script setup>
import { ref, reactive } from 'vue'
import { useRouter } from 'vue-router'
import { showToast } from 'vant'
import { register } from '@/api/auth'

const router = useRouter()
const loading = ref(false)
const confirmPwd = ref('')
const form = reactive({ username: '', email: '', password: '' })

const handleRegister = async () => {
  if (form.password !== confirmPwd.value) { showToast('两次密码不一致'); return }
  loading.value = true
  try {
    await register(form)
    showToast('注册成功')
    router.push('/login')
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
