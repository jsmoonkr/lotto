<script setup>
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { api, errorMessage } from '@/api'
import { auth, clearAuth } from '@/auth'

const router = useRouter()
const current = ref('')
const next = ref('')
const confirm = ref('')
const error = ref('')
const loading = ref(false)

async function submit() {
  error.value = ''
  if (next.value !== confirm.value) {
    error.value = '새 비밀번호 확인이 일치하지 않아요.'
    return
  }
  loading.value = true
  try {
    await api.changePassword(current.value, next.value)
    // 서버가 모든 기기의 로그인을 끊으므로 다시 로그인한다.
    clearAuth()
    router.replace('/login')
  } catch (e) {
    error.value = errorMessage(e)
  } finally {
    loading.value = false
  }
}
</script>

<template>
  <section class="card narrow">
    <h2>내 정보</h2>
    <dl class="info">
      <dt>아이디</dt><dd>{{ auth.user.loginId }}</dd>
      <dt>이름</dt><dd>{{ auth.user.name }}</dd>
      <dt>이메일</dt><dd>{{ auth.user.email ?? '-' }}</dd>
      <dt>휴대폰</dt><dd>{{ auth.user.phone ?? '-' }}</dd>
    </dl>
  </section>

  <form class="card narrow form" @submit.prevent="submit">
    <h2>비밀번호 변경</h2>
    <p class="muted">바꾸면 모든 기기에서 로그아웃돼요.</p>
    <label>
      현재 비밀번호
      <input v-model="current" type="password" autocomplete="current-password" required />
    </label>
    <label>
      새 비밀번호
      <input v-model="next" type="password" autocomplete="new-password" required minlength="8" maxlength="64" />
      <span class="hint">영문과 숫자를 포함해 8자 이상</span>
    </label>
    <label>
      새 비밀번호 확인
      <input v-model="confirm" type="password" autocomplete="new-password" required />
    </label>
    <p v-if="error" class="error" role="alert">{{ error }}</p>
    <button class="btn primary" :disabled="loading">{{ loading ? '바꾸는 중…' : '비밀번호 바꾸기' }}</button>
  </form>
</template>

<style scoped>
.narrow {
  max-width: 480px;
}
.narrow + .narrow {
  margin-top: 16px;
}
.info {
  display: grid;
  grid-template-columns: 80px 1fr;
  gap: 8px 12px;
  margin: 16px 0 0;
}
.info dt {
  color: var(--muted);
}
.info dd {
  margin: 0;
}
.form {
  display: flex;
  flex-direction: column;
  gap: 12px;
}
.form .muted {
  margin: 0;
}
.form button {
  align-self: flex-start;
}
</style>
