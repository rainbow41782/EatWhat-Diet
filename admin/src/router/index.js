import { createRouter, createWebHistory } from 'vue-router'
import { useAuthStore } from '@/stores/auth'

const routes = [
  {
    path: '/login',
    component: () => import('@/views/Login.vue'),
    meta: { public: true }
  },
  {
    path: '/',
    component: () => import('@/views/Layout.vue'),
    redirect: '/dashboard',
    children: [
      { path: 'dashboard', component: () => import('@/views/Dashboard.vue'), meta: { title: '概览仪表盘' } },
      { path: 'users', component: () => import('@/views/Users.vue'), meta: { title: '用户管理' } },
      { path: 'foods', component: () => import('@/views/Foods.vue'), meta: { title: '食品管理' } },
      { path: 'restaurants', component: () => import('@/views/Restaurants.vue'), meta: { title: '餐厅管理' } },
      { path: 'meituan-crawl', component: () => import('@/views/MeituanCrawler.vue'), meta: { title: '美团菜单爬取' } },
      { path: 'feedback', component: () => import('@/views/Feedback.vue'), meta: { title: '用户反馈' } },
    ]
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

router.beforeEach((to) => {
  const auth = useAuthStore()
  if (!to.meta.public && !auth.token) {
    return '/login'
  }
})

export default router
