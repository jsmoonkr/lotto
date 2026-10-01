import { createRouter, createWebHistory } from 'vue-router'
import { auth, canUse, homePath, isAnyAdmin, isSystemAdmin, ready } from '@/auth'

/**
 * meta.public: 로그인 없이 볼 수 있는 화면
 * meta.menu: 이 메뉴의 USE 권한이 있어야 볼 수 있는 화면
 * meta.access: 메뉴 외의 접근 조건 (관리자 화면)
 */
const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes: [
    { path: '/login', component: () => import('@/views/LoginView.vue'), meta: { public: true } },
    { path: '/signup', component: () => import('@/views/SignupView.vue'), meta: { public: true } },
    // redirect는 가드보다 먼저 계산돼 로그인 복원 전 상태를 보게 되므로, 가드에서 첫 화면으로 보낸다.
    { path: '/', component: { render: () => null }, meta: { home: true } },
    { path: '/recommend', component: () => import('@/views/HomeView.vue'), meta: { menu: 'RECOMMEND' } },
    { path: '/history', component: () => import('@/views/HistoryView.vue'), meta: { menu: 'HISTORY' } },
    { path: '/draws', component: () => import('@/views/DrawsView.vue'), meta: { menu: 'DRAWS' } },
    { path: '/stats', component: () => import('@/views/StatsView.vue'), meta: { menu: 'STATS' } },
    { path: '/account', component: () => import('@/views/AccountView.vue') },
    { path: '/no-access', component: () => import('@/views/NoAccessView.vue') },
    {
      path: '/admin/users',
      component: () => import('@/views/admin/AdminUsersView.vue'),
      meta: { access: isSystemAdmin },
    },
    {
      path: '/admin/permissions',
      component: () => import('@/views/admin/AdminPermissionsView.vue'),
      meta: { access: isAnyAdmin },
    },
    { path: '/:pathMatch(.*)*', redirect: '/' },
  ],
})

router.beforeEach(async (to) => {
  await ready
  if (to.meta.public) {
    return auth.user ? homePath() : true
  }
  if (!auth.user) {
    return { path: '/login', query: to.meta.home ? {} : { redirect: to.fullPath } }
  }
  if (to.meta.home || (to.path === '/no-access' && homePath() !== '/no-access')) {
    return homePath()
  }
  if (to.meta.menu && !canUse(to.meta.menu)) {
    return homePath()
  }
  if (to.meta.access && !to.meta.access()) {
    return homePath()
  }
  return true
})

export default router
