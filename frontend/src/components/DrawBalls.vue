<script setup>
import LottoBall from './LottoBall.vue'

defineProps({
  numbers: { type: Array, required: true },
  bonus: { type: Number, default: null },
  size: { type: String, default: 'md' },
  // 이 번호들만 진하게, 나머지는 흐리게 (당첨 확인용)
  highlight: { type: Array, default: null },
  bonusHit: { type: Boolean, default: false },
})
</script>

<template>
  <div class="balls">
    <LottoBall
      v-for="n in numbers"
      :key="n"
      :number="n"
      :size="size"
      :dim="highlight !== null && !highlight.includes(n)"
    />
    <template v-if="bonus !== null">
      <span class="plus">+</span>
      <LottoBall :number="bonus" :size="size" :dim="highlight !== null && !bonusHit" />
    </template>
  </div>
</template>

<style scoped>
.balls {
  display: flex;
  align-items: center;
  gap: 6px;
}
.plus {
  color: var(--muted);
  font-weight: 700;
  margin: 0 2px;
}
</style>
