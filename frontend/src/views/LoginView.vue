<template>
  <div class="login-shell">
    <div class="login-shell__noise"></div>

    <section class="login-stage">
      <div class="login-story">
        <span class="login-story__eyebrow">Campus Material Control</span>
        <h1>校园物资智能管理系统</h1>
        <p>
          统一管理物资档案、库存台账、申领调拨、预警事件与安全策略，
          让日常运营和应急响应都保持同一条数字链路。
        </p>

        <div class="login-story__metrics">
          <article>
            <strong>01</strong>
            <span>库存与批次一体化追踪</span>
          </article>
          <article>
            <strong>02</strong>
            <span>申领、调拨、预警闭环联动</span>
          </article>
          <article>
            <strong>03</strong>
            <span>支持大屏与后台双视图协同</span>
          </article>
        </div>

        <div class="login-story__footer">
          <span>安全策略、日志与通知统一沉淀</span>
          <span class="mono">OPS / FLOW / SAFETY</span>
        </div>
      </div>

      <div class="login-card">
        <div class="login-card__header">
          <div class="login-card__mark">
            <el-icon><Monitor /></el-icon>
          </div>
          <div>
            <span class="login-card__eyebrow">Secure Access</span>
            <h2>进入数据舱</h2>
            <p>使用系统账号访问校园物资运营后台。</p>
          </div>
        </div>

        <el-form :model="form" @submit.prevent="handleLogin" class="login-form">
          <el-form-item>
            <el-input v-model="form.username" placeholder="用户名" size="large">
              <template #prefix>
                <el-icon><User /></el-icon>
              </template>
            </el-input>
          </el-form-item>
          <el-form-item>
            <el-input v-model="form.password" placeholder="密码" type="password" show-password size="large">
              <template #prefix>
                <el-icon><Lock /></el-icon>
              </template>
            </el-input>
          </el-form-item>

          <div class="login-form__submit">
            <el-button type="primary" :loading="loading" class="login-form__button" @click="handleLogin">
              登录系统
            </el-button>
          </div>
        </el-form>

        <div v-if="errorMsg" class="login-error">
          <el-icon><Warning /></el-icon>
          <span>{{ errorMsg }}</span>
        </div>

        <div class="login-card__tips">
          <span>登录后将加载权限菜单、通知徽标与个人资料。</span>
        </div>
      </div>
    </section>
  </div>
</template>

<script setup>
import { reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { Lock, Monitor, User, Warning } from '@element-plus/icons-vue'
import { useAuthStore } from '../store/auth'

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
  } catch (error) {
    errorMsg.value = error.message || '登录失败'
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.login-shell {
  position: relative;
  min-height: 100vh;
  display: grid;
  place-items: center;
  overflow: hidden;
  background:
    radial-gradient(circle at top left, rgba(38, 112, 233, 0.18), transparent 26%),
    radial-gradient(circle at bottom right, rgba(53, 212, 198, 0.12), transparent 20%),
    linear-gradient(145deg, #07111f 0%, #0c1628 45%, #0b1422 100%);
}

.login-shell__noise {
  position: absolute;
  inset: 0;
  opacity: 0.18;
  background-image:
    linear-gradient(rgba(130, 165, 214, 0.08) 1px, transparent 1px),
    linear-gradient(90deg, rgba(130, 165, 214, 0.08) 1px, transparent 1px);
  background-size: 28px 28px;
  mask-image: radial-gradient(circle at center, black 30%, transparent 92%);
}

.login-stage {
  position: relative;
  z-index: 1;
  display: grid;
  grid-template-columns: minmax(0, 1.2fr) minmax(380px, 460px);
  gap: 24px;
  width: min(1180px, calc(100vw - 40px));
  align-items: stretch;
}

.login-story,
.login-card {
  border: 1px solid rgba(163, 200, 255, 0.1);
  border-radius: 34px;
  backdrop-filter: blur(20px);
}

.login-story {
  display: grid;
  align-content: space-between;
  gap: 24px;
  padding: 42px;
  background:
    radial-gradient(circle at top left, rgba(38, 112, 233, 0.2), transparent 28%),
    linear-gradient(180deg, rgba(11, 24, 44, 0.88), rgba(7, 15, 28, 0.84));
  color: rgba(240, 246, 255, 0.94);
  box-shadow: 0 30px 80px rgba(5, 10, 20, 0.38);
}

.login-story__eyebrow,
.login-card__eyebrow {
  display: inline-flex;
  align-items: center;
  min-height: 30px;
  padding: 0 12px;
  border-radius: 999px;
  background: rgba(53, 212, 198, 0.12);
  color: #7ee8de;
  font-size: 12px;
  font-weight: 700;
  letter-spacing: 0.08em;
  text-transform: uppercase;
}

.login-story h1 {
  margin: 0;
  font-family: var(--font-display);
  font-size: clamp(42px, 5vw, 68px);
  line-height: 0.96;
  letter-spacing: -0.05em;
}

.login-story p {
  max-width: 40ch;
  margin: 0;
  color: rgba(196, 212, 236, 0.8);
  line-height: 1.8;
  font-size: 16px;
}

.login-story__metrics {
  display: grid;
  gap: 12px;
}

.login-story__metrics article {
  display: grid;
  grid-template-columns: 60px minmax(0, 1fr);
  gap: 14px;
  align-items: center;
  padding: 16px 18px;
  border-radius: 20px;
  border: 1px solid rgba(163, 200, 255, 0.1);
  background: rgba(255, 255, 255, 0.04);
}

.login-story__metrics strong {
  font-family: var(--font-display);
  font-size: 24px;
  color: #fff;
}

.login-story__metrics span {
  color: rgba(223, 233, 248, 0.88);
  line-height: 1.6;
}

.login-story__footer {
  display: flex;
  justify-content: space-between;
  gap: 12px;
  padding-top: 18px;
  color: rgba(171, 188, 212, 0.74);
  border-top: 1px solid rgba(163, 200, 255, 0.12);
  font-size: 13px;
}

.login-card {
  padding: 30px;
  background:
    radial-gradient(circle at top left, rgba(38, 112, 233, 0.12), transparent 28%),
    linear-gradient(180deg, rgba(255, 255, 255, 0.94), rgba(246, 249, 255, 0.92));
  box-shadow: 0 28px 70px rgba(5, 10, 20, 0.28);
  display: grid;
  align-content: center;
  gap: 22px;
}

.login-card__header {
  display: grid;
  grid-template-columns: 68px minmax(0, 1fr);
  gap: 16px;
  align-items: center;
}

.login-card__mark {
  display: grid;
  place-items: center;
  width: 68px;
  height: 68px;
  border-radius: 24px;
  background: linear-gradient(135deg, rgba(38, 112, 233, 0.94), rgba(53, 212, 198, 0.84));
  color: #fff;
  font-size: 30px;
  box-shadow: 0 18px 38px rgba(38, 112, 233, 0.26);
}

.login-card__header h2 {
  margin: 10px 0 6px;
  font-family: var(--font-display);
  font-size: 32px;
  line-height: 1;
  letter-spacing: -0.04em;
  color: var(--text-primary);
}

.login-card__header p {
  margin: 0;
  color: var(--text-secondary);
  line-height: 1.7;
}

.login-form {
  display: grid;
  gap: 6px;
}

.login-form__submit {
  padding-top: 8px;
}

.login-form__button {
  width: 100%;
  min-height: 52px;
  border-radius: 18px;
  font-size: 15px;
}

.login-error {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 14px 16px;
  border-radius: 18px;
  background: rgba(250, 82, 82, 0.12);
  border: 1px solid rgba(250, 82, 82, 0.16);
  color: var(--danger-strong);
  font-weight: 600;
}

.login-card__tips {
  padding-top: 10px;
  border-top: 1px solid var(--border-subtle);
  color: var(--text-tertiary);
  font-size: 13px;
}

@media (max-width: 960px) {
  .login-stage {
    grid-template-columns: 1fr;
  }

  .login-story {
    padding: 30px;
  }

  .login-story__footer {
    flex-direction: column;
  }
}

@media (max-width: 640px) {
  .login-stage {
    width: min(100vw - 24px, 560px);
  }

  .login-card,
  .login-story {
    padding: 24px;
    border-radius: 28px;
  }

  .login-card__header {
    grid-template-columns: 1fr;
  }
}
</style>
