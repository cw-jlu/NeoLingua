/**
 * 路由配置
 */
import { createRouter, createWebHistory } from 'vue-router'
import { useAuthStore } from '@/stores/auth'

const routes = [
  {
    path: '/login',
    name: 'Login',
    component: () => import('@/views/Login.vue'),
    meta: { title: '登录' },
  },
  {
    path: '/',
    component: () => import('@/layout/AdminLayout.vue'),
    redirect: '/dashboard',
    children: [
      {
        path: 'dashboard',
        name: 'Dashboard',
        component: () => import('@/views/Dashboard.vue'),
        meta: { title: '仪表盘', icon: 'Odometer' },
      },
      // 用户管理
      {
        path: 'users',
        name: 'Users',
        component: () => import('@/views/user/UserList.vue'),
        meta: { title: '用户管理', icon: 'User' },
      },
      {
        path: 'badges',
        name: 'Badges',
        component: () => import('@/views/user/BadgeList.vue'),
        meta: { title: '徽章管理', icon: 'Medal' },
      },
      // 练习管理
      {
        path: 'themes',
        name: 'Themes',
        component: () => import('@/views/practice/ThemeList.vue'),
        meta: { title: '主题管理', icon: 'Collection' },
      },
      {
        path: 'roles',
        name: 'Roles',
        component: () => import('@/views/practice/RoleList.vue'),
        meta: { title: '角色管理', icon: 'Avatar' },
      },
      {
        path: 'sessions',
        name: 'Sessions',
        component: () => import('@/views/practice/SessionList.vue'),
        meta: { title: '会话管理', icon: 'ChatDotRound' },
      },
      // Meeting管理
      {
        path: 'meetings',
        name: 'Meetings',
        component: () => import('@/views/meeting/MeetingList.vue'),
        meta: { title: 'Meeting管理', icon: 'VideoCamera' },
      },
      // 社区管理
      {
        path: 'posts',
        name: 'Posts',
        component: () => import('@/views/community/PostList.vue'),
        meta: { title: '帖子管理', icon: 'Document' },
      },
      {
        path: 'comments',
        name: 'Comments',
        component: () => import('@/views/community/CommentList.vue'),
        meta: { title: '评论管理', icon: 'ChatLineSquare' },
      },
      // 通知管理
      {
        path: 'notifications',
        name: 'Notifications',
        component: () => import('@/views/notification/NotificationList.vue'),
        meta: { title: '通知管理', icon: 'Bell' },
      },
      // 分析管理
      {
        path: 'analysis/reports',
        name: 'AnalysisReports',
        component: () => import('@/views/analysis/ReportList.vue'),
        meta: { title: '分析报告', icon: 'DataAnalysis' },
      },
      {
        path: 'analysis/ranking',
        name: 'AnalysisRanking',
        component: () => import('@/views/analysis/RankingList.vue'),
        meta: { title: '用户排名', icon: 'Trophy' },
      },
      // AI管理
      {
        path: 'ai/models',
        name: 'AIModels',
        component: () => import('@/views/ai/ModelList.vue'),
        meta: { title: 'AI模型管理', icon: 'Cpu' },
      },
      {
        path: 'ai/routing',
        name: 'AIRouting',
        component: () => import('@/views/ai/RoutingList.vue'),
        meta: { title: '路由规则', icon: 'Connection' },
      },
      {
        path: 'ai/knowledge',
        name: 'KnowledgeBase',
        component: () => import('@/views/ai/KnowledgeBase.vue'),
        meta: { title: '知识库管理', icon: 'Files' },
      },
      // 系统管理
      {
        path: 'system/config',
        name: 'SystemConfig',
        component: () => import('@/views/system/ConfigList.vue'),
        meta: { title: '系统配置', icon: 'Setting' },
      },
      {
        path: 'system/logs',
        name: 'SystemLogs',
        component: () => import('@/views/system/LogList.vue'),
        meta: { title: '系统日志', icon: 'Tickets' },
      },
      {
        path: 'system/monitor',
        name: 'SystemMonitor',
        component: () => import('@/views/system/Monitor.vue'),
        meta: { title: '系统监控', icon: 'Monitor' },
      },
    ],
  },
]

const router = createRouter({
  history: createWebHistory(),
  routes,
})

// 路由守卫
router.beforeEach((to, from, next) => {
  document.title = `${to.meta.title || 'SpeakMaster'} - 管理后台`
  const authStore = useAuthStore()
  if (to.path !== '/login' && !authStore.token) {
    next('/login')
  } else {
    next()
  }
})

export default router
