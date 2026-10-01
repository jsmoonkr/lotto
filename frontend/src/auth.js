import { reactive } from 'vue'
import axios from 'axios'

/**
 * 로그인 상태.
 * 액세스 토큰은 메모리에만 둔다(새로고침하면 사라짐). 리프레시 토큰은 서버가 HttpOnly 쿠키로 관리하므로
 * 새로고침 뒤에는 restore()가 /api/auth/refresh로 액세스 토큰을 다시 받아 온다.
 */
export const MENUS = [
  { key: 'RECOMMEND', label: '추천', path: '/recommend' },
  { key: 'HISTORY', label: '추천 기록', path: '/history' },
  { key: 'DRAWS', label: '당첨번호', path: '/draws' },
  { key: 'STATS', label: '통계', path: '/stats' },
]

export const LEVELS = [
  { value: 'NONE', label: '없음' },
  { value: 'USE', label: '사용' },
  { value: 'ADMIN', label: '관리' },
]

export const auth = reactive({
  user: null,
  accessToken: null,
})

// 인터셉터를 거치지 않도록 인증 요청은 별도 인스턴스로 보낸다.
const authHttp = axios.create({ baseURL: '/api/auth' })

function apply(data) {
  auth.accessToken = data.accessToken
  auth.user = data.user
}

export function clearAuth() {
  auth.accessToken = null
  auth.user = null
}

export async function login(loginId, password) {
  const { data } = await authHttp.post('/login', { loginId, password })
  // MFA를 붙이면 data.status === 'MFA_REQUIRED'일 때 OTP 입력 화면으로 넘긴다.
  apply(data)
  return data
}

export async function signup(form) {
  const { data } = await authHttp.post('/signup', form)
  return data
}

export async function logout() {
  try {
    await authHttp.post('/logout')
  } finally {
    clearAuth()
  }
}

let refreshing = null

/** 동시에 여러 요청이 401을 받아도 갱신은 한 번만 한다. 실패하면 false. */
export function refresh() {
  if (!refreshing) {
    refreshing = authHttp
      .post('/refresh')
      .then(({ data }) => {
        apply(data)
        return true
      })
      .catch(() => {
        clearAuth()
        return false
      })
      .finally(() => {
        refreshing = null
      })
  }
  return refreshing
}

/** 앱 시작 시 한 번: 쿠키가 살아 있으면 로그인 상태를 되살린다. */
export const ready = refresh()

export function levelOf(menu) {
  return auth.user?.permissions?.[menu] ?? 'NONE'
}

export function canUse(menu) {
  const level = levelOf(menu)
  return level === 'USE' || level === 'ADMIN'
}

export function isMenuAdmin(menu) {
  return levelOf(menu) === 'ADMIN'
}

export function isSystemAdmin() {
  return !!auth.user?.systemAdmin
}

export function isAnyAdmin() {
  return isSystemAdmin() || MENUS.some((m) => isMenuAdmin(m.key))
}

/** 로그인 직후나 '/'로 왔을 때 보낼 첫 화면. */
export function homePath() {
  return MENUS.find((m) => canUse(m.key))?.path ?? '/no-access'
}
