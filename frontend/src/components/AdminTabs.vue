<script setup>
import { RouterLink } from 'vue-router'
import { isSystemAdmin } from '@/auth'
</script>

<template>
  <div class="tabs">
    <RouterLink to="/admin/permissions">권한 관리</RouterLink>
    <RouterLink v-if="isSystemAdmin()" to="/admin/users">사용자 관리</RouterLink>
  </div>
</template>

<style scoped>
.tabs {
  display: flex;
  gap: 4px;
  border-bottom: 1px solid var(--border);
  margin-bottom: 16px;
}
a {
  padding: 8px 14px;
  color: var(--muted);
  text-decoration: none;
  border-bottom: 2px solid transparent;
  margin-bottom: -1px;
}
a.router-link-exact-active {
  color: var(--accent);
  border-bottom-color: var(--accent);
  font-weight: 600;
}
</style>
