<template>
  <div class="layout">
    <aside class="sidebar" :class="{ collapsed: sidebarCollapsed }">
      <div class="sidebar-logo" @click="$router.push('/dashboard')" v-if="!sidebarCollapsed">
        <div class="logo-shield">
          <svg viewBox="0 0 24 24" fill="currentColor" width="28" height="28" style="color: #0ea5e9;">
            <path d="M12 1L3 5v6c0 5.55 3.84 10.74 9 12 5.16-1.26 9-6.45 9-12V5l-9-4zm0 2.18l7 3.12v4.7c0 4.67-3.13 8.9-7 10.02-3.87-1.12-7-5.35-7-10.02v-4.7l7-3.12zm-1 6.82V8h2v2h2v2h-2v2h-2v-2H9v-2h2z" />
          </svg>
        </div>
        <div class="logo-text">
          <strong style="font-size: 15px; color: #1e293b; letter-spacing: 1px;">应急物资管理</strong>
          <span style="font-size: 13px; color: #0ea5e9; font-weight: bold;">Campus EMS</span>
        </div>
      </div>
      <div class="sidebar-logo collapsed-logo" @click="$router.push('/dashboard')" v-else>
         <svg viewBox="0 0 24 24" fill="currentColor" width="24" height="24" style="color: #0ea5e9;">
            <path d="M12 1L3 5v6c0 5.55 3.84 10.74 9 12 5.16-1.26 9-6.45 9-12V5l-9-4zm0 2.18l7 3.12v4.7c0 4.67-3.13 8.9-7 10.02-3.87-1.12-7-5.35-7-10.02v-4.7l7-3.12zm-1 6.82V8h2v2h2v2h-2v2h-2v-2H9v-2h2z" />
         </svg>
      </div>
      <nav class="nav-menu">
        <div v-for="group in menuGroups" :key="group.name">
          <!-- 首页组：直接展示 -->
          <template v-if="group.name === '首页'">
            <router-link
              v-for="item in group.items"
              :key="item.key"
              :to="item.path"
              class="nav-item"
              :class="{ active: $route.path === item.path }"
            >
              <el-icon :size="16"><component :is="groupIcons[group.name]" /></el-icon>
              <span class="nav-label">{{ item.title }}</span>
            </router-link>
          </template>
          <!-- 其他组：可折叠 -->
          <template v-else>
            <div class="nav-group-header" @click="toggleGroup(group.name)">
              <div class="nav-group-left">
                <el-icon :size="15"><component :is="groupIcons[group.name] || Setting" /></el-icon>
                <span v-if="!sidebarCollapsed" class="nav-group-title">{{ group.name }}</span>
              </div>
              <el-icon v-if="!sidebarCollapsed" :size="12" class="nav-group-arrow" :class="{ expanded: expandedGroups[group.name] }">
                <ArrowRight />
              </el-icon>
            </div>
            <div class="nav-group-items" v-show="expandedGroups[group.name]">
              <router-link
                v-for="item in group.items"
                :key="item.key"
                :to="item.path"
                class="nav-item sub-item"
                :class="{ active: $route.path === item.path }"
              >
                <span class="nav-label">{{ item.title }}</span>
              </router-link>
            </div>
          </template>
        </div>
      </nav>
      <div class="sidebar-toggle" @click="sidebarCollapsed = !sidebarCollapsed">
        <span>{{ sidebarCollapsed ? '展开' : '< 收起' }}</span>
      </div>
    </aside>
    <div class="content">
      <header class="topbar">
        <span class="greeting">你好，{{ realName || username }}</span>
        <el-space>
          <el-button color="#22d3ee" style="color: #0f172a; font-weight: 600" size="small" @click="$router.push('/bigscreen')">
            <el-icon><DataLine /></el-icon> 实时数据大屏
          </el-button>
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
import { computed, onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { HomeFilled, Bell, ArrowRight, Setting, User, DataBoard, Box, OfficeBuilding, Warning, Document, DataLine } from '@element-plus/icons-vue'
import { apiGet } from '../api'
import { useAuthStore } from '../store/auth'

const router = useRouter()
const authStore = useAuthStore()
const menus = ref([])
const sidebarCollapsed = ref(false)
const username = ref(localStorage.getItem('username') || '')
const realName = ref(localStorage.getItem('realName') || '')
const unreadCount = ref(0)

// 分组图标映射
const groupIcons = {
  '首页': HomeFilled,
  '系统管理': User,
  '基础数据': DataBoard,
  '仓储管理': Box,
  '业务操作': Document,
  '安全监控': Warning,
  '系统工具': Setting
}

// 分组展开状态 — 默认全部展开
const expandedGroups = reactive({})

const toggleGroup = (name) => {
  expandedGroups[name] = !expandedGroups[name]
}

// 将扁平菜单按 group 字段分组，保持原始顺序
const menuGroups = computed(() => {
  const map = new Map()
  for (const item of menus.value) {
    const g = item.group || '其他'
    if (!map.has(g)) map.set(g, [])
    map.get(g).push(item)
  }
  return Array.from(map, ([name, items]) => ({ name, items }))
})

const loadMenus = async () => {
  try {
    menus.value = await apiGet('/api/auth/menus')
    // 默认展开所有组
    for (const item of menus.value) {
      if (item.group && !(item.group in expandedGroups)) {
        expandedGroups[item.group] = true
      }
    }
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
.sidebar-logo {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 12px;
  height: 70px;
  cursor: pointer;
  border-bottom: 2px solid #e2e8f0;
}
.collapsed-logo {
  padding: 0;
}
.logo-shield {
  display: flex;
  align-items: center;
  justify-content: center;
}
.logo-text {
  display: flex;
  flex-direction: column;
  line-height: 1.2;
}
.nav-menu {
  flex: 1;
  overflow-y: auto;
  padding: 10px 0;
}
.nav-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 12px 20px;
  color: #475569;
  text-decoration: none;
  font-size: 14px;
  border-left: 4px solid transparent;
  transition: all .2s;
}
.nav-item.sub-item {
  padding-left: 48px;
  font-size: 13px;
  color: #64748b;
}
.nav-item:hover {
  background: #f8fafc;
  color: #0ea5e9;
}
.nav-item.active {
  color: #0ea5e9;
  background: #f0f9ff;
  border-left-color: #0ea5e9;
  font-weight: bold;
}
/* 分组头 */
.nav-group-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 12px 16px 12px 20px;
  cursor: pointer;
  user-select: none;
  color: #475569;
  font-size: 13px;
  font-weight: 600;
  background: #f8fafc;
  border-bottom: 1px solid #f1f5f9;
  border-top: 1px solid #f1f5f9;
  margin-top: 6px;
}
.nav-group-header:hover { color: #0ea5e9; }
.nav-group-left {
  display: flex;
  align-items: center;
  gap: 8px;
}
.nav-group-title { white-space: nowrap; }
.nav-group-arrow {
  transition: transform .2s ease;
}
.nav-group-arrow.expanded {
  transform: rotate(90deg);
}
.nav-group-items {
  overflow: hidden;
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
  overflow-y: auto;
  position: relative;
}
.topbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 24px;
  height: 56px;
  background: #fff;
  border-bottom: 1px solid var(--divider, #e8e8e8);
  flex-shrink: 0;
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
}
/* ===== 响应式 ===== */
@media (max-width: 768px) {
  .layout { grid-template-columns: 1fr; }
  .sidebar { display: none; }
}
</style>
