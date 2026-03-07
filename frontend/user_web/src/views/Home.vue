<template>
  <div class="page">
    <!-- 顶部欢迎 -->
    <div class="card" style="background: linear-gradient(135deg, #667eea, #764ba2); color: #fff">
      <h3>Hi, {{ authStore.userInfo?.username || '同学' }} 👋</h3>
      <p style="margin-top: 4px; font-size: 13px; opacity: 0.9">今天也要加油练习英语哦</p>
    </div>

    <!-- 签到 -->
    <div class="card" style="display: flex; justify-content: space-between; align-items: center">
      <div>
        <p style="font-size: 14px; color: #969799">每日签到</p>
        <p style="font-size: 18px; font-weight: bold; margin-top: 4px">积分: {{ points }}</p>
      </div>
      <van-button type="primary" size="small" round :disabled="signedIn" @click="handleSignIn">
        {{ signedIn ? '已签到 ✓' : '签到 +5' }}
      </van-button>
    </div>

    <!-- 快捷入口 -->
    <div class="card">
      <van-grid :column-num="4" :border="false">
        <van-grid-item icon="chat-o" text="开始练习" to="/chat" />
        <van-grid-item icon="video-o" text="Meeting" to="/meeting" />
        <van-grid-item icon="bar-chart-o" text="我的分析" to="/analysis" />
        <van-grid-item icon="friends-o" text="社区" to="/community" />
      </van-grid>
    </div>

    <!-- 每日挑战 -->
    <div class="card">
      <h4 style="margin-bottom: 12px">🎯 每日口语挑战</h4>
      <van-cell title="今日主题" :value="dailyTheme" is-link @click="startDailyChallenge" />
    </div>

    <!-- 通知 -->
    <div class="card" v-if="unreadCount > 0">
      <van-notice-bar left-icon="volume-o" :text="`你有 ${unreadCount} 条未读通知`" />
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { showToast } from 'vant'
import { useAuthStore } from '@/stores/auth'
import { getPoints, signIn, getSignInStatus } from '@/api/user'
import { getUnreadCount } from '@/api/notification'

const router = useRouter()
const authStore = useAuthStore()
const points = ref(0)
const signedIn = ref(false)
const unreadCount = ref(0)
const dailyTheme = ref('日常对话')

const handleSignIn = async () => {
  try {
    await signIn()
    signedIn.value = true
    points.value += 5
    showToast('签到成功 +5积分')
  } catch (e) {}
}

const startDailyChallenge = () => {
  router.push('/chat')
}

onMounted(async () => {
  try {
    const p = await getPoints()
    points.value = p || 0
  } catch (e) {}
  try {
    const status = await getSignInStatus()
    signedIn.value = !!status
  } catch (e) {}
  try {
    const count = await getUnreadCount()
    unreadCount.value = count || 0
  } catch (e) {}
})
</script>
