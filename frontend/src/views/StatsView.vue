<script setup>
import { computed, onMounted, ref } from 'vue'
import { Bar } from 'vue-chartjs'
import { BarElement, CategoryScale, Chart as ChartJS, LinearScale, Tooltip } from 'chart.js'
import { api, errorMessage } from '@/api'
import LottoBall from '@/components/LottoBall.vue'

ChartJS.register(BarElement, CategoryScale, LinearScale, Tooltip)

const RANGES = [
  { value: null, label: '전체' },
  { value: 100, label: '최근 100회' },
  { value: 50, label: '최근 50회' },
  { value: 20, label: '최근 20회' },
]

const recent = ref(null)
const stats = ref(null)
const error = ref('')

async function load() {
  error.value = ''
  try {
    stats.value = await api.frequency(recent.value ?? undefined)
  } catch (e) {
    error.value = errorMessage(e)
  }
}
onMounted(load)

// 공 색상과 같은 번호대별 색
const BAND_COLORS = ['#f2b720', '#3d8be0', '#e5534b', '#8a8f98', '#3fae5a']
const bandColor = (n) => BAND_COLORS[Math.min(4, Math.floor((n - 1) / 10))]

const chartData = computed(() => ({
  labels: stats.value.numbers.map((s) => s.number),
  datasets: [
    {
      label: '출현 횟수',
      data: stats.value.numbers.map((s) => s.count),
      backgroundColor: stats.value.numbers.map((s) => bandColor(s.number)),
      borderRadius: 3,
    },
  ],
}))

const chartOptions = {
  responsive: true,
  maintainAspectRatio: false,
  plugins: {
    tooltip: { callbacks: { title: (items) => `${items[0].label}번`, label: (item) => ` ${item.raw}회` } },
  },
  scales: {
    x: { grid: { display: false }, ticks: { autoSkip: false, maxRotation: 0, font: { size: 10 } } },
    y: { beginAtZero: true, ticks: { precision: 0 } },
  },
}

const top = computed(() => [...stats.value.numbers].sort((a, b) => b.count - a.count).slice(0, 6))
const bottom = computed(() => [...stats.value.numbers].sort((a, b) => a.count - b.count).slice(0, 6))
const overdue = computed(() => [...stats.value.numbers].sort((a, b) => b.gap - a.gap).slice(0, 6))
</script>

<template>
  <section class="card">
    <div class="toolbar">
      <h2>번호별 출현 횟수</h2>
      <div class="ranges" role="group" aria-label="집계 범위">
        <button
          v-for="r in RANGES"
          :key="r.label"
          class="btn"
          :class="{ on: recent === r.value }"
          @click="recent = r.value; load()"
        >
          {{ r.label }}
        </button>
      </div>
    </div>
    <p v-if="error" class="error">{{ error }}</p>

    <template v-if="stats">
      <p class="muted sub">{{ stats.drawCount }}개 회차 기준 (최신 {{ stats.latestDrawNo }}회, 보너스 번호 제외)</p>
      <div class="chart"><Bar :data="chartData" :options="chartOptions" /></div>
    </template>
  </section>

  <div v-if="stats" class="grid">
    <section class="card">
      <h3>많이 나온 번호</h3>
      <ul>
        <li v-for="s in top" :key="s.number"><LottoBall :number="s.number" size="sm" /> {{ s.count }}회</li>
      </ul>
    </section>
    <section class="card">
      <h3>적게 나온 번호</h3>
      <ul>
        <li v-for="s in bottom" :key="s.number"><LottoBall :number="s.number" size="sm" /> {{ s.count }}회</li>
      </ul>
    </section>
    <section class="card">
      <h3>오래 안 나온 번호</h3>
      <ul>
        <li v-for="s in overdue" :key="s.number">
          <LottoBall :number="s.number" size="sm" /> {{ s.gap }}회째 미출현
        </li>
      </ul>
    </section>
  </div>
</template>

<style scoped>
.toolbar {
  display: flex;
  align-items: center;
  gap: 12px;
  flex-wrap: wrap;
}
.toolbar h2 {
  margin-right: auto;
}
.ranges {
  display: flex;
  gap: 6px;
  flex-wrap: wrap;
}
.ranges .on {
  border-color: var(--accent);
  background: var(--accent-soft);
  color: var(--accent);
  font-weight: 600;
}
.sub {
  margin: 8px 0 12px;
  font-size: 14px;
}
.chart {
  height: 320px;
}
.grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(220px, 1fr));
  gap: 16px;
  margin-top: 16px;
}
ul {
  list-style: none;
  padding: 0;
  margin: 12px 0 0;
  display: flex;
  flex-direction: column;
  gap: 8px;
}
li {
  display: flex;
  align-items: center;
  gap: 10px;
  font-variant-numeric: tabular-nums;
}
</style>
