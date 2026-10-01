<script setup>
import { onMounted, ref } from 'vue'
import { api, errorMessage, STRATEGIES, strategyLabel } from '@/api'
import DrawBalls from '@/components/DrawBalls.vue'

const latest = ref(null)
const latestError = ref('')
const strategy = ref('FILTERED')
const count = ref(5)
const results = ref([])
const loading = ref(false)
const error = ref('')

onMounted(async () => {
  try {
    latest.value = await api.latestDraw()
  } catch (e) {
    latestError.value =
      e.response?.status === 404 ? '당첨번호를 수집하는 중이에요. 잠시 뒤 새로고침해 주세요.' : errorMessage(e)
  }
})

async function recommend() {
  loading.value = true
  error.value = ''
  try {
    results.value = await api.recommend(strategy.value, count.value)
  } catch (e) {
    error.value = errorMessage(e)
  } finally {
    loading.value = false
  }
}
</script>

<template>
  <section class="card latest">
    <template v-if="latest">
      <div class="head">
        <h2>{{ latest.drawNo }}회 당첨번호</h2>
        <span class="muted">{{ latest.drawDate }} 추첨</span>
      </div>
      <DrawBalls :numbers="latest.numbers" :bonus="latest.bonus" />
    </template>
    <p v-else-if="latestError" class="muted">{{ latestError }}</p>
    <p v-else class="muted">불러오는 중…</p>
  </section>

  <section class="card">
    <h2>번호 추천받기</h2>
    <p class="muted sub">
      {{ latest ? `${latest.drawNo + 1}회차용` : '다음 회차용' }} 번호를 뽑아요. 뽑은 번호는 추천 기록에 저장돼요.
    </p>

    <div class="strategies">
      <label v-for="s in STRATEGIES" :key="s.value" class="strategy" :class="{ on: strategy === s.value }">
        <input v-model="strategy" type="radio" name="strategy" :value="s.value" />
        <strong>{{ s.label }}</strong>
        <span class="muted">{{ s.desc }}</span>
      </label>
    </div>

    <div class="actions">
      <label>
        게임 수
        <select v-model.number="count">
          <option v-for="n in 5" :key="n" :value="n">{{ n }}게임</option>
        </select>
      </label>
      <button class="btn primary" :disabled="loading" @click="recommend">
        {{ loading ? '뽑는 중…' : '추천받기' }}
      </button>
    </div>
    <p v-if="error" class="error">{{ error }}</p>

    <ol v-if="results.length" class="results">
      <li v-for="(r, i) in results" :key="r.id">
        <span class="label">{{ String.fromCharCode(65 + i) }}</span>
        <DrawBalls :numbers="r.numbers" />
      </li>
    </ol>
    <p v-if="results.length" class="muted small">{{ strategyLabel(results[0].strategy) }} 방식으로 뽑았어요.</p>
  </section>
</template>

<style scoped>
section + section {
  margin-top: 16px;
}
.latest .head {
  display: flex;
  align-items: baseline;
  gap: 10px;
  flex-wrap: wrap;
  margin-bottom: 14px;
}
.sub {
  margin: 4px 0 16px;
}
.strategies {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(200px, 1fr));
  gap: 10px;
}
.strategy {
  display: flex;
  flex-direction: column;
  gap: 2px;
  padding: 12px 14px;
  border: 1px solid var(--border);
  border-radius: 10px;
  cursor: pointer;
  font-size: 14px;
}
.strategy input {
  position: absolute;
  opacity: 0;
  pointer-events: none;
}
.strategy strong {
  font-size: 15px;
}
.strategy.on {
  border-color: var(--accent);
  background: var(--accent-soft);
}
.strategy:has(input:focus-visible) {
  outline: 2px solid var(--accent);
  outline-offset: 2px;
}
.actions {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-top: 16px;
}
.actions label {
  display: flex;
  align-items: center;
  gap: 8px;
}
.results {
  list-style: none;
  padding: 0;
  margin: 20px 0 0;
  display: flex;
  flex-direction: column;
  gap: 10px;
}
.results li {
  display: flex;
  align-items: center;
  gap: 12px;
}
.label {
  width: 18px;
  font-weight: 700;
  color: var(--muted);
}
.small {
  font-size: 13px;
  margin-bottom: 0;
}
</style>
