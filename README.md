# 校园物资智能管理系统

校园物资精细化、标准化管理方案，毕业设计全功能版实现（前后端分离）：

- **前端**：Vue3 + Element Plus + ECharts + Pinia + Vite
- **后端**：Spring Boot 3 + Spring Security + JWT + MyBatis-Plus
- **数据库**：MySQL 8
- **UI 风格**：深色模式玻璃拟态 (Glassmorphism) / 极简商务风

## 目录结构

- `backend`：后端服务 (基于 Spring Boot 3)
- `frontend`：前端管理端 (基于 Vue 3)
- `sql`：数据库建表与种子数据脚本

## 快速启动

### 1. 初始化数据库

1. 在 MySQL 8 环境下手动创建数据库：`CREATE DATABASE campus_material DEFAULT CHARACTER SET utf8mb4;`
2. 执行脚本：`sql/schema.sql` (结构定义)
3. 执行脚本：`sql/seed.sql` (演示数据)
4. 确认数据库连接配置：
   - 数据库名：`campus_material`
   - 配置文件：`backend/src/main/resources/application-dev.yml`

### 2. 启动后端

```bash
cd backend
mvn clean spring-boot:run
```

启动前请先在系统环境变量或 `.env` 中配置以下关键项：

- `DB_USERNAME`
- `DB_PASSWORD`
- `JWT_SECRET`（必须为至少 32 字节的高强度随机字符串）
- `CORS_ALLOWED_ORIGINS`（默认 `http://localhost:5173`，生产环境请改为实际前端域名）

- 后端接口：`http://127.0.0.1:8080`
- API 文档：`http://127.0.0.1:8080/swagger-ui/index.html`

### 3. 启动前端

```bash
cd frontend
npm install
npm run dev
```

- 后端地址：`http://127.0.0.1:5173`

## 核心功能模块

- **仪表盘/大数据屏**：实时库存概览、物资流转监控、事件警示。
- **仓储管理**：支持多校区、多仓库、多库位结构；支持入库、出库、移库及库存盘点。
- **业务申领**：完整的 申领 -> 审批 -> 出库 -> 签收 闭环流程，支持紧急领用。
- **物资调拨**：支持跨库、跨校区物资互助。
- **智能预警**：库存不足告警、积压告警、效期临期/过期提醒。
- **统计分析**：物资消耗趋势分析、部门领用排行、库存占比分布。
- **事件管理**：突发性物资需求登记与跟踪。
- **系统工具**：RBAC 权限控制、操作日志审计、消息通知。

## 演示账号

> **安全提示**：为保障系统安全，演示账号的初始密码已在 `application-dev.yml`、`seed.sql` 及 `.env.example` 中集中管理（默认 BCrypt 加密为 `Abc@123456`）。
> 生产环境部署前，请务必修改！

- 管理员：`admin`
- 仓库管理员：`warehouse`
- 审批人员：`approver`
- 部门用户：`dept`

## 技术亮点

- **FEFO (先到期先出)**：系统在出库推荐中采用先进先出/先到期先出逻辑。
- **响应式设计**：适配 PC 端与移动端显示。
- **高阶 UI**：采用自定义玻璃拟态设计，系统感官精致。

## 提交与验收建议

- 提交分组与文件清单请参考：`SUBMIT_CHECKLIST.md`
- 建议验收前执行：
   - `mvn -f backend/pom.xml test`
   - `cd frontend && npm run build`

---
*本项目原名为“校园应急物资智能管理系统”，现已升级为更通用的“校园物资智能管理系统”。*
