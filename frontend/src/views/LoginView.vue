<script setup>
import { ref } from 'vue'
import { RouterLink, useRoute, useRouter } from 'vue-router'
import { homePath, login } from '@/auth'
import { errorMessage } from '@/api'

const route = useRoute()
const router = useRouter()
const loginId = ref('')
const password = ref('')
const error = ref('')
const loading = ref(false)
const justSignedUp = route.query.signedUp === '1'

async function submit() {
  error.value = ''
  loading.value = true
  try {
    await login(loginId.value.trim(), password.value)
    const redirect = typeof route.query.redirect === 'string' && route.query.redirect.startsWith('/')
      ? route.query.redirect
      : homePath()
    router.replace(redirect)
  } catch (e) {
    error.value = errorMessage(e)
    password.value = ''
  } finally {
    loading.value = false
  }
}
</script>

<template>
  <div class="auth-page">
    <form class="card auth-card" @submit.prevent="submit">
      <h1>로또 추천</h1>
      <p class="muted">로그인해 주세요.</p>
      <p v-if="justSignedUp" class="notice">가입이 끝났어요. 처음에는 통계 메뉴만 볼 수 있고, 다른 메뉴는 관리자가 권한을 주면 열려요.</p>

      <label>
        아이디
        <input v-model="loginId" autocomplete="username" required autofocus />
      </label>
      <label>
        비밀번호
        <input v-model="password" type="password" autocomplete="current-password" required />
      </label>
      <p v-if="error" class="error" role="alert">{{ error }}</p>
      <button class="btn primary full" :disabled="loading">{{ loading ? '확인 중…' : '로그인' }}</button>
      <p class="muted center">계정이 없나요? <RouterLink to="/signup">회원가입</RouterLink></p>
    </form>
  </div>
</template>
