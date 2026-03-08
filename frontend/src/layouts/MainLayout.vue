<template>
  <div class="layout">
    <aside :class="['sidebar', { collapsed }]">
      <div class="logo" @click="go('/dashboard')">应急智管</div>
      <el-scrollbar>
        <el-menu :default-active="route.path" class="menu" @select="go">
          <el-menu-item v-for="menu in auth.menus" :key="menu.path" :index="menu.path">{{ menu.title }}</el-menu-item>
        </el-menu>
      </el-scrollbar>
      <div class="collapse-trigger" @click="collapsed = !collapsed">{{ collapsed ? '展开' : '收起' }}</div>
    </aside>

    <main class="main">
      <header class="top">
        <div>
          <strong>{{ auth.user?.realName || auth.user?.username }}</strong>
          <span class="role">（{{ auth.user?.roleCode }}）</span>
        </div>
        <el-space>
          <el-button text @click="refresh">刷新菜单</el-button>
          <el-button type="danger" plain @click="logout">退出</el-button>
        </el-space>
      </header>

      <section class="content">
        <router-view />
      </section>
    </main>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useAuthStore } from '../store/auth'

const route = useRoute()
const router = useRouter()
const auth = useAuthStore()
const collapsed = ref(false)

const go = (path) => router.push(path)
const logout = () => {
  auth.logout()
  router.replace('/login')
}
const refresh = () => auth.loadProfile()
</script>

<style scoped>
.layout {
  min-height: 100vh;
  display: grid;
  grid-template-columns: 220px 1fr;
}

.sidebar {
  background: linear-gradient(180deg, #153a4f, #0f6b63);
  color: #fff;
  display: flex;
  flex-direction: column;
}

.sidebar.collapsed {
  width: 74px;
}

.logo {
  font-size: 22px;
  font-weight: 700;
  padding: 18px;
  cursor: pointer;
}

.menu {
  border-right: none;
  background: transparent;
}

.menu :deep(.el-menu-item) {
  color: #dbe9ff;
}

.menu :deep(.el-menu-item.is-active) {
  color: #fff;
  background: rgba(255, 255, 255, 0.16);
}

.collapse-trigger {
  margin-top: auto;
  padding: 14px 18px;
  font-size: 12px;
  cursor: pointer;
  border-top: 1px solid rgba(255, 255, 255, 0.15);
}

.main {
  padding: 16px;
}

.top {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 14px;
}

.role {
  color: #6c7787;
}

.content {
  min-height: calc(100vh - 92px);
}

@media (max-width: 900px) {
  .layout {
    grid-template-columns: 1fr;
  }

  .sidebar {
    display: none;
  }
}
</style>
