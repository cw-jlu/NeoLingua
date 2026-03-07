/**
 * 用户端路由配置
 */
import { createRouter, createWebHistory } from 'vue-router'
import { useAuthStore } from '@/stores/auth'

const routes = [
  { path: '/login', name: 'Login', component: () => import('@/views/Login.vue'), meta: { title: '登录' } },
  { path: '/register', name: 'Register', component: () => import('@/views/Register.vue'), meta: { title: '注册' } },
  {
    path: '/',
    component: () => import('@/layout/TabLayout.vue'),
    redirect: '/home',
    children: [
      { path: 'home', name: 'Home', component: () => import('@/views/Home.vue'), meta: { title: '首页', tabbar: true } },
      { path: 'chat', name: 'Chat', component: () => import('@/views/Chat.vue'), meta: { title: '练习', tabbar: true } },
      { path: 'meeting', name: 'Meeting', component: () => import('@/views/Meeting.vue'), meta: { title: 'Meeting', tabbar: true } },
      { path: 'analysis', name: 'Analysis', component: () => import('@/views/Analysis.vue'), meta: { title: '分析', tabbar: true } },
      { path: 'community', name: 'Community', component: () => import('@/views/Community.vue'), meta: { title: '社区', tabbar: true } },
      { path: 'profile', name: 'Profile', component: () => import('@/views/Profile.vue'), meta: { title: '我的', tabbar: true } },
    ],
  },
  // 二级页面（无底部导航）
  { path: '/chat/new', name: 'NewChat', component: () => import('@/views/NewChat.vue'), meta: { title: '新建对话' } },
  { path: '/chat/:sessionId', name: 'ChatRoom', component: () => import('@/views/ChatRoom.vue'), meta: { title: '对话' } },
  { path: '/meeting/:id', name: 'MeetingRoom', component: () => import('@/views/MeetingRoom.vue'), meta: { title: 'Meeting' } },
  { path: '/post/:id', name: 'PostDetail', component: () => import('@/views/PostDetail.vue'), meta: { title: '帖子详情' } },
  { path: '/settings', name: 'Settings', component: () => import('@/views/Settings.vue'), meta: { title: '设置' } },
  { path: '/friends', name: 'Friends', component: () => import('@/views/Friends.vue'), meta: { title: '好友' } },
  { path: '/badges', name: 'Badges', component: () => import('@/views/Badges.vue'), meta: { title: '成就徽章' } },
  { path: '/roles', name: 'RoleLibrary', component: () => import('@/views/RoleLibrary.vue'), meta: { title: '我的角色库' } },
]

const router = createRouter({ history: createWebHistory(), routes })

router.beforeEach((to, from, next) => {
  document.title = `${to.meta.title || 'SpeakMaster'}`
  const authStore = useAuthStore()
  if (!['/login', '/register'].includes(to.path) && !authStore.token) {
    next('/login')
  } else {
    next()
  }
})

export default router
