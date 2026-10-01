<script setup>
import { onMounted, ref } from 'vue'
import { api, errorMessage, formatDateTime } from '@/api'
import { auth } from '@/auth'
import AdminTabs from '@/components/AdminTabs.vue'

const page = ref(null)
const pageNo = ref(0)
const q = ref('')
const error = ref('')
const message = ref('')
const busy = ref(null)

async function load(n = 0) {
  error.value = ''
  try {
    page.value = await api.adminUsers(q.value.trim(), n)
    pageNo.value = n
  } catch (e) {
    error.value = errorMessage(e)
  }
}
onMounted(() => load())

async function run(u, action, confirmText, done) {
  if (confirmText && !window.confirm(confirmText)) return
  busy.value = u.id
  error.value = ''
  message.value = ''
  try {
    Object.assign(u, await action())
    message.value = done
  } catch (e) {
    error.value = errorMessage(e)
  } finally {
    busy.value = null
  }
}

const toggleStatus = (u) =>
  u.status === 'ACTIVE'
    ? run(u, () => api.setStatus(u.id, 'DISABLED'),
        `${u.name}(${u.loginId}) 계정을 정지할까요? 바로 로그아웃되고 다시 로그인할 수 없어요.`,
        `${u.loginId} 계정을 정지했어요.`)
    : run(u, () => api.setStatus(u.id, 'ACTIVE'), null, `${u.loginId} 계정을 다시 활성화했어요.`)

const unlock = (u) => run(u, () => api.unlock(u.id), null, `${u.loginId} 계정의 잠금을 풀었어요.`)

const toggleSystemAdmin = (u) =>
  run(u, () => api.setSystemAdmin(u.id, !u.systemAdmin),
    u.systemAdmin
      ? `${u.loginId}의 시스템 관리자 권한을 해제할까요?`
      : `${u.loginId}를 시스템 관리자로 지정할까요? 모든 메뉴와 사용자를 관리할 수 있게 돼요.`,
    `${u.loginId}의 시스템 관리자 권한을 ${u.systemAdmin ? '해제' : '지정'}했어요.`)
</script>

<template>
  <section class="card">
    <AdminTabs />
    <div class="toolbar">
      <div>
        <h2>사용자 관리</h2>
        <p class="muted sub">개인정보는 DB에 암호화되어 있고, 시스템 관리자에게만 복호화해서 보여요.</p>
      </div>
      <form class="search" @submit.prevent="load(0)">
        <input v-model="q" placeholder="아이디 또는 이메일" aria-label="사용자 검색" />
        <button class="btn">검색</button>
      </form>
    </div>
    <p v-if="message" class="notice">{{ message }}</p>
    <p v-if="error" class="error" role="alert">{{ error }}</p>

    <div v-if="page" class="table-wrap">
      <table>
        <thead>
          <tr>
            <th>사용자</th>
            <th>연락처</th>
            <th>상태</th>
            <th class="hide-sm">가입 / 최근 로그인</th>
            <th>관리</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="u in page.content" :key="u.id">
            <td class="nowrap">
              <strong>{{ u.name }}</strong>
              <div class="muted small">{{ u.loginId }}</div>
            </td>
            <td class="small">
              <div>{{ u.email ?? '-' }}</div>
              <div class="muted">{{ u.phone ?? '' }}</div>
            </td>
            <td>
              <div class="badges">
              <span v-if="u.systemAdmin" class="badge accent">시스템 관리자</span>
              <span v-if="u.status === 'DISABLED'" class="badge danger">정지</span>
              <span v-else-if="u.locked" class="badge danger">잠김</span>
              <span v-else class="badge">정상</span>
              </div>
            </td>
            <td class="muted small nowrap hide-sm">
              <div>{{ formatDateTime(u.createdAt) }}</div>
              <div>{{ formatDateTime(u.lastLoginAt) }}</div>
            </td>
            <td>
              <div v-if="u.id !== auth.user.id" class="actions">
                <button v-if="u.locked" class="btn sm" :disabled="busy === u.id" @click="unlock(u)">잠금 해제</button>
                <button class="btn sm" :class="{ warn: u.status === 'ACTIVE' }" :disabled="busy === u.id" @click="toggleStatus(u)">
                  {{ u.status === 'ACTIVE' ? '정지' : '정지 해제' }}
                </button>
                <button class="btn sm" :disabled="busy === u.id" @click="toggleSystemAdmin(u)">
                  {{ u.systemAdmin ? '관리자 해제' : '관리자 지정' }}
                </button>
              </div>
              <span v-else class="muted small">내 계정</span>
            </td>
          </tr>
        </tbody>
      </table>
      <p v-if="!page.content.length" class="muted">검색 결과가 없어요.</p>
      <div v-if="page.totalPages > 1" class="pager">
        <button class="btn" :disabled="pageNo === 0" @click="load(pageNo - 1)">이전</button>
        <span class="muted">{{ pageNo + 1 }} / {{ page.totalPages }}</span>
        <button class="btn" :disabled="pageNo + 1 >= page.totalPages" @click="load(pageNo + 1)">다음</button>
      </div>
    </div>
  </section>
</template>

<style scoped>
.toolbar {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 12px;
  flex-wrap: wrap;
}
.sub {
  margin: 4px 0 12px;
  font-size: 14px;
}
.search {
  display: flex;
  gap: 6px;
}
.notice {
  margin-bottom: 12px;
}
.table-wrap {
  overflow-x: auto;
}
.nowrap {
  white-space: nowrap;
}
.small {
  font-size: 13px;
}
.badges {
  display: flex;
  flex-wrap: wrap;
  gap: 4px;
}
.actions {
  display: flex;
  gap: 6px;
  flex-wrap: wrap;
}
.btn.sm {
  padding: 4px 10px;
  font-size: 13px;
  white-space: nowrap;
}
.btn.warn {
  color: var(--danger);
}
</style>
