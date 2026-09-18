import { createRouter, createWebHistory } from 'vue-router'
import { useUserStore } from '../stores/user'

const routes = [
  { path: '/login', name: 'login', component: () => import('../views/common/Login.vue') },
  // —— 患者端（Vant，手机）——
  { path: '/', name: 'chat', component: () => import('../views/patient/Chat.vue') },
  { path: '/register', name: 'sim-register', component: () => import('../views/patient/SimRegister.vue') },
  // —— 管理端（Element Plus，PC，校验 ROLE_ADMIN）——
  {
    path: '/admin',
    component: () => import('../views/admin/AdminLayout.vue'),
    meta: { requiresAdmin: true },
    children: [
      { path: '', redirect: '/admin/dashboard' },
      { path: 'dashboard', name: 'dashboard', component: () => import('../views/admin/Dashboard.vue') },
      { path: 'kb', name: 'kb', component: () => import('../views/admin/KbManage.vue') },
      { path: 'review', name: 'review', component: () => import('../views/admin/Review.vue') },
      { path: 'llm', name: 'llm-config', component: () => import('../views/admin/LlmConfig.vue') },
      { path: 'users', name: 'users', component: () => import('../views/admin/UserManage.vue') }
    ]
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

// 路由守卫：未登录跳登录；admin 路由校验 ROLE_ADMIN
router.beforeEach((to) => {
  const user = useUserStore()
  if (to.path !== '/login' && !user.isLogin) {
    return { path: '/login', query: { redirect: to.fullPath } }
  }
  if (to.meta.requiresAdmin && !user.isAdmin) {
    return { path: '/' }
  }
})

export default router
