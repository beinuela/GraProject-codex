import { createRouter, createWebHistory } from 'vue-router'
import { useAuthStore } from '../store/auth'

const meta = (title, section, description, pageType = 'standard', extra = {}) => ({
  title,
  section,
  description,
  pageType,
  ...extra
})

const routes = [
  {
    path: '/login',
    component: () => import('../views/LoginView.vue'),
    meta: meta('系统登录', '系统入口', '校验身份并进入校园物资智能管理系统。', 'auth')
  },
  {
    path: '/bigscreen',
    component: () => import('../views/bigscreen/BigScreenView.vue'),
    meta: meta('指挥大屏', '数据中枢', '查看全局库存、事件与预警的实时态势。', 'screen', { requiresAuth: true })
  },
  {
    path: '/',
    component: () => import('../layouts/MainLayout.vue'),
    redirect: '/dashboard',
    children: [
      { path: 'dashboard', component: () => import('../views/DashboardView.vue'), meta: meta('系统首页', '首页概览', '汇总关键指标、近期动态与核心图表的首页概览。', 'dashboard') },
      { path: 'rbac/users', component: () => import('../views/rbac/UserView.vue'), meta: meta('用户管理', '系统管理', '维护账号、角色关联与启停状态。') },
      { path: 'rbac/roles', component: () => import('../views/rbac/RoleView.vue'), meta: meta('角色管理', '系统管理', '维护角色编码、名称和说明信息。') },
      { path: 'rbac/depts', component: () => import('../views/rbac/DeptView.vue'), meta: meta('部门管理', '系统管理', '维护组织单元与业务归属信息。') },
      { path: 'campus/list', component: () => import('../views/campus/CampusView.vue'), meta: meta('校区管理', '基础数据', '维护校区、地址与联络信息。') },
      { path: 'material/category', component: () => import('../views/material/CategoryView.vue'), meta: meta('分类管理', '基础数据', '维护物资分类编码与分类说明。') },
      { path: 'material/info', component: () => import('../views/material/MaterialView.vue'), meta: meta('物资档案', '基础数据', '维护物资编码、单位和安全库存阈值。') },
      { path: 'supplier/list', component: () => import('../views/supplier/SupplierView.vue'), meta: meta('供应商管理', '基础数据', '管理供应商联系人、供应范围与协作资料。') },
      { path: 'warehouse/list', component: () => import('../views/warehouse/WarehouseView.vue'), meta: meta('仓库管理', '基础数据', '维护仓库编码、负责人和校区归属。') },
      { path: 'warehouse/location', component: () => import('../views/campus/StorageLocationView.vue'), meta: meta('库位管理', '基础数据', '查看库位容量、占用和启停情况。') },
      { path: 'inventory/list', component: () => import('../views/inventory/InventoryView.vue'), meta: meta('库存查询', '仓储管理', '查询当前库存、锁定量和批次情况。') },
      { path: 'inventory/stock-in', component: () => import('../views/inventory/StockInView.vue'), meta: meta('入库管理', '仓储管理', '登记入库单、批次与生产有效期信息。') },
      { path: 'inventory/stock-out', component: () => import('../views/inventory/StockOutView.vue'), meta: meta('出库管理', '仓储管理', '查看出库记录与申领关联情况。') },
      { path: 'apply/list', component: () => import('../views/apply/ApplyView.vue'), meta: meta('申领审批', '业务流程', '追踪申领单状态、审批动作与流转轨迹。', 'workflow') },
      { path: 'transfer/list', component: () => import('../views/transfer/TransferView.vue'), meta: meta('调拨管理', '业务流程', '管理跨仓调拨申请、推荐与执行状态。', 'workflow') },
      { path: 'warning/list', component: () => import('../views/warning/WarningView.vue'), meta: meta('预警中心', '安全监控', '处理库存、效期和异常消耗类预警。', 'monitor') },
      { path: 'event/list', component: () => import('../views/event/EventView.vue'), meta: meta('事件管理', '安全监控', '上报突发事件并跟踪处理闭环。', 'workflow') },
      { path: 'analytics/charts', component: () => import('../views/analytics/AnalyticsView.vue'), meta: meta('统计分析', '数据分析', '按主题查看库存、仓库、部门与效期分析图表。', 'analytics') },
      { path: 'log/operation', component: () => import('../views/log/OperationLogView.vue'), meta: meta('操作日志', '系统工具', '检索模块操作记录与行为细节。') },
      { path: 'log/login', component: () => import('../views/log/LoginLogView.vue'), meta: meta('登录日志', '系统工具', '查看登录时间、IP 与状态记录。') },
      { path: 'notification/list', component: () => import('../views/notification/NotificationView.vue'), meta: meta('通知中心', '系统工具', '统一查看系统通知、广播与未读状态。') },
      { path: 'config/list', component: () => import('../views/config/SystemConfigView.vue'), meta: meta('系统配置', '系统工具', '维护系统运行参数与配置分组。') },
      { path: 'security/policy', component: () => import('../views/security/SecurityPolicyView.vue'), meta: meta('安全策略', '系统工具', '查看 token 策略并执行管理员安全清理。', 'workflow') }
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
