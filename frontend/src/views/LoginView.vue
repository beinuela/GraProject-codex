<template>
  <div class="login-wrapper">
    <!-- Deep textured background -->
    <div class="noise-bg"></div>
    
    <div class="login-container">
      <div class="glass-panel">
        <div class="panel-header">
          <div class="logo-circle">
            <el-icon><Monitor /></el-icon>
          </div>
          <h1>校园物资智能管理系统</h1>
          <p>Campus Emergency Material System</p>
        </div>
        
        <el-form :model="form" @submit.prevent="handleLogin" label-width="0" class="login-form">
          <el-form-item>
            <el-input 
              v-model="form.username" 
              placeholder="输入用户名" 
              size="large" 
              class="custom-input custom-input-centered"
            >
              <template #prefix>
                <el-icon><User /></el-icon>
              </template>
            </el-input>
          </el-form-item>
          <el-form-item>
            <el-input 
              v-model="form.password" 
              placeholder="输入密码" 
              type="password" 
              size="large" 
              show-password 
              class="custom-input custom-input-centered"
            >
              <template #prefix>
                <el-icon><Lock /></el-icon>
              </template>
            </el-input>
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
/* 谷歌字体导入: Poppins */
@import url('https://fonts.googleapis.com/css2?family=Poppins:wght@400;500;600&display=swap');

.login-wrapper {
  position: relative;
  min-height: 100vh;
  width: 100vw;
  overflow: hidden;
  display: flex;
  align-items: center;
  justify-content: center;
  background: radial-gradient(circle at center, #1e293b 0%, #0f172a 50%, #020617 100%);
  font-family: -apple-system, BlinkMacSystemFont, 'Helvetica Neue', Helvetica, Arial, sans-serif;
}

/* 细腻的高级噪点纹理背景，提升质感 */
.noise-bg {
  position: absolute;
  top: 0; left: 0; right: 0; bottom: 0;
  background-image: url("data:image/svg+xml,%3Csvg viewBox='0 0 200 200' xmlns='http://www.w3.org/2000/svg'%3E%3Cfilter id='noiseFilter'%3E%3CfeTurbulence type='fractalNoise' baseFrequency='0.85' numOctaves='3' stitchTiles='stitch'/%3E%3C/filter%3E%3Crect width='100%25' height='100%25' filter='url(%23noiseFilter)' opactiy='0.05'/%3E%3C/svg%3E");
  opacity: 0.03; /* 仅作为非常微弱的纹理 */
  pointer-events: none;
  z-index: 0;
}

.login-container {
  position: relative;
  z-index: 10;
  width: 100%;
  max-width: 440px;
  padding: 0 20px;
  perspective: 1000px;
}

/* 多层磨砂玻璃卡片 */
.glass-panel {
  position: relative;
  background: rgba(15, 23, 42, 0.4);
  backdrop-filter: blur(40px);
  -webkit-backdrop-filter: blur(40px);
  border-radius: 32px;
  padding: 60px 48px;
  box-shadow: 
    0 30px 60px rgba(0, 0, 0, 0.6), /* 深沉的外阴影，制造悬浮感 */
    inset 0 1px 0 rgba(255, 255, 255, 0.15), /* 顶部受光面的细腻高光 */
    inset 0 -1px 0 rgba(0, 0, 0, 0.3); /* 底部暗面的压边效果 */
  animation: fadeInUp 0.6s cubic-bezier(0.2, 0.8, 0.2, 1) forwards;
  opacity: 0;
  transform: translateY(20px);
}

/* 利用伪元素制造柔和边缘发光 */
.glass-panel::before {
  content: "";
  position: absolute;
  top: -1px; left: -1px; right: -1px; bottom: -1px;
  border-radius: 33px; /* 比内容圆角大1px */
  background: linear-gradient(135deg, rgba(255,255,255,0.15) 0%, transparent 40%, rgba(255,255,255,0.03) 100%);
  z-index: -1;
  pointer-events: none;
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
  margin-bottom: 40px;
}

/* 更干净、流线型、雕塑感的图标 */
.logo-circle {
  width: 68px;
  height: 68px;
  margin: 0 auto 20px;
  background: rgba(30, 58, 138, 0.2);
  border: 1px solid rgba(59, 130, 246, 0.25);
  box-shadow: 
    inset 0 0 20px rgba(59, 130, 246, 0.4), /* 内部蓝色微光 */
    0 0 30px rgba(37, 99, 235, 0.3);        /* 外部柔和光晕 */
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 32px;
  color: #60a5fa;
}

.panel-header h1 {
  font-family: 'PingFang SC', 'Microsoft YaHei', sans-serif;
  font-size: 24px;
  font-weight: 800; /* 高端、平衡、重磅中文 */
  color: #f8fafc;
  margin: 0 0 8px 0;
  letter-spacing: 2px;
}

.panel-header p {
  font-family: 'Poppins', 'Helvetica Neue', Helvetica, Arial, sans-serif;
  font-size: 13px;
  font-weight: 500;
  color: #64748b;
  margin: 0;
  letter-spacing: 0.5px;
}

/* 表单与居中输入框定制 */
.login-form {
  margin-bottom: 24px;
}

::v-deep(.custom-input-centered .el-input__wrapper) {
  background: rgba(0, 0, 0, 0.2) !important;
  /* 柔和纤细的内阴影，增加深度 */
  box-shadow: inset 0 2px 5px rgba(0,0,0,0.4), inset 0 0 0 1px rgba(255,255,255,0.06) !important;
  border-radius: 16px;
  /* 左右边距预留 40px，确保文字真正居中，防止图标重叠 */
  padding: 0 40px; 
  height: 56px;
  transition: all 0.3s cubic-bezier(0.25, 0.8, 0.25, 1);
}

::v-deep(.custom-input-centered.is-focus .el-input__wrapper) {
  /* 聚焦时的柔和蓝色微光轮廓 */
  box-shadow: inset 0 2px 5px rgba(0,0,0,0.4), inset 0 0 0 1px #3b82f6, 0 0 16px rgba(59,130,246,0.25) !important;
  background: rgba(0, 0, 0, 0.3) !important;
}

::v-deep(.custom-input-centered .el-input__inner) {
  color: #f8fafc !important;
  background-color: transparent !important;
  /* 完美绝对居中 */
  text-align: center !important;
  font-size: 15px;
  font-family: 'Poppins', 'PingFang SC', sans-serif;
  letter-spacing: 1px;
}

::v-deep(.custom-input-centered .el-input__inner::placeholder) {
  color: #64748b;
  text-align: center;
  /* 占位符轻微交互 */
  transition: opacity 0.3s ease;
}

/* 彻底解决浏览器(Chrome)自动填充引发的白底/黄底问题！ */
::v-deep(.custom-input-centered .el-input__inner:-webkit-autofill),
::v-deep(.custom-input-centered .el-input__inner:-webkit-autofill:hover),
::v-deep(.custom-input-centered .el-input__inner:-webkit-autofill:focus),
::v-deep(.custom-input-centered .el-input__inner:-webkit-autofill:active) {
  -webkit-text-fill-color: #f8fafc !important;
  /* 利用极长的过渡时间让背景色保持透明，这是针对 Chrome autofill 最佳 Hack 方案 */
  transition: background-color 5000s ease-in-out 0s !important;
  background-color: transparent !important;
}

::v-deep(.custom-input-centered.is-focus .el-input__inner::placeholder) {
  opacity: 0.5; /* 聚焦时占位符变淡增添质感 */
}

/* 前缀图标 (User/Lock) */
::v-deep(.custom-input-centered .el-input__prefix) {
  position: absolute;
  left: 16px;
  color: #64748b;
  font-size: 18px;
  transition: all 0.3s ease;
}
::v-deep(.custom-input-centered.is-focus .el-input__prefix) {
  color: #3b82f6; /* 聚焦时图标变蓝 */
}

/* 针对密码框带有小眼睛图标的特殊对齐处理 */
::v-deep(.custom-input-centered .el-input__suffix) {
  position: absolute;
  right: 16px;
  color: #64748b;
  /* 确保垂直居中与圆润过渡 */
  transition: all 0.3s cubic-bezier(0.25, 0.8, 0.25, 1);
}
::v-deep(.custom-input-centered .el-input__suffix-inner) {
  display: flex;
  align-items: center;
}

/* 登录按钮定制 (高级金属感发光) */
.form-actions {
  margin-top: 36px;
}

.login-btn {
  width: 100%;
  height: 56px; /* 与输入框等高 */
  border-radius: 28px; /* 更加圆润 */
  font-size: 16px;
  font-weight: 600;
  letter-spacing: 1px;
  border: none;
  /* 丰富且深沉的渐变蓝色底色 */
  background: linear-gradient(180deg, #2563eb 0%, #1d4ed8 100%);
  color: white;
  /* 顶部 1px 高光模拟金属物理按键，底部扩散光模拟发光 */
  box-shadow: inset 0 1px 0 rgba(255,255,255,0.25), 0 8px 20px rgba(30, 58, 138, 0.6);
  transition: all 0.3s cubic-bezier(0.25, 0.8, 0.25, 1);
}

.login-btn:hover {
  transform: translateY(-2px);
  /* 提亮悬浮色，柔和的呼吸感过渡 */
  background: linear-gradient(180deg, #3b82f6 0%, #2563eb 100%);
  box-shadow: inset 0 1px 0 rgba(255,255,255,0.3), 0 12px 24px rgba(37, 99, 235, 0.7);
}

.login-btn:active {
  transform: translateY(1px);
  box-shadow: inset 0 2px 4px rgba(0,0,0,0.2), 0 4px 8px rgba(30, 58, 138, 0.4);
}

/* 错误提示 */
.tip-container {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  margin-top: 24px;
  padding: 12px;
  border-radius: 12px;
  background: rgba(239, 68, 68, 0.1);
  border: 1px solid rgba(239, 68, 68, 0.2);
  color: #fca5a5;
  font-size: 14px;
  animation: slideIn 0.3s cubic-bezier(0.25, 0.8, 0.25, 1);
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
    padding: 40px 30px;
    border-radius: 24px;
  }
}
</style>
