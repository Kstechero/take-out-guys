import { createRouter, createWebHistory } from 'vue-router'
import AppLayout from '@/layout/AppLayout.vue'

const router = createRouter({
  history: createWebHistory(),
  routes: [
    { path: '/login', component: () => import('@/views/LoginView.vue'), meta: { public: true } },
    {
      path: '/',
      component: AppLayout,
      redirect: '/dashboard',
      children: [
        { path: 'dashboard', component: () => import('@/views/DashboardView.vue'), meta: { title: '经营总览' } },
        { path: 'reports', component: () => import('@/views/ReportsView.vue'), meta: { title: '数据统计' } },
        { path: 'agent', component: () => import('@/views/AgentView.vue'), meta: { title: 'AI Agent' } },
        { path: 'orders', component: () => import('@/views/BusinessView.vue'), meta: { title: '订单管理', kind: 'order' } },
        { path: 'dishes', component: () => import('@/views/BusinessView.vue'), meta: { title: '菜品管理', kind: 'dish' } },
        { path: 'categories', component: () => import('@/views/BusinessView.vue'), meta: { title: '分类管理', kind: 'category' } },
        { path: 'setmeals', component: () => import('@/views/BusinessView.vue'), meta: { title: '套餐管理', kind: 'setmeal' } },
        { path: 'coupons', component: () => import('@/views/CouponView.vue'), meta: { title: '优惠券管理' } },
        { path: 'reviews', component: () => import('@/views/ReviewsView.vue'), meta: { title: '评价管理' } },
        { path: 'service', component: () => import('@/views/ServiceView.vue'), meta: { title: '人工客服' } },
        { path: 'sensitive', component: () => import('@/views/BusinessView.vue'), meta: { title: '敏感词库', kind: 'sensitive' } },
        { path: 'employees', component: () => import('@/views/BusinessView.vue'), meta: { title: '员工管理', kind: 'employee' } }
      ]
    },
    { path: '/:pathMatch(.*)*', redirect: '/' }
  ]
})

router.beforeEach(to => {
  if (!to.meta.public && !localStorage.getItem('sky_admin_token')) return '/login'
})

export default router
