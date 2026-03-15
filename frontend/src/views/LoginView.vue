<template>
  <div class="login-wrapper">
    <div class="hero">
      <h1>校园应急物资管理系统</h1>
      <p>Campus Emergency Material System</p>
    </div>
    <div class="login-panel">
      <h2>用户登录</h2>
      <el-form :model="form" @submit.prevent="handleLogin" label-width="0">
        <el-form-item>
          <el-input v-model="form.username" placeholder="用户名" prefix-icon="User" size="large" />
        </el-form-item>
        <el-form-item>
          <el-input v-model="form.password" placeholder="密码" type="password" prefix-icon="Lock" size="large" show-password />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleLogin" :loading="loading" size="large" style="width:100%">登 录</el-button>
        </el-form-item>
      </el-form>
      <div class="tip" v-if="errorMsg">{{ errorMsg }}</div>
    </div>
  </div>
</template>

<script setup>
import { reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { useAuthStore } from '../store/auth'

const router = useRouter()
const authStore = useAuthStore()
const form = reactive({ username: '', password: '' })
const loading = ref(false)
const errorMsg = ref('')

const handleLogin = async () => {
  if (loading.value) return
  loading.value = true
  errorMsg.value = ''
  try {
    await authStore.login(form.username, form.password)
    router.push('/dashboard')
  } catch (e) {
    errorMsg.value = e.message || '登录失败'
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.login-wrapper {
  min-height: 100vh;
  display: grid;
  grid-template-columns: 1fr 1fr;
  background: linear-gradient(135deg, #e0e7ff 0%, #f0f6ff 100%);
}
.hero {
  display: flex;
  flex-direction: column;
  justify-content: center;
  align-items: center;
  padding: 40px 24px;
  background: linear-gradient(135deg, var(--primary, #2563EB), #1e40af);
  color: #fff;
}
.hero h1 {
  font-size: 28px;
  margin-bottom: 12px;
}
.hero p {
  opacity: .8;
  font-size: 16px;
}
.login-panel {
  display: flex;
  flex-direction: column;
  justify-content: center;
  padding: 60px 48px;
  max-width: 420px;
  margin: auto;
}
.login-panel h2 {
  margin-bottom: 28px;
  font-size: 22px;
}
.tip {
  color: #f56c6c;
  font-size: 13px;
  margin-top: 8px;
}
@media (max-width: 768px) {
  .login-wrapper { grid-template-columns: 1fr; }
  .hero { display: none; }
}
</style>
