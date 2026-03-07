<template>
  <div class="page">
    <van-nav-bar title="我的" />

    <!-- 用户信息卡片 -->
    <div class="card" style="display: flex; align-items: center; gap: 16px">
      <van-image round width="64" height="64" src="" />
      <div style="flex: 1">
        <h3>{{ authStore.userInfo?.username || '用户' }}</h3>
        <p style="font-size: 13px; color: #969799">{{ authStore.userInfo?.email || '' }}</p>
        <van-tag type="primary" style="margin-top: 4px">积分: {{ authStore.userInfo?.points || 0 }}</van-tag>
      </div>
    </div>

    <!-- 功能列表 -->
    <van-cell-group inset style="margin-top: 12px">
      <van-cell title="我的角色库" icon="user-o" is-link to="/roles" />
      <van-cell title="成就徽章" icon="medal-o" is-link to="/badges" />
      <van-cell title="好友列表" icon="friends-o" is-link to="/friends" />
      <van-cell title="学习分析" icon="bar-chart-o" is-link to="/analysis" />
      <van-cell title="通知中心" icon="bell" is-link :value="unreadCount > 0 ? `${unreadCount}条未读` : ''" />
    </van-cell-group>

    <van-cell-group inset style="margin-top: 12px">
      <van-cell title="设置" icon="setting-o" is-link to="/settings" />
      <van-cell title="退出登录" icon="revoke" @click="handleLogout" />
    </van-cell-group>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { showDialog } from 'vant'
import { useAuthStore } from '@/stores/auth'
import { getUnreadCount } from '@/api/notification'

const router = useRouter()
const authStore = useAuthStore()
const unreadCount = ref(0)

const handleLogout = () => {
  showDialog({ title: '确认退出', message: '确定要退出登录吗？', showCancelButton: true }).then(() => {
    authStore.logout()
    router.push('/login')
  }).catch(() => {})
}

onMounted(async () => {
  try { unreadCount.value = await getUnreadCount() || 0 } catch (e) {}
})
</script>
