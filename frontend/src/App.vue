<script setup>
import { RouterLink, RouterView } from 'vue-router'
</script>

<template>
  <header>
    <div class="wrap bar">
      <RouterLink to="/" class="logo">로또 추천</RouterLink>
      <nav>
        <RouterLink to="/">추천</RouterLink>
        <RouterLink to="/draws">당첨번호</RouterLink>
        <RouterLink to="/stats">통계</RouterLink>
        <RouterLink to="/history">추천 기록</RouterLink>
      </nav>
    </div>
  </header>

  <main class="wrap">
    <RouterView />
  </main>

  <footer class="wrap muted">
    로또 추첨은 매 회차 독립적인 무작위 추첨이에요. 과거 통계는 당첨 확률을 높여 주지 않으니 재미로만 봐 주세요.
  </footer>
</template>

<style scoped>
.wrap {
  max-width: 960px;
  margin: 0 auto;
  padding: 0 16px;
}
header {
  background: var(--surface);
  border-bottom: 1px solid var(--border);
}
.bar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  height: 56px;
}
.logo {
  font-weight: 800;
  font-size: 18px;
  color: var(--text);
  text-decoration: none;
  white-space: nowrap;
}
nav {
  display: flex;
  gap: 4px;
  overflow-x: auto;
}
nav a {
  padding: 6px 10px;
  border-radius: 8px;
  color: var(--muted);
  text-decoration: none;
  white-space: nowrap;
  font-size: 15px;
}
nav a.router-link-exact-active {
  color: var(--accent);
  background: var(--accent-soft);
  font-weight: 600;
}
main.wrap {
  padding-top: 24px;
  padding-bottom: 24px;
}
footer.wrap {
  font-size: 13px;
  padding-top: 8px;
  padding-bottom: 32px;
}
</style>
