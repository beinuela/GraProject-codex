<template>
  <div class="app-shell">
    <transition name="shell-fade">
      <div v-if="isMobile && mobileMenuOpen" class="shell-scrim" @click="mobileMenuOpen = false" />
    </transition>

    <aside
      class="shell-sidebar"
      :class="{
        'shell-sidebar--collapsed': sidebarCollapsed && !isMobile,
        'shell-sidebar--mobile': isMobile,
        'shell-sidebar--open': mobileMenuOpen
      }"
    >
      <div class="shell-brand" @click="$router.push('/dashboard')">
        <div class="shell-brand__mark">
          <span>CM</span>
        </div>
        <div v-if="!sidebarCollapsed || isMobile" class="shell-brand__text">
          <strong>校园物资智控台</strong>
          <span>Campus Material Operations</span>
        </div>
      </div>

      <div class="shell-sidebar__scroll">
        <section v-for="group in menuGroups" :key="group.name" class="shell-group">
          <button class="shell-group__header" type="button" @click="toggleGroup(group.name)">
            <span class="shell-group__title-wrap">
              <el-icon><component :is="groupIcons[group.name] || Setting" /></el-icon>
              <span v-if="!sidebarCollapsed || isMobile" class="shell-group__title">{{ group.name }}</span>
            </span>
            <el-icon v-if="(!sidebarCollapsed || isMobile) && group.name !== '首页'" class="shell-group__arrow" :class="{ 'shell-group__arrow--open': expandedGroups[group.name] }">
              <ArrowRight />
            </el-icon>
          </button>

          <div v-show="group.name === '首页' || expandedGroups[group.name]" class="shell-group__items">
            <router-link
              v-for="item in group.items"
              :key="item.key"
              :to="item.path"
              class="shell-nav-item"
              :class="{ 'shell-nav-item--active': $route.path === item.path }"
              :title="item.title"
              @click="handleNavClick"
            >
              <span class="shell-nav-item__marker">{{ item.title.slice(0, 1) }}</span>
              <span v-if="!sidebarCollapsed || isMobile" class="shell-nav-item__label">{{ item.title }}</span>
            </router-link>
          </div>
        </section>
      </div>

      <div class="shell-sidebar__footer">
        <div v-if="!sidebarCollapsed || isMobile" class="shell-sidebar__card">
          <span class="shell-sidebar__card-label">当前身份</span>
          <strong>{{ currentRole }}</strong>
          <span>{{ currentSection }}</span>
        </div>
        <button v-if="!isMobile" class="shell-collapse" type="button" @click="sidebarCollapsed = !sidebarCollapsed">
          <el-icon><Expand v-if="sidebarCollapsed" /><Fold v-else /></el-icon>
          <span v-if="!sidebarCollapsed">收起导航</span>
        </button>
      </div>
    </aside>

    <div class="shell-main">
      <header class="shell-topbar">
        <div class="shell-topbar__left">
          <button class="shell-burger" type="button" @click="mobileMenuOpen = !mobileMenuOpen">
            <el-icon><Operation /></el-icon>
          </button>
          <div class="shell-context">
            <div class="shell-context__capsules">
              <span class="shell-context__section">{{ currentSection }}</span>
              <span class="shell-context__type">{{ currentTypeLabel }}</span>
            </div>
            <strong class="shell-context__title">{{ currentTitle }}</strong>
            <p class="shell-context__description">{{ currentDescription }}</p>
          </div>
        </div>

        <div class="shell-topbar__right">
          <div class="shell-live">
            <span class="shell-live__label">系统时间</span>
            <strong class="shell-live__value">{{ currentTime }}</strong>
          </div>
          <el-button class="shell-ghost" @click="$router.push('/bigscreen')">
            <el-icon><DataLine /></el-icon>
            指挥大屏
          </el-button>
          <el-button class="shell-icon-button" circle @click="$router.push('/notification/list')">
            <el-badge :value="unreadCount" :hidden="!unreadCount" :max="99">
              <el-icon><Bell /></el-icon>
            </el-badge>
          </el-button>
          <el-dropdown @command="handleCommand">
            <div class="shell-user">
              <el-avatar :size="38">{{ displayName.slice(0, 1) }}</el-avatar>
              <div class="shell-user__meta">
                <strong>{{ displayName }}</strong>
                <span>{{ currentRole }}</span>
              </div>
            </div>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item command="logout">退出登录</el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </div>
      </header>

      <main class="shell-content">
        <router-view />
      </main>
    </div>
  </div>
</template>

<script setup>
import { computed, onBeforeUnmount, onMounted, reactive, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import {
  ArrowRight,
  Bell,
  DataBoard,
  DataLine,
  Document,
  Expand,
  Fold,
  HomeFilled,
  Operation,
  Setting,
  User,
  Warning,
  Box
} from '@element-plus/icons-vue'
import { apiGet } from '../api'
import { useAuthStore } from '../store/auth'

const router = useRouter()
const route = useRoute()
const authStore = useAuthStore()
const menus = ref([])
const unreadCount = ref(0)
const sidebarCollapsed = ref(false)
const mobileMenuOpen = ref(false)
const isMobile = ref(false)
const currentTime = ref('')
const expandedGroups = reactive({})

let clockTimer = null

const groupIcons = {
  首页: HomeFilled,
  系统管理: User,
  基础数据: DataBoard,
  仓储管理: Box,
  业务操作: Document,
  业务流程: Document,
  安全监控: Warning,
  系统工具: Setting
}

const pageTypeMap = {
  dashboard: '数据看板',
  analytics: '分析视图',
  workflow: '流程页面',
  monitor: '监控页面',
  screen: '大屏视图',
  auth: '认证页面',
  standard: '业务页面'
}

const menuGroups = computed(() => {
  const map = new Map()
  for (const item of menus.value) {
    const name = item.group || '其他'
    if (!map.has(name)) map.set(name, [])
    map.get(name).push(item)
  }
  return Array.from(map, ([name, items]) => ({ name, items }))
})

const currentTitle = computed(() => route.meta.title || '校园物资系统')
const currentSection = computed(() => route.meta.section || '业务页面')
const currentDescription = computed(() => route.meta.description || '统一管理校园物资、库存、事件与安全流程。')
const currentTypeLabel = computed(() => pageTypeMap[route.meta.pageType] || pageTypeMap.standard)
const displayName = computed(() => authStore.user?.realName || localStorage.getItem('realName') || authStore.user?.username || localStorage.getItem('username') || '访客')
const currentRole = computed(() => authStore.user?.roleName || authStore.user?.roleCode || '系统用户')

const syncViewport = () => {
  isMobile.value = window.innerWidth < 1024
  if (!isMobile.value) {
    mobileMenuOpen.value = false
  }
}

const syncClock = () => {
  currentTime.value = new Date().toLocaleString('zh-CN', {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit',
    second: '2-digit'
  })
}

const toggleGroup = (name) => {
  if (name === '首页') return
  expandedGroups[name] = !expandedGroups[name]
}

const loadMenus = async () => {
  try {
    menus.value = await apiGet('/api/auth/menus')
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
    unreadCount.value = await apiGet('/api/notification/unread-count')
  } catch {
    unreadCount.value = 0
  }
}

const handleNavClick = () => {
  if (isMobile.value) {
    mobileMenuOpen.value = false
  }
}

const handleCommand = (command) => {
  if (command === 'logout') {
    authStore.logout()
    router.push('/login')
  }
}

watch(() => route.path, async () => {
  handleNavClick()
  if (authStore.token) {
    await loadUnread()
  }
})

onMounted(async () => {
  syncViewport()
  syncClock()
  clockTimer = window.setInterval(syncClock, 1000)
  window.addEventListener('resize', syncViewport)
  window.addEventListener('notification-updated', loadUnread)
  await loadMenus()
  await loadUnread()
})

onBeforeUnmount(() => {
  if (clockTimer) {
    window.clearInterval(clockTimer)
  }
  window.removeEventListener('resize', syncViewport)
  window.removeEventListener('notification-updated', loadUnread)
})
</script>

<style scoped>
.app-shell {
  display: grid;
  grid-template-columns: auto minmax(0, 1fr);
  min-height: 100vh;
  gap: 22px;
  padding: 18px;
}

.shell-scrim {
  position: fixed;
  inset: 0;
  background: rgba(8, 17, 33, 0.36);
  backdrop-filter: blur(6px);
  z-index: 20;
}

.shell-sidebar {
  position: sticky;
  top: 18px;
  display: grid;
  grid-template-rows: auto minmax(0, 1fr) auto;
  width: 292px;
  max-height: calc(100vh - 36px);
  border-radius: 32px;
  padding: 16px;
  background:
    linear-gradient(180deg, rgba(10, 18, 33, 0.95), rgba(14, 23, 38, 0.92)),
    radial-gradient(circle at top left, rgba(53, 212, 198, 0.08), transparent 34%);
  color: rgba(238, 244, 255, 0.92);
  border: 1px solid rgba(112, 160, 255, 0.12);
  box-shadow: var(--shadow-sidebar);
  overflow: hidden;
  z-index: 30;
}

.shell-sidebar--collapsed {
  width: 92px;
}

.shell-sidebar--mobile {
  position: fixed;
  top: 14px;
  left: 14px;
  bottom: 14px;
  max-height: none;
  transform: translateX(calc(-100% - 30px));
  transition: transform var(--motion-base);
}

.shell-sidebar--mobile.shell-sidebar--open {
  transform: translateX(0);
}

.shell-brand {
  display: flex;
  align-items: center;
  gap: 14px;
  padding: 12px;
  border-radius: 24px;
  background: rgba(255, 255, 255, 0.04);
  cursor: pointer;
}

.shell-brand__mark {
  display: grid;
  place-items: center;
  width: 46px;
  height: 46px;
  border-radius: 16px;
  background: linear-gradient(135deg, rgba(38, 112, 233, 0.92), rgba(53, 212, 198, 0.78));
  color: #fff;
  font-family: var(--font-display);
  font-size: 17px;
  font-weight: 800;
}

.shell-brand__text {
  display: grid;
  gap: 4px;
}

.shell-brand__text strong {
  font-size: 16px;
}

.shell-brand__text span {
  color: rgba(195, 212, 235, 0.68);
  font-size: 12px;
}

.shell-sidebar__scroll {
  margin-top: 18px;
  overflow: auto;
  padding-right: 4px;
}

.shell-group {
  display: grid;
  gap: 8px;
  margin-bottom: 12px;
}

.shell-group__header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
  width: 100%;
  border: none;
  padding: 10px 12px;
  border-radius: 18px;
  background: transparent;
  color: inherit;
  cursor: pointer;
}

.shell-group__header:hover {
  background: rgba(255, 255, 255, 0.05);
}

.shell-group__title-wrap {
  display: flex;
  align-items: center;
  gap: 10px;
}

.shell-group__title {
  font-size: 13px;
  font-weight: 700;
  letter-spacing: 0.06em;
  text-transform: uppercase;
}

.shell-group__arrow {
  transition: transform var(--motion-fast);
}

.shell-group__arrow--open {
  transform: rotate(90deg);
}

.shell-group__items {
  display: grid;
  gap: 6px;
}

.shell-nav-item {
  display: grid;
  grid-template-columns: 28px minmax(0, 1fr);
  gap: 10px;
  align-items: center;
  padding: 12px;
  border-radius: 18px;
  color: rgba(222, 233, 250, 0.76);
  transition: background var(--motion-base), color var(--motion-base), transform var(--motion-base);
}

.shell-nav-item:hover {
  background: rgba(255, 255, 255, 0.06);
  color: #fff;
  transform: translateX(2px);
}

.shell-nav-item--active {
  background: linear-gradient(135deg, rgba(38, 112, 233, 0.24), rgba(53, 212, 198, 0.14));
  color: #fff;
  box-shadow: inset 0 0 0 1px rgba(135, 184, 255, 0.12);
}

.shell-nav-item__marker {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 28px;
  height: 28px;
  border-radius: 12px;
  background: rgba(255, 255, 255, 0.08);
  font-family: var(--font-display);
  font-size: 12px;
  font-weight: 700;
}

.shell-nav-item__label {
  min-width: 0;
  font-weight: 600;
}

.shell-sidebar__footer {
  display: grid;
  gap: 10px;
  margin-top: 14px;
}

.shell-sidebar__card {
  display: grid;
  gap: 4px;
  padding: 14px;
  border-radius: 20px;
  background: rgba(255, 255, 255, 0.05);
  border: 1px solid rgba(135, 184, 255, 0.1);
}

.shell-sidebar__card-label {
  color: rgba(195, 212, 235, 0.64);
  font-size: 12px;
  letter-spacing: 0.08em;
  text-transform: uppercase;
}

.shell-collapse {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  border: 1px solid rgba(135, 184, 255, 0.1);
  border-radius: 18px;
  min-height: 42px;
  background: rgba(255, 255, 255, 0.04);
  color: rgba(238, 244, 255, 0.9);
  cursor: pointer;
}

.shell-main {
  min-width: 0;
  display: grid;
  grid-template-rows: auto minmax(0, 1fr);
  gap: 18px;
}

.shell-topbar {
  position: sticky;
  top: 18px;
  z-index: 10;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 18px;
  padding: 16px 20px;
  border-radius: 28px;
  border: 1px solid var(--border-subtle);
  background: rgba(255, 255, 255, 0.72);
  backdrop-filter: blur(18px);
  box-shadow: var(--shadow-soft);
}

.shell-topbar__left,
.shell-topbar__right {
  display: flex;
  align-items: center;
  gap: 14px;
}

.shell-topbar__left {
  min-width: 0;
  flex: 1;
}

.shell-topbar__right {
  justify-content: flex-end;
  flex-wrap: wrap;
}

.shell-burger {
  display: none;
  border: none;
  width: 44px;
  height: 44px;
  border-radius: 14px;
  background: rgba(20, 54, 107, 0.06);
  color: var(--accent-primary-strong);
  cursor: pointer;
}

.shell-context {
  min-width: 0;
  display: grid;
  gap: 6px;
}

.shell-context__capsules {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
}

.shell-context__section,
.shell-context__type {
  display: inline-flex;
  align-items: center;
  min-height: 26px;
  padding: 0 10px;
  border-radius: 999px;
  font-size: 12px;
  font-weight: 700;
}

.shell-context__section {
  background: rgba(38, 112, 233, 0.08);
  color: var(--accent-primary-strong);
}

.shell-context__type {
  background: rgba(53, 212, 198, 0.1);
  color: var(--accent-teal-strong);
}

.shell-context__title {
  font-size: 20px;
  color: var(--text-primary);
}

.shell-context__description {
  margin: 0;
  color: var(--text-secondary);
  line-height: 1.5;
}

.shell-live {
  display: grid;
  gap: 4px;
  padding: 10px 12px;
  border-radius: 16px;
  background: rgba(20, 54, 107, 0.04);
}

.shell-live__label {
  font-size: 11px;
  letter-spacing: 0.08em;
  text-transform: uppercase;
  color: var(--text-tertiary);
}

.shell-live__value {
  font-family: var(--font-mono);
  font-size: 13px;
  color: var(--text-primary);
}

.shell-ghost {
  border-color: rgba(38, 112, 233, 0.12);
  background: rgba(255, 255, 255, 0.74);
}

.shell-icon-button {
  width: 42px;
  height: 42px;
}

.shell-user {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 6px 8px;
  border-radius: 18px;
  cursor: pointer;
  background: rgba(20, 54, 107, 0.04);
}

.shell-user__meta {
  display: grid;
  gap: 2px;
}

.shell-user__meta strong {
  font-size: 14px;
  color: var(--text-primary);
}

.shell-user__meta span {
  color: var(--text-tertiary);
  font-size: 12px;
}

.shell-content {
  min-width: 0;
  display: grid;
  gap: 18px;
}

.shell-fade-enter-active,
.shell-fade-leave-active {
  transition: opacity var(--motion-base);
}

.shell-fade-enter-from,
.shell-fade-leave-to {
  opacity: 0;
}

@media (max-width: 1280px) {
  .shell-live {
    display: none;
  }
}

@media (max-width: 1024px) {
  .app-shell {
    grid-template-columns: 1fr;
    gap: 14px;
    padding: 14px;
  }

  .shell-topbar {
    top: 14px;
  }

  .shell-burger {
    display: inline-flex;
    align-items: center;
    justify-content: center;
  }
}

@media (max-width: 768px) {
  .shell-topbar {
    padding: 14px;
    border-radius: 24px;
  }

  .shell-context__description,
  .shell-user__meta {
    display: none;
  }

  .shell-topbar__right {
    gap: 10px;
  }
}
</style>
