<script setup>
import { reactive, ref } from 'vue'
import { RouterLink, useRouter } from 'vue-router'
import { signup } from '@/auth'
import { errorMessage } from '@/api'

const router = useRouter()
const form = reactive({ loginId: '', password: '', passwordConfirm: '', name: '', email: '', phone: '' })
const error = ref('')
const loading = ref(false)

async function submit() {
  error.value = ''
  if (form.password !== form.passwordConfirm) {
    error.value = '비밀번호 확인이 일치하지 않아요.'
    return
  }
  loading.value = true
  try {
    const { passwordConfirm, ...body } = form
    await signup({ ...body, loginId: body.loginId.trim(), phone: body.phone.trim() || null })
    router.replace({ path: '/login', query: { signedUp: '1' } })
  } catch (e) {
    error.value = errorMessage(e)
  } finally {
    loading.value = false
  }
}
</script>

<template>
  <div class="auth-page">
    <form class="card auth-card" @submit.prevent="submit">
      <h1>회원가입</h1>
      <p class="muted">이름, 이메일, 휴대폰 번호는 암호화해서 저장해요.</p>

      <label>
        아이디
        <input v-model="form.loginId" autocomplete="username" required minlength="4" maxlength="20"
               pattern="[A-Za-z0-9_]{4,20}" title="영문, 숫자, _ 로 4~20자" />
        <span class="hint">영문, 숫자, _ 로 4~20자</span>
      </label>
      <label>
        비밀번호
        <input v-model="form.password" type="password" autocomplete="new-password" required minlength="8" maxlength="64" />
        <span class="hint">영문과 숫자를 포함해 8자 이상</span>
      </label>
      <label>
        비밀번호 확인
        <input v-model="form.passwordConfirm" type="password" autocomplete="new-password" required />
      </label>
      <label>
        이름
        <input v-model="form.name" autocomplete="name" required maxlength="50" />
      </label>
      <label>
        이메일
        <input v-model="form.email" type="email" autocomplete="email" required maxlength="100" />
      </label>
      <label>
        휴대폰 번호 <span class="muted">(선택)</span>
        <input v-model="form.phone" type="tel" autocomplete="tel" placeholder="010-1234-5678" />
      </label>
      <p v-if="error" class="error" role="alert">{{ error }}</p>
      <button class="btn primary full" :disabled="loading">{{ loading ? '가입 중…' : '가입하기' }}</button>
      <p class="muted center">이미 계정이 있나요? <RouterLink to="/login">로그인</RouterLink></p>
    </form>
  </div>
</template>
