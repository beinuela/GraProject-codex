<template>
  <div class="login-wrapper">
    <div class="mesh-bg">
      <div class="blob color-1"></div>
      <div class="blob color-2"></div>
      <div class="blob color-3"></div>
      <div class="blob color-4"></div>
    </div>
    
    <div class="login-container">
      <div class="glass-panel">
        <div class="panel-header">
          <div class="logo-circle">
            <el-icon><Monitor /></el-icon>
          </div>
          <h1>校园应急物资管理系统</h1>
          <p>Campus Emergency Material System</p>
        </div>
        
        <el-form :model="form" @submit.prevent="handleLogin" label-width="0" class="login-form">
          <el-form-item>
            <el-input 
              v-model="form.username" 
              placeholder="用户名 / Username" 
              prefix-icon="User" 
              size="large" 
              class="custom-input"
            />
          </el-form-item>
          <el-form-item>
            <el-input 
              v-model="form.password" 
              placeholder="密码 / Password" 
              type="password" 
              prefix-icon="Lock" 
              size="large" 
              show-password 
              class="custom-input"
            />
          </el-form-item>
          <div class="form-actions">
            <el-button 
              type="primary" 
              @click="handleLogin" 
              :loading="loading" 
              class="login-btn"
            >
              登录系统
            </el-button>
          </div>
        </el-form>
        <div class="tip-container" v-if="errorMsg">
          <el-icon class="tip-icon"><Warning /></el-icon>
          <span class="tip-text">{{ errorMsg }}</span>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { useAuthStore } from '../store/auth'
import { User, Lock, Monitor, Warning } from '@element-plus/icons-vue'

const router = useRouter()
const authStore = useAuthStore()
const form = reactive({ username: '', password: '' })
const loading = ref(false)
const errorMsg = ref('')

const handleLogin = async () => {
  if (loading.value) return
  if (!form.username || !form.password) {
    errorMsg.value = '请输入用户名和密码'
    return
  }
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
  position: relative;
  min-height: 100vh;
  width: 100vw;
  overflow: hidden;
  display: flex;
  align-items: center;
  justify-content: center;
  background-color: #0d1117;
  font-family: 'Inter', -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif;
}

/* 动态流体渐变背景 */
.mesh-bg {
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  overflow: hidden;
  z-index: 0;
  background: #0f172a; /* 沉稳科技深蓝底色 */
}

.blob {
  position: absolute;
  filter: blur(80px);
  border-radius: 50%;
  opacity: 0.6;
  animation: float 20s infinite alternate cubic-bezier(0.45, 0.05, 0.55, 0.95);
  transform: translateZ(0);
  will-change: transform;
}

/* 契合应急管理系统的沉稳与科技感配色 */
.color-1 {
  background: #1e40af; /* 强烈的品牌深蓝 */
  width: 60vw;
  height: 60vw;
  top: -10%;
  left: -10%;
  animation-delay: 0s;
}

.color-2 {
  background: #0284c7; /* 清爽青蓝 */
  width: 50vw;
  height: 50vw;
  bottom: -20%;
  right: -10%;
  animation-delay: -5s;
}

.color-3 {
  background: #047857; /* 沉稳的墨绿/青色 */
  width: 40vw;
  height: 40vw;
  top: 40%;
  left: 30%;
  animation-delay: -10s;
  opacity: 0.4;
}

.color-4 {
  background: #ea580c; /* 微橙色，带有警示/活力意味的对比色 */
  width: 30vw;
  height: 30vw;
  top: 10%;
  right: 15%;
  animation-delay: -15s;
  opacity: 0.3;
}

@keyframes float {
  0% { transform: translate(0, 0) scale(1); }
  33% { transform: translate(30px, -50px) scale(1.1); }
  66% { transform: translate(-20px, 20px) scale(0.9); }
  100% { transform: translate(40px, 40px) scale(1.05); }
}

/* 毛玻璃登录卡片主体 */
.login-container {
  position: relative;
  z-index: 10;
  width: 100%;
  max-width: 440px;
  padding: 0 20px;
  perspective: 1000px;
}

.glass-panel {
  background: rgba(30, 41, 59, 0.5); /* 半透明深色面板 */
  backdrop-filter: blur(24px); /* 强大的高斯模糊 */
  -webkit-backdrop-filter: blur(24px);
  border: 1px solid rgba(255, 255, 255, 0.12); /* 高光边缘 */
  border-radius: 24px;
  padding: 48px 40px;
  box-shadow: 0 25px 50px -12px rgba(0, 0, 0, 0.5), 
              inset 0 1px 0 rgba(255, 255, 255, 0.1);
  animation: fadeInUp 0.5s cubic-bezier(0.25, 1, 0.5, 1) forwards;
  opacity: 0;
}

/* 如果不支持backdrop-filter的后备方案 */
@supports not (backdrop-filter: blur(24px)) {
  .glass-panel {
    background: rgba(15, 23, 42, 0.95);
  }
}

@keyframes fadeInUp {
  from {
    opacity: 0;
    transform: translateY(30px) translateZ(0);
  }
  to {
    opacity: 1;
    transform: translateY(0) translateZ(0);
  }
}

/* 顶部信息区域 */
.panel-header {
  text-align: center;
  margin-bottom: 36px;
}

.logo-circle {
  width: 64px;
  height: 64px;
  margin: 0 auto 20px;
  background: linear-gradient(135deg, #3b82f6, #1d4ed8);
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 32px;
  color: white;
  box-shadow: 0 8px 16px rgba(37, 99, 235, 0.3);
}

.panel-header h1 {
  font-size: 24px;
  font-weight: 600;
  color: #f8fafc;
  margin: 0 0 8px 0;
  letter-spacing: 1px;
}

.panel-header p {
  font-size: 14px;
  color: #94a3b8;
  margin: 0;
  letter-spacing: 0.5px;
}

/* 表单与输入框定制 */
.login-form {
  margin-bottom: 24px;
}

::v-deep(.custom-input .el-input__wrapper) {
  background-color: rgba(15, 23, 42, 0.4) !important;
  border-radius: 12px;
  box-shadow: 0 0 0 1px rgba(255, 255, 255, 0.08) inset !important;
  padding: 12px 16px;
  transition: all 0.3s cubic-bezier(0.25, 1, 0.5, 1);
}

::v-deep(.custom-input .el-input__wrapper.is-focus) {
  background-color: rgba(255, 255, 255, 0.05) !important;
  box-shadow: 0 0 0 1px #3b82f6 inset, 0 0 12px rgba(59, 130, 246, 0.4) !important;
}

::v-deep(.custom-input .el-input__inner) {
  color: #f8fafc !important;
  height: 24px;
  font-size: 15px;
}

::v-deep(.custom-input .el-input__inner::placeholder) {
  color: #64748b;
}

::v-deep(.custom-input .el-input__prefix) {
  color: #94a3b8;
  font-size: 18px;
  margin-right: 8px;
}

::v-deep(.custom-input .el-input__suffix) {
  color: #94a3b8;
}

/* 登录按钮定制 */
.form-actions {
  margin-top: 32px;
}

.login-btn {
  width: 100%;
  height: 48px;
  border-radius: 12px;
  font-size: 16px;
  font-weight: 500;
  letter-spacing: 1px;
  border: none;
  background: linear-gradient(135deg, #3b82f6 0%, #2563eb 100%);
  color: white;
  box-shadow: 0 4px 14px rgba(37, 99, 235, 0.39);
  transition: all 0.3s cubic-bezier(0.25, 1, 0.5, 1);
  overflow: hidden;
  position: relative;
}

.login-btn::after {
  content: '';
  position: absolute;
  top: 0;
  left: -100%;
  width: 100%;
  height: 100%;
  background: linear-gradient(90deg, transparent, rgba(255, 255, 255, 0.2), transparent);
  transition: 0.5s;
}

.login-btn:hover {
  transform: translateY(-2px);
  box-shadow: 0 6px 20px rgba(37, 99, 235, 0.5);
  background: linear-gradient(135deg, #60a5fa 0%, #3b82f6 100%);
}

.login-btn:hover::after {
  left: 100%;
}

.login-btn:active {
  transform: translateY(1px) scale(0.97);
  box-shadow: 0 2px 8px rgba(37, 99, 235, 0.4);
}

/* 错误提示 */
.tip-container {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  padding: 12px;
  border-radius: 8px;
  background: rgba(239, 68, 68, 0.1);
  border: 1px solid rgba(239, 68, 68, 0.2);
  color: #fca5a5;
  font-size: 14px;
  animation: slideIn 0.3s cubic-bezier(0.25, 1, 0.5, 1);
}

.tip-icon {
  font-size: 16px;
}

@keyframes slideIn {
  from { opacity: 0; transform: translateY(-10px); }
  to { opacity: 1; transform: translateY(0); }
}

@media (max-width: 480px) {
  .glass-panel {
    padding: 36px 28px;
  }
}
</style>
