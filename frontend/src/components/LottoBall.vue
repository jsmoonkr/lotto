<script setup>
import { computed } from 'vue'

const props = defineProps({
  number: { type: Number, required: true },
  size: { type: String, default: 'md' }, // sm | md
  dim: { type: Boolean, default: false },
})

// 동행복권 공 색상: 1~10 노랑, 11~20 파랑, 21~30 빨강, 31~40 회색, 41~45 초록
const color = computed(() => {
  const n = props.number
  if (n <= 10) return 'yellow'
  if (n <= 20) return 'blue'
  if (n <= 30) return 'red'
  if (n <= 40) return 'gray'
  return 'green'
})
</script>

<template>
  <span class="ball" :class="[color, size, { dim }]">{{ number }}</span>
</template>

<style scoped>
.ball {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  border-radius: 50%;
  font-weight: 700;
  color: #fff;
  text-shadow: 0 1px 1px rgb(0 0 0 / 0.25);
  flex-shrink: 0;
  font-variant-numeric: tabular-nums;
}
.md {
  width: 40px;
  height: 40px;
  font-size: 16px;
}
.sm {
  width: 30px;
  height: 30px;
  font-size: 13px;
}
.yellow { background: #f2b720; }
.blue { background: #3d8be0; }
.red { background: #e5534b; }
.gray { background: #8a8f98; }
.green { background: #3fae5a; }
.dim {
  opacity: 0.28;
}
</style>
