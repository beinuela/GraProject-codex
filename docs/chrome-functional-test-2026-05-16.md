# Chrome 功能测试记录（2026-05-16）

## 测试范围

- 测试方式：通过 `@chrome` 连接真实 Chrome 浏览器，对本地联调环境执行页面冒烟与核心流程操作。
- 前端地址：`http://127.0.0.1:5173`
- 后端地址：`http://127.0.0.1:8080`
- 测试账号：`user`、`dept`、`approver`、`warehouse`、`admin`

## 已覆盖内容

- 登录、登出、首页、指挥大屏
- 各角色菜单页逐页访问
- 核心动作验证：
  - `user` 新建申领成功
  - `dept` 上报事件成功
  - `warehouse` 新建调拨成功
  - `warehouse` 处理预警成功

## 问题清单

### 1. `USER` / `DEPT_USER` 登录首页后固定弹出 3 条“无权限访问该资源”，且首页关键指标被清零

- 严重级别：高
- 复现账号：`user`、`dept`
- 复现步骤：
  1. 使用 `user` 或 `dept` 登录系统
  2. 停留在 `/dashboard`
- 实际结果：
  - 页面连续弹出 3 条“无权限访问该资源”
  - 首页四个概览卡片显示为 `0`
- 预期结果：
  - 低权限角色首页不应主动请求无权限接口
  - 已授权的统计项应正常展示，不应因为部分接口无权限而整体归零
- 直接原因：
  - [frontend/src/views/DashboardView.vue](../frontend/src/views/DashboardView.vue) 在挂载时无角色分支地请求 `/api/inventory/list`、`/api/warning/list`、`/api/log/list`
  - 其中任一请求失败后，`Promise.all` 整体进入 `catch`，导致连已授权的 `materials`、`warehouses` 指标也被重置为 0
  - 全局 HTTP 拦截器会对每个 403 单独弹错
- 相关位置：
  - [frontend/src/views/DashboardView.vue](../frontend/src/views/DashboardView.vue)
  - [frontend/src/api/http.js](../frontend/src/api/http.js)
  - [backend/src/main/java/com/campus/material/modules/inventory/controller/InventoryController.java](../backend/src/main/java/com/campus/material/modules/inventory/controller/InventoryController.java)
  - [backend/src/main/java/com/campus/material/modules/warning/controller/WarningController.java](../backend/src/main/java/com/campus/material/modules/warning/controller/WarningController.java)
  - [backend/src/main/java/com/campus/material/modules/log/controller/OperationLogController.java](../backend/src/main/java/com/campus/material/modules/log/controller/OperationLogController.java)
- 日志佐证：
  - 后端日志多次记录 `userId=7`、`userId=3` 对 `/api/inventory/list`、`/api/warning/list`、`/api/log/list` 的 403

### 2. 前端路由允许 `USER` 进入事件管理页，但后端明确拒绝，导致直接访问时报 403

- 严重级别：高
- 复现账号：`user`
- 复现步骤：
  1. 使用 `user` 登录系统
  2. 直接访问 `/event/list`
- 实际结果：
  - 页面进入“事件管理”路由
  - 随后弹出“无权限访问该资源”
- 预期结果：
  - 前端路由与后端权限应保持一致
  - 要么前端直接拦截 `USER`，要么后端放行该角色
- 直接原因：
  - [frontend/src/router/index.js](../frontend/src/router/index.js) 将 `/event/list` 绑定到 `roles.analytics`，其中包含 `USER`
  - [backend/src/main/java/com/campus/material/modules/event/controller/EventController.java](../backend/src/main/java/com/campus/material/modules/event/controller/EventController.java) 的 `list/detail/create` 仅允许 `ADMIN`、`WAREHOUSE_ADMIN`、`APPROVER`、`DEPT_USER`

### 3. 物资档案页权限模型不一致，低权限角色进入后会触发额外 403，页面能力与后端接口不匹配

- 严重级别：高
- 影响账号：`user`、`dept`、`approver`
- 复现现象：
  - `user` 直接访问 `/material/info` 时弹出“无权限访问该资源”
  - `dept` 直接访问 `/material/info` 时弹出“无权限访问该资源”
  - `approver` 访问 `/material/info` 时页面可见，但后端日志仍记录 `/api/material/category` 403
- 预期结果：
  - 若页面允许这些角色访问，就不应在挂载阶段请求其无权访问的分类接口
  - 若这些角色仅可只读浏览，页面上的新增/编辑/删除能力也应同步降级
- 直接原因：
  - [frontend/src/router/index.js](../frontend/src/router/index.js) 允许 `roles.analytics` 访问 `/material/info`，其中包含 `USER`、`DEPT_USER`、`APPROVER`
  - [frontend/src/views/material/MaterialView.vue](../frontend/src/views/material/MaterialView.vue) 挂载时无条件请求 `/api/material/category`
  - [backend/src/main/java/com/campus/material/modules/material/controller/MaterialController.java](../backend/src/main/java/com/campus/material/modules/material/controller/MaterialController.java) 中：
    - `/api/material/info` 对上述角色放行
    - `/api/material/category` 仅对 `ADMIN`、`WAREHOUSE_ADMIN`、`PURCHASER` 放行
    - `/api/material/info` 的保存、删除也不对 `USER`、`DEPT_USER`、`APPROVER` 放行

### 4. 部分功能实际可访问，但菜单未提供入口，导致角色能力与导航不一致

- 严重级别：中
- 已实测现象：
  - `dept` 可直接访问 `/event/list`，并成功完成“上报事件”，但左侧菜单没有“事件管理”
  - `approver` 可直接访问 `/event/list`，但左侧菜单没有“事件管理”
  - `approver` 可直接访问 `/notification/list`，但左侧菜单没有“消息通知”
- 预期结果：
  - 若角色有功能权限，菜单应提供正常入口
  - 若该页面不想开放给该角色，则前端路由与后端接口都应收紧
- 相关位置：
  - [backend/src/main/java/com/campus/material/modules/auth/AuthService.java](../backend/src/main/java/com/campus/material/modules/auth/AuthService.java)
  - [frontend/src/router/index.js](../frontend/src/router/index.js)

## 本轮未发现问题的区域

- `warehouse` 的菜单页冒烟整体通过
- `admin` 的菜单页冒烟整体通过
- `user` 申领新建成功
- `dept` 事件上报成功
- `warehouse` 调拨新建成功
- `warehouse` 预警处理成功

## 建议修复顺序

1. 先修首页聚合请求的角色分支与降级逻辑，消除 `user/dept` 登录即报错的问题。
2. 统一前端路由、菜单和后端接口的权限矩阵，先处理 `/event/list` 与 `/material/info`。
3. 按角色补齐菜单入口或收紧路由，避免“能直达但没有入口”。
