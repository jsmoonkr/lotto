<script setup>
import { onMounted, ref } from 'vue'
import { api, errorMessage, won } from '@/api'
import { isMenuAdmin } from '@/auth'
import DrawBalls from '@/components/DrawBalls.vue'

const page = ref(null)
const pageNo = ref(0)
const search = ref('')
const found = ref(null)
const error = ref('')
const syncing = ref(false)
const syncMessage = ref('')

async function load(n) {
  error.value = ''
  try {
    page.value = await api.draws(n)
    pageNo.value = n
  } catch (e) {
    error.value = errorMessage(e)
  }
}

async function find() {
  const no = Number(search.value)
  if (!Number.isInteger(no) || no < 1) return
  error.value = ''
  try {
    found.value = await api.draw(no)
  } catch (e) {
    found.value = null
    error.value = errorMessage(e)
  }
}

async function sync() {
  syncing.value = true
  syncMessage.value = ''
  try {
    const { added } = await api.sync()
    syncMessage.value = added ? `${added}개 회차를 새로 가져왔어요.` : '이미 최신 상태예요.'
    await load(0)
  } catch (e) {
    syncMessage.value = errorMessage(e)
  } finally {
    syncing.value = false
  }
}

onMounted(() => load(0))
</script>

<template>
  <section class="card">
    <div class="toolbar">
      <h2>회차별 당첨번호</h2>
      <form class="search" @submit.prevent="find">
        <input v-model="search" type="number" min="1" placeholder="회차 번호" aria-label="회차 번호" />
        <button class="btn">찾기</button>
      </form>
      <button v-if="isMenuAdmin('DRAWS')" class="btn" :disabled="syncing" @click="sync">{{ syncing ? '가져오는 중…' : '최신 회차 가져오기' }}</button>
    </div>
    <p v-if="syncMessage" class="muted">{{ syncMessage }}</p>
    <p v-if="error" class="error">{{ error }}</p>

    <div v-if="found" class="found">
      <div class="found-head">
        <strong>{{ found.drawNo }}회</strong>
        <span class="muted">{{ found.drawDate }}</span>
        <button class="btn close" @click="found = null">닫기</button>
      </div>
      <DrawBalls :numbers="found.numbers" :bonus="found.bonus" />
      <table class="prizes">
        <thead>
          <tr><th>등수</th><th class="num">당첨자 수</th><th class="num">1인당 당첨금</th></tr>
        </thead>
        <tbody>
          <tr v-for="p in found.prizes" :key="p.rank">
            <td>{{ p.rank }}등</td>
            <td class="num">{{ p.winners.toLocaleString('ko-KR') }}명</td>
            <td class="num">{{ won(p.amount) }}</td>
          </tr>
        </tbody>
      </table>
      <p class="muted small">총 판매액 {{ won(found.totalSales) }}</p>
    </div>

    <div v-if="page" class="table-wrap">
      <table>
        <thead>
          <tr>
            <th>회차</th>
            <th class="hide-sm">추첨일</th>
            <th>당첨번호</th>
            <th class="num">1등 당첨자</th>
            <th class="num hide-sm">1등 1인당</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="d in page.content" :key="d.drawNo">
            <td><strong>{{ d.drawNo }}</strong></td>
            <td class="muted nowrap hide-sm">{{ d.drawDate }}</td>
            <td><DrawBalls :numbers="d.numbers" :bonus="d.bonus" size="sm" /></td>
            <td class="num">{{ d.prizes[0].winners }}명</td>
            <td class="num hide-sm">{{ d.prizes[0].winners ? won(d.prizes[0].amount) : '이월' }}</td>
          </tr>
        </tbody>
      </table>
      <div class="pager">
        <button class="btn" :disabled="pageNo === 0" @click="load(pageNo - 1)">최신</button>
        <span class="muted">{{ pageNo + 1 }} / {{ page.totalPages }}</span>
        <button class="btn" :disabled="pageNo + 1 >= page.totalPages" @click="load(pageNo + 1)">이전</button>
      </div>
    </div>
  </section>
</template>

<style scoped>
.toolbar {
  display: flex;
  align-items: center;
  gap: 12px;
  flex-wrap: wrap;
  margin-bottom: 12px;
}
.toolbar h2 {
  margin-right: auto;
}
.search {
  display: flex;
  gap: 6px;
}
.search input {
  width: 120px;
}
.found {
  border: 1px solid var(--accent);
  background: var(--accent-soft);
  border-radius: 10px;
  padding: 16px;
  margin-bottom: 16px;
}
.found-head {
  display: flex;
  align-items: baseline;
  gap: 10px;
  margin-bottom: 10px;
}
.close {
  margin-left: auto;
  padding: 4px 10px;
}
.prizes {
  margin-top: 12px;
}
.prizes th,
.prizes td {
  padding: 6px 8px;
}
.small {
  font-size: 13px;
  margin: 8px 0 0;
}
.table-wrap {
  overflow-x: auto;
}
.nowrap {
  white-space: nowrap;
}
</style>
