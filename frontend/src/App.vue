<script setup>
import { computed, ref } from 'vue'
import { RouterLink, RouterView, useRoute, useRouter } from 'vue-router'
import { auth, canUse, isAnyAdmin, logout, MENUS } from '@/auth'

const route = useRoute()
const router = useRouter()
const menuOpen = ref(false)

const menus = computed(() => MENUS.filter((m) => canUse(m.key)))
const showAdmin = computed(() => isAnyAdmin())

async function doLogout() {
  menuOpen.value = false
  await logout()
  router.replace('/login')
}
</script>

<template>
  <header v-if="!route.meta.public && auth.user">
    <div class="wrap bar">
      <RouterLink to="/" class="logo">로또 추천</RouterLink>
      <nav>
        <RouterLink v-for="m in menus" :key="m.key" :to="m.path">{{ m.label }}</RouterLink>
        <RouterLink v-if="showAdmin" to="/admin/permissions" :class="{ 'router-link-active': route.path.startsWith('/admin') }">
          관리
        </RouterLink>
      </nav>
      <div class="user">
        <button class="user-btn" :aria-expanded="menuOpen" @click="menuOpen = !menuOpen">
          {{ auth.user.name }}님 ▾
        </button>
        <div v-if="menuOpen" class="dropdown" @click="menuOpen = false">
          <span class="muted small">{{ auth.user.loginId }}{{ auth.user.systemAdmin ? ' · 시스템 관리자' : '' }}</span>
          <RouterLink to="/account">비밀번호 변경</RouterLink>
          <button @click.stop="doLogout">로그아웃</button>
        </div>
      </div>
    </div>
  </header>

  <main class="wrap">
    <RouterView />
  </main>

  <footer v-if="!route.meta.public" class="wrap muted">
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
  margin-left: auto;
}
nav a {
  padding: 6px 10px;
  border-radius: 8px;
  color: var(--muted);
  text-decoration: none;
  white-space: nowrap;
  font-size: 15px;
}
nav a.router-link-active {
  color: var(--accent);
  background: var(--accent-soft);
  font-weight: 600;
}
.user {
  position: relative;
}
.user-btn {
  border: 0;
  background: none;
  cursor: pointer;
  white-space: nowrap;
  padding: 6px 4px;
  font-size: 14px;
}
.dropdown {
  position: absolute;
  right: 0;
  top: calc(100% + 6px);
  z-index: 10;
  min-width: 180px;
  display: flex;
  flex-direction: column;
  gap: 2px;
  padding: 8px;
  background: var(--surface);
  border: 1px solid var(--border);
  border-radius: 10px;
  box-shadow: 0 8px 24px rgb(0 0 0 / 0.12);
}
.dropdown a,
.dropdown button {
  text-align: left;
  padding: 8px 10px;
  border-radius: 6px;
  border: 0;
  background: none;
  color: var(--text);
  text-decoration: none;
  cursor: pointer;
  font-size: 14px;
}
.dropdown a:hover,
.dropdown button:hover {
  background: var(--accent-soft);
}
.small {
  font-size: 12px;
  padding: 4px 10px;
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
@media (max-width: 640px) {
  .logo {
    display: none;
  }
  nav {
    margin-left: 0;
  }
}
</style>
