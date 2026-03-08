<template>
  <div class="login-wrap">
    <div class="hero">
      <h1>校园应急物资智能管理系统</h1>
      <p>校园场景 · 应急保障 · 智能决策</p>
    </div>
    <el-card class="login-card">
      <h2>登录系统</h2>
      <el-form :model="form" @submit.prevent>
        <el-form-item>
          <el-input v-model="form.username" placeholder="用户名" />
        </el-form-item>
        <el-form-item>
          <el-input v-model="form.password" type="password" show-password placeholder="密码" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :loading="loading" style="width: 100%" @click="onLogin">登录</el-button>
        </el-form-item>
      </el-form>
      <div class="tips">演示账号：admin / warehouse / dept / approver，密码：123456</div>
    </el-card>
  </div>
</template>

<script setup>
import { reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { useAuthStore } from '../store/auth'

const router = useRouter()
const auth = useAuthStore()
const loading = ref(false)
const form = reactive({ username: 'admin', password: '123456' })

const onLogin = async () => {
  if (!form.username || !form.password) {
    ElMessage.warning('请输入用户名和密码')
    return
  }
  loading.value = true
  try {
    await auth.login(form.username, form.password)
    await router.push('/dashboard')
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.login-wrap {
  min-height: 100vh;
  display: grid;
  grid-template-columns: 1.2fr 1fr;
  gap: 24px;
  align-items: center;
  padding: 24px;
}

.hero {
  border-radius: 24px;
  padding: 42px;
  color: #0f6b63;
  background: linear-gradient(135deg, rgba(201, 246, 240, 0.9), rgba(232, 245, 255, 0.95));
}

.hero h1 {
  margin: 0;
  font-size: 40px;
}

.hero p {
  margin-top: 14px;
  font-size: 20px;
}

.login-card {
  border-radius: 20px;
}

.tips {
  color: #5f6b7a;
  font-size: 13px;
}

@media (max-width: 900px) {
  .login-wrap {
    grid-template-columns: 1fr;
  }

  .hero h1 {
    font-size: 28px;
  }
}
</style>
