import { createRouter, createWebHistory } from 'vue-router'
import { useAuthStore } from '../store/auth'

const routes = [
  { path: '/login', component: () => import('../views/LoginView.vue') },
  { path: '/bigscreen', component: () => import('../views/bigscreen/BigScreenView.vue'), meta: { requiresAuth: true } },
  {
    path: '/',
    component: () => import('../layouts/MainLayout.vue'),
    redirect: '/dashboard',
    children: [
      { path: 'dashboard', component: () => import('../views/DashboardView.vue') },
      { path: 'rbac/users', component: () => import('../views/rbac/UserView.vue') },
      { path: 'rbac/depts', component: () => import('../views/rbac/DeptView.vue') },
      { path: 'campus/list', component: () => import('../views/campus/CampusView.vue') },
      { path: 'material/category', component: () => import('../views/material/CategoryView.vue') },
      { path: 'material/info', component: () => import('../views/material/MaterialView.vue') },
      { path: 'supplier/list', component: () => import('../views/supplier/SupplierView.vue') },
      { path: 'warehouse/list', component: () => import('../views/warehouse/WarehouseView.vue') },
      { path: 'warehouse/location', component: () => import('../views/campus/StorageLocationView.vue') },
      { path: 'inventory/list', component: () => import('../views/inventory/InventoryView.vue') },
      { path: 'inventory/stock-in', component: () => import('../views/inventory/StockInView.vue') },
      { path: 'inventory/stock-out', component: () => import('../views/inventory/StockOutView.vue') },
      { path: 'apply/list', component: () => import('../views/apply/ApplyView.vue') },
      { path: 'transfer/list', component: () => import('../views/transfer/TransferView.vue') },
      { path: 'warning/list', component: () => import('../views/warning/WarningView.vue') },
      { path: 'event/list', component: () => import('../views/event/EmergencyEventView.vue') },
      { path: 'analytics/charts', component: () => import('../views/analytics/AnalyticsView.vue') },
      { path: 'log/operation', component: () => import('../views/log/OperationLogView.vue') },
      { path: 'log/login', component: () => import('../views/log/LoginLogView.vue') },
      { path: 'notification/list', component: () => import('../views/notification/NotificationView.vue') },
      { path: 'config/list', component: () => import('../views/config/SystemConfigView.vue') },
      { path: 'security/policy', component: () => import('../views/security/SecurityPolicyView.vue') }
    ]
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

router.beforeEach(async (to) => {
  const auth = useAuthStore()
  if (to.path === '/login') {
    if (auth.token) {
      return '/dashboard'
    }
    return true
  }

  if (!auth.token) {
    return '/login'
  }

  if (!auth.user) {
    try {
      await auth.loadProfile()
    } catch (_) {
      auth.logout()
      return '/login'
    }
  }

  if (to.path === '/security/policy' && auth.user?.roleCode !== 'ADMIN') {
    return '/dashboard'
  }

  return true
})

export default router
