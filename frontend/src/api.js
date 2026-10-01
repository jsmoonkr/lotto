import axios from 'axios'

const http = axios.create({ baseURL: '/api' })

export const STRATEGIES = [
  { value: 'FILTERED', label: '무작위 + 조건', desc: '홀수 2~4개, 합계 100~175, 연속번호 최대 2개' },
  { value: 'FREQUENCY', label: '자주 나온 번호', desc: '역대 출현 횟수가 많을수록 잘 뽑혀요' },
  { value: 'OVERDUE', label: '오래 안 나온 번호', desc: '마지막 출현 뒤 오래될수록 잘 뽑혀요' },
  { value: 'RANDOM', label: '완전 무작위', desc: '아무 조건 없이 뽑아요' },
]

export const strategyLabel = (value) => STRATEGIES.find((s) => s.value === value)?.label ?? value

export function errorMessage(e) {
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
}

export const won = (n) => `${n.toLocaleString('ko-KR')}원`
