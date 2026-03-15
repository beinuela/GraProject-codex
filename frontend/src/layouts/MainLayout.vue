<template>
  <div class="layout">
    <aside class="sidebar" :class="{ collapsed: sidebarCollapsed }">
      <div class="logo" @click="$router.push('/dashboard')">
        <el-icon size="24"><HomeFilled /></el-icon>
        <span v-if="!sidebarCollapsed" class="logo-text">
          <strong>应急物资管理</strong>
          <small>Campus EMS</small>
        </span>
      </div>
      <nav class="nav-menu">
        <router-link
          v-for="item in menus"
          :key="item.key"
          :to="item.path"
          class="nav-item"
          :class="{ active: $route.path === item.path }"
        >
          <span class="nav-label">{{ item.title }}</span>
        </router-link>
      </nav>
      <div class="sidebar-toggle" @click="sidebarCollapsed = !sidebarCollapsed">
        <span>{{ sidebarCollapsed ? '展开导航' : '< 收起导航' }}</span>
      </div>
    </aside>
    <div class="content">
      <header class="topbar">
        <span class="greeting">你好，{{ realName || username }}</span>
        <el-space>
          <el-button text @click="$router.push('/notification/list')">
            <el-badge :value="unreadCount" :hidden="!unreadCount" :max="99">
              <el-icon size="20"><Bell /></el-icon>
            </el-badge>
          </el-button>
          <el-dropdown @command="handleCommand">
            <span class="avatar-area">
              <el-avatar size="small">{{ (realName || username || 'U').charAt(0) }}</el-avatar>
              <span class="username-text">{{ realName || username }}</span>
            </span>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item command="logout">退出登录</el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </el-space>
      </header>
      <main class="main-body">
        <router-view />
      </main>
    </div>
  </div>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { HomeFilled, Bell } from '@element-plus/icons-vue'
import { apiGet } from '../api'
import { useAuthStore } from '../store/auth'

const router = useRouter()
const authStore = useAuthStore()
const menus = ref([])
const sidebarCollapsed = ref(false)
const username = ref(localStorage.getItem('username') || '')
const realName = ref(localStorage.getItem('realName') || '')
const unreadCount = ref(0)

const loadMenus = async () => {
  try {
    menus.value = await apiGet('/api/auth/menus')
  } catch {
    menus.value = []
  }
}

const loadUnread = async () => {
  try {
    const list = await apiGet('/api/notification')
    unreadCount.value = list.filter(n => !n.isRead).length
  } catch {
    unreadCount.value = 0
  }
}

const handleCommand = (cmd) => {
  if (cmd === 'logout') {
    authStore.logout()
    router.push('/login')
  }
}

onMounted(async () => {
  await loadMenus()
  await loadUnread()
})
</script>

<style scoped>
.layout {
  display: grid;
  grid-template-columns: 220px 1fr;
  min-height: 100vh;
  background: var(--bg-page, #f5f7fa);
}
.sidebar {
  background: #fff;
  border-right: 1px solid var(--divider, #e8e8e8);
  display: flex;
  flex-direction: column;
  transition: width .2s;
}
.sidebar.collapsed {
  width: 64px;
}
.sidebar.collapsed .nav-label,
.sidebar.collapsed .logo-text {
  display: none;
}
.logo {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 20px 16px;
  cursor: pointer;
}
.logo-text {
  display: flex;
  flex-direction: column;
  line-height: 1.3;
}
.logo-text strong {
  font-size: 15px;
  color: var(--primary, #2563EB);
}
.logo-text small {
  font-size: 11px;
  color: #999;
}
.nav-menu {
  flex: 1;
  overflow-y: auto;
  padding: 8px 0;
}
.nav-item {
  display: block;
  padding: 10px 20px;
  color: #333;
  text-decoration: none;
  font-size: 14px;
  border-left: 3px solid transparent;
  transition: all .15s;
}
.nav-item:hover {
  background: #f0f6ff;
}
.nav-item.active {
  color: var(--primary, #2563EB);
  background: #e8f0fe;
  border-left-color: var(--primary, #2563EB);
  font-weight: 600;
}
.sidebar-toggle {
  padding: 12px 16px;
  text-align: center;
  font-size: 12px;
  color: #999;
  cursor: pointer;
  border-top: 1px solid #eee;
}
.content {
  display: flex;
  flex-direction: column;
  min-width: 0;
}
.topbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 24px;
  height: 56px;
  background: #fff;
  border-bottom: 1px solid var(--divider, #e8e8e8);
}
.greeting {
  font-size: 14px;
  color: var(--primary, #2563EB);
}
.avatar-area {
  display: flex;
  align-items: center;
  gap: 8px;
  cursor: pointer;
}
.username-text {
  font-size: 14px;
}
.main-body {
  flex: 1;
  padding: 20px 24px;
  overflow-y: auto;
}
/* ===== 响应式 ===== */
@media (max-width: 768px) {
  .layout { grid-template-columns: 1fr; }
  .sidebar { display: none; }
}
</style>
