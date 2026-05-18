import { createRouter, createWebHistory } from 'vue-router'

// S1 只包含登录和仪表盘；后续审核、用户管理页面会作为 MainLayout 的子路由加入。
const router = createRouter({
  history: createWebHistory(),
  routes: [
    {
      path: '/login',
      name: 'login',
      component: () => import('../views/LoginView.vue'),
    },
    {
      path: '/',
      component: () => import('../layouts/MainLayout.vue'),
      redirect: '/dashboard',
      children: [
        { path: 'dashboard', name: 'dashboard', component: () => import('../views/DashboardView.vue') },
        { path: 'posts', name: 'posts', component: () => import('../views/PostAuditView.vue') },
        { path: 'matches', name: 'matches', component: () => import('../views/MatchResultView.vue') },
        { path: 'claims', name: 'claims', component: () => import('../views/ClaimReviewView.vue') },
        { path: 'users', name: 'users', component: () => import('../views/UserManageView.vue') },
      ],
    },
  ],
})

// 导航守卫：未登录时强制跳转登录页，避免进入受保护页面。
router.beforeEach((to) => {
  const token = localStorage.getItem('adminToken')
  if (!token && to.path !== '/login') {
    return '/login'
  }
})

export default router
