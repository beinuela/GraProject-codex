# 校园应急物资智能管理系统

毕业设计全功能版实现（前后端分离）：
- 前端：Vue3 + Element Plus + ECharts + Pinia + Vite
- 后端：Spring Boot 3 + Spring Security + JWT + MyBatis-Plus
- 数据库：MySQL 8

## 目录结构

- `backend`：后端服务
- `frontend`：前端管理端
- `sql`：建表与演示数据脚本

## 快速启动

### 1. 初始化数据库

1. 在 MySQL 8 执行：`sql/schema.sql`
2. 再执行：`sql/seed.sql`
3. 确认数据库连接（默认）：
   - 数据库：`campus_emergency`
   - 用户名：`root`
   - 密码：`root`

如需修改，请编辑：`backend/src/main/resources/application.yml`

### 2. 启动后端

```bash
cd backend
mvn spring-boot:run
```

后端地址：`http://127.0.0.1:8080`

API 文档：
- Knife4j: `http://127.0.0.1:8080/doc.html`
- OpenAPI: `http://127.0.0.1:8080/swagger-ui/index.html`

### 3. 启动前端

```bash
cd frontend
npm install
npm run dev
```

前端地址：`http://127.0.0.1:5173`

## 演示账号

- 管理员：`admin / 123456`
- 仓库管理员：`warehouse / 123456`
- 部门用户：`dept / 123456`
- 审批人员：`approver / 123456`

## 一键答辩演示流程（建议）

1. 使用 `dept` 登录，创建普通申请并提交。
2. 使用 `approver` 登录，对申请审批通过。
3. 使用 `warehouse` 登录，在出库管理按 FEFO 执行出库（关联申请ID）。
4. 使用 `dept` 登录，执行签收。
5. 在预警中心触发扫描并处理预警。
6. 打开首页仪表盘与数据分析页，展示图表、预测与补货建议。

## 已实现能力

- 登录认证、JWT 鉴权、角色菜单控制
- 用户/角色/部门、物资分类/物资、仓库管理
- 入库、出库、库存查询、批次管理、盘点
- 申请审批（普通/紧急快速审批）、签收闭环
- 调拨申请、审批、执行、确认
- 预警扫描（不足/积压/临期/过期/异常领用）
- 统计分析图表与移动平均预测、补货建议

## 说明

- 默认密码允许明文种子数据登录（便于演示），新增/编辑用户密码将自动 BCrypt 存储。
- 当前版本为单体架构，适配毕业设计答辩场景。
