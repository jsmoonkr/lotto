import axios from 'axios'
import router from '@/router'
import { auth, refresh } from '@/auth'

const http = axios.create({ baseURL: '/api' })

http.interceptors.request.use((config) => {
  if (auth.accessToken) {
    config.headers.Authorization = `Bearer ${auth.accessToken}`
  }
  return config
})

// 액세스 토큰이 만료되면 한 번 갱신해서 다시 보내고, 그래도 안 되면 로그인 화면으로 보낸다.
http.interceptors.response.use(
  (response) => response,
  async (error) => {
    const config = error.config
    if (error.response?.status === 401 && config && !config._retried) {
      config._retried = true
      if (await refresh()) {
        return http(config)
      }
      const current = router.currentRoute.value
      if (!current.meta.public) {
        router.replace({ path: '/login', query: { redirect: current.fullPath } })
      }
    }
    return Promise.reject(error)
  },
)

export const STRATEGIES = [
  { value: 'FILTERED', label: '무작위 + 조건', desc: '홀수 2~4개, 합계 100~175, 연속번호 최대 2개' },
  { value: 'FREQUENCY', label: '자주 나온 번호', desc: '역대 출현 횟수가 많을수록 잘 뽑혀요' },
  { value: 'OVERDUE', label: '오래 안 나온 번호', desc: '마지막 출현 뒤 오래될수록 잘 뽑혀요' },
  { value: 'RANDOM', label: '완전 무작위', desc: '아무 조건 없이 뽑아요' },
]

export const strategyLabel = (value) => STRATEGIES.find((s) => s.value === value)?.label ?? value

export function errorMessage(e) {
  if (e.response?.status === 403 && !e.response?.data?.detail) {
    return '이 기능을 쓸 권한이 없어요.'
  }
  return e.response?.data?.detail ?? '서버에 연결하지 못했어요. 백엔드가 실행 중인지 확인해 주세요.'
}

export const api = {
  latestDraw: () => http.get('/draws/latest').then((r) => r.data),
  draw: (drawNo) => http.get(`/draws/${drawNo}`).then((r) => r.data),
  draws: (page, size = 20) => http.get('/draws', { params: { page, size } }).then((r) => r.data),
  sync: () => http.post('/draws/sync').then((r) => r.data),
  frequency: (recent) => http.get('/stats/frequency', { params: { recent } }).then((r) => r.data),
  recommend: (strategy, count) => http.post('/recommendations', { strategy, count }).then((r) => r.data),
  history: (page, size = 20) => http.get('/recommendations', { params: { page, size } }).then((r) => r.data),

  changePassword: (currentPassword, newPassword) =>
    http.post('/auth/password', { currentPassword, newPassword }),

  adminUsers: (q, page, size = 20) =>
    http.get('/admin/users', { params: { q: q || undefined, page, size } }).then((r) => r.data),
  setPermission: (id, menu, level) =>
    http.put(`/admin/users/${id}/permissions`, { menu, level }).then((r) => r.data),
  setStatus: (id, status) => http.put(`/admin/users/${id}/status`, { status }).then((r) => r.data),
  unlock: (id) => http.post(`/admin/users/${id}/unlock`).then((r) => r.data),
  setSystemAdmin: (id, value) => http.put(`/admin/users/${id}/system-admin`, { value }).then((r) => r.data),
}

export const won = (n) => `${n.toLocaleString('ko-KR')}원`

export const formatDateTime = (iso) =>
  iso ? new Date(iso).toLocaleString('ko-KR', { dateStyle: 'short', timeStyle: 'short' }) : '-'
