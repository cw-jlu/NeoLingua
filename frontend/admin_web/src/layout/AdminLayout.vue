<template>
  <el-container style="height: 100%">
    <!-- 侧边栏 -->
    <el-aside :width="isCollapse ? '64px' : '220px'" style="transition: width 0.3s">
      <div class="logo">
        <img src="" alt="" style="width: 32px; height: 32px" />
        <span v-show="!isCollapse">SpeakMaster</span>
      </div>
      <el-menu
        :default-active="$route.path"
        :collapse="isCollapse"
        router
        background-color="#001529"
        text-color="#ffffffa6"
        active-text-color="#1890ff"
      >
        <el-menu-item index="/dashboard">
          <el-icon><Odometer /></el-icon>
          <span>仪表盘</span>
        </el-menu-item>

        <el-sub-menu index="user-group">
          <template #title>
            <el-icon><User /></el-icon>
            <span>用户管理</span>
          </template>
          <el-menu-item index="/users">用户列表</el-menu-item>
          <el-menu-item index="/badges">徽章管理</el-menu-item>
        </el-sub-menu>

        <el-sub-menu index="practice-group">
          <template #title>
            <el-icon><ChatDotRound /></el-icon>
            <span>练习管理</span>
          </template>
          <el-menu-item index="/themes">主题管理</el-menu-item>
          <el-menu-item index="/roles">角色管理</el-menu-item>
          <el-menu-item index="/sessions">会话管理</el-menu-item>
        </el-sub-menu>

        <el-menu-item index="/meetings">
          <el-icon><VideoCamera /></el-icon>
          <span>Meeting管理</span>
        </el-menu-item>

        <el-sub-menu index="community-group">
          <template #title>
            <el-icon><Document /></el-icon>
            <span>社区管理</span>
          </template>
          <el-menu-item index="/posts">帖子管理</el-menu-item>
          <el-menu-item index="/comments">评论管理</el-menu-item>
        </el-sub-menu>

        <el-menu-item index="/notifications">
          <el-icon><Bell /></el-icon>
          <span>通知管理</span>
        </el-menu-item>

        <el-sub-menu index="analysis-group">
          <template #title>
            <el-icon><DataAnalysis /></el-icon>
            <span>数据分析</span>
          </template>
          <el-menu-item index="/analysis/reports">分析报告</el-menu-item>
          <el-menu-item index="/analysis/ranking">用户排名</el-menu-item>
        </el-sub-menu>

        <el-sub-menu index="ai-group">
          <template #title>
            <el-icon><Cpu /></el-icon>
            <span>AI管理</span>
          </template>
          <el-menu-item index="/ai/models">模型管理</el-menu-item>
          <el-menu-item index="/ai/routing">路由规则</el-menu-item>
          <el-menu-item index="/ai/knowledge">知识库管理</el-menu-item>
        </el-sub-menu>

        <el-sub-menu index="system-group">
          <template #title>
            <el-icon><Setting /></el-icon>
            <span>系统管理</span>
          </template>
          <el-menu-item index="/system/config">系统配置</el-menu-item>
          <el-menu-item index="/system/logs">系统日志</el-menu-item>
          <el-menu-item index="/system/monitor">系统监控</el-menu-item>
        </el-sub-menu>
      </el-menu>
    </el-aside>

    <!-- 主内容区 -->
    <el-container>
      <el-header style="display: flex; align-items: center; justify-content: space-between; border-bottom: 1px solid #eee">
        <el-icon style="cursor: pointer; font-size: 20px" @click="isCollapse = !isCollapse">
          <Fold v-if="!isCollapse" />
          <Expand v-else />
        </el-icon>
        <div style="display: flex; align-items: center; gap: 16px">
          <span>{{ authStore.userInfo?.username || '管理员' }}</span>
          <el-button type="danger" text @click="handleLogout">退出</el-button>
        </div>
      </el-header>
      <el-main style="background: #f0f2f5; padding: 20px">
        <router-view />
      </el-main>
    </el-container>
  </el-container>
</template>

<script setup>
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/auth'

const isCollapse = ref(false)
const router = useRouter()
const authStore = useAuthStore()

const handleLogout = () => {
  authStore.logout()
  router.push('/login')
}
</script>

<style scoped>
.logo {
  height: 60px;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  color: #fff;
  font-size: 18px;
  font-weight: bold;
  background: #001529;
}
.el-aside {
  background: #001529;
  overflow-y: auto;
}
.el-header {
  background: #fff;
}
</style>
