<script setup>
import { onMounted, ref } from 'vue'
import { api, errorMessage } from '@/api'
import { auth, isMenuAdmin, isSystemAdmin, LEVELS, MENUS } from '@/auth'
import AdminTabs from '@/components/AdminTabs.vue'

const page = ref(null)
const pageNo = ref(0)
const q = ref('')
const error = ref('')
const message = ref('')
const saving = ref(null) // "userId:menu"

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

const levelOf = (u, menu) => u.permissions[menu] ?? 'NONE'

/** 이 칸을 내가 바꿀 수 있는지. 메뉴 관리자는 자기 메뉴의 없음/사용만 바꿀 수 있다. */
function editable(u, menu) {
  if (isSystemAdmin()) return !u.systemAdmin
  return isMenuAdmin(menu) && !u.systemAdmin && levelOf(u, menu) !== 'ADMIN'
}

function options(u, menu) {
  return isSystemAdmin() ? LEVELS : LEVELS.filter((l) => l.value !== 'ADMIN' || levelOf(u, menu) === 'ADMIN')
}

async function change(u, menu, level) {
  const key = `${u.id}:${menu}`
  saving.value = key
  error.value = ''
  message.value = ''
  try {
    const updated = await api.setPermission(u.id, menu, level)
    Object.assign(u, updated)
    message.value = `${u.name}(${u.loginId})님의 ${MENUS.find((m) => m.key === menu).label} 권한을 바꿨어요.`
  } catch (e) {
    error.value = errorMessage(e)
  } finally {
    saving.value = null
  }
}
</script>

<template>
  <section class="card">
    <AdminTabs />
    <div class="toolbar">
      <div>
        <h2>사용자 권한 관리</h2>
        <p class="muted sub">
          {{ isSystemAdmin() ? '모든 메뉴의 권한을 바꿀 수 있어요.' : '내가 관리자인 메뉴의 사용 권한만 주거나 뺄 수 있어요.' }}
          새로 가입한 사용자는 통계만 사용할 수 있어요.
        </p>
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
            <th v-for="m in MENUS" :key="m.key">{{ m.label }}</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="u in page.content" :key="u.id">
            <td class="nowrap">
              <strong>{{ u.name }}</strong>
              <span class="muted id">{{ u.loginId }}</span>
              <span v-if="u.id === auth.user.id" class="badge accent">나</span>
              <div class="muted small">{{ u.email ?? '' }}</div>
            </td>
            <td v-if="u.systemAdmin" :colspan="MENUS.length">
              <span class="badge accent">시스템 관리자 · 모든 메뉴 관리</span>
            </td>
            <template v-else>
              <td v-for="m in MENUS" :key="m.key">
                <select
                  v-if="editable(u, m.key)"
                  :value="levelOf(u, m.key)"
                  :disabled="saving === `${u.id}:${m.key}`"
                  :aria-label="`${u.loginId} ${m.label} 권한`"
                  :class="'lv-' + levelOf(u, m.key)"
                  @change="change(u, m.key, $event.target.value)"
                >
                  <option v-for="l in options(u, m.key)" :key="l.value" :value="l.value">{{ l.label }}</option>
                </select>
                <span v-else class="badge" :class="{ accent: levelOf(u, m.key) !== 'NONE' }">
                  {{ LEVELS.find((l) => l.value === levelOf(u, m.key)).label }}
                </span>
              </td>
            </template>
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
  font-size: 12px;
}
.id {
  margin: 0 6px;
}
select.lv-USE {
  border-color: var(--accent);
}
select.lv-ADMIN {
  border-color: var(--accent);
  background: var(--accent-soft);
  font-weight: 600;
}
</style>
