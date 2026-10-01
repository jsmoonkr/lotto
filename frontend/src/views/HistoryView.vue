<script setup>
import { onMounted, ref } from 'vue'
import { api, errorMessage, strategyLabel } from '@/api'
import DrawBalls from '@/components/DrawBalls.vue'

const page = ref(null)
const pageNo = ref(0)
const error = ref('')

async function load(n) {
  error.value = ''
  try {
    page.value = await api.history(n)
    pageNo.value = n
  } catch (e) {
    error.value = errorMessage(e)
  }
}
onMounted(() => load(0))

const formatTime = (iso) => iso.slice(0, 16).replace('T', ' ')
</script>

<template>
  <section class="card">
    <h2>추천 기록</h2>
    <p class="muted sub">추첨이 끝난 회차는 맞힌 번호만 진하게 표시해요.</p>
    <p v-if="error" class="error">{{ error }}</p>

    <template v-if="page">
      <p v-if="!page.content.length" class="muted">아직 추천받은 번호가 없어요.</p>
      <div v-else class="table-wrap">
        <table>
          <thead>
            <tr>
              <th>대상 회차</th>
              <th>번호</th>
              <th class="hide-sm">방식</th>
              <th>결과</th>
              <th class="hide-sm">추천 시각</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="r in page.content" :key="r.id">
              <td><strong>{{ r.targetDrawNo }}회</strong></td>
              <td><DrawBalls :numbers="r.numbers" size="sm" :highlight="r.drawn ? r.matched : null" /></td>
              <td class="nowrap hide-sm">{{ strategyLabel(r.strategy) }}</td>
              <td class="nowrap">
                <span v-if="!r.drawn" class="muted">추첨 전</span>
                <span v-else-if="r.rank" class="win">{{ r.rank }}등</span>
                <span v-else class="muted">{{ r.matched.length }}개 일치</span>
              </td>
              <td class="muted nowrap hide-sm">{{ formatTime(r.createdAt) }}</td>
            </tr>
          </tbody>
        </table>
        <div class="pager">
          <button class="btn" :disabled="pageNo === 0" @click="load(pageNo - 1)">최신</button>
          <span class="muted">{{ pageNo + 1 }} / {{ page.totalPages }}</span>
          <button class="btn" :disabled="pageNo + 1 >= page.totalPages" @click="load(pageNo + 1)">이전</button>
        </div>
      </div>
    </template>
  </section>
</template>

<style scoped>
.sub {
  margin: 4px 0 12px;
}
.table-wrap {
  overflow-x: auto;
}
.nowrap {
  white-space: nowrap;
}
.win {
  color: var(--accent);
  font-weight: 700;
}
</style>
