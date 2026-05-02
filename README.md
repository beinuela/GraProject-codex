# 校园物资智能管理系统

前后端分离的校园物资管理系统，覆盖认证、申领、审批、库存、调拨、预警、日志、通知与观测能力。

- 前端：Vue 3 + Vite + Element Plus + Pinia + ECharts
- 后端：Spring Boot 3 + Spring Security + JWT + MyBatis-Plus
- 数据库：MySQL 8 / H2（截图与 E2E）
- 监控：Actuator + Prometheus + Grafana + Loki + Sentry

## 0. 当前交付状态

截至 `2026-04-21`，本轮工程化整改在仓库内已完成：

- 文档补齐：`README`、`DEPLOY`、`CONTRIBUTING`、架构、版本策略、可观测性说明
- 安全加固：环境变量化、JWT 启动校验、限流、安全头
- 可测性补齐：后端单测与集成测试、前端单测、Playwright E2E、k6 脚本
- 性能与接口治理：高增长列表分页化、查询热点优化、统一 `PageQuery` / `PageResult<T>`
- 可观测性接入：Actuator、Prometheus 指标、JSON 日志、Loki/Promtail、Sentry 降级接入

本地已验证通过：

- `cd backend && mvn test`，共 `48` 个测试通过
- `cd frontend && npm run build`
- `cd frontend && npm run test:unit`
- `cd frontend && npm run test:e2e`
- `k6` 冒烟脚本已覆盖登录、库存分页、预警分页、操作日志分页

仍需人工准备的外部前置条件：

- 首次执行 Playwright 需安装浏览器：`cd frontend && npx playwright install chromium`
- 首次执行 k6 需安装客户端；Windows 可执行 `winget install -e --id GrafanaLabs.k6`
- 若要完整联调观测栈，需要先启动 Docker Desktop
- 若要验证真实 Sentry 上报，需要提供 `SENTRY_DSN` 与 `VITE_SENTRY_DSN`

## 1. 先看这些文档

- [架构设计](./docs/architecture.md)
- [API 版本控制策略](./docs/api-versioning.md)
- [可观测性说明](./docs/observability.md)
- [性能基线记录](./docs/performance-baseline.md)
- [部署说明](./DEPLOY.md)
- [开发者贡献指南](./CONTRIBUTING.md)

## 2. 目录结构

- `backend/`：Spring Boot 后端服务与测试
- `frontend/`：Vue 管理端、单测与 Playwright E2E
- `sql/`：MySQL 初始化脚本与种子数据
- `docs/`：架构、版本、观测、部署文档
- `tests/performance/`：k6 性能脚本

## 3. 快速启动

### 3.1 准备环境变量

1. 复制根目录 `.env.example` 为 `.env`
2. 至少设置：
   - `SPRING_PROFILES_ACTIVE`
   - `JWT_SECRET`
   - 正式运行必须设置 `DB_URL` / `DB_USERNAME` / `DB_PASSWORD`

PowerShell 示例：

```powershell
Copy-Item .env.example .env
```

### 3.2 启动模式

常用模式：

- `prod`：正式运行环境，连接真实 MySQL
- `dev` / `mysql`：开发联调环境，连接真实 MySQL
- `screenshot`：使用 H2 演示数据，仅适合截图和 E2E

### 3.3 启动后端

```bash
cd backend
mvn spring-boot:run
```

默认业务接口：

- API：`http://127.0.0.1:8080`
- Swagger：`http://127.0.0.1:8080/swagger-ui/index.html`
- 管理端点：`http://127.0.0.1:18080/actuator`

### 3.4 启动前端

```bash
cd frontend
npm install
npm run dev
```

默认前端地址：

- `http://127.0.0.1:5173`

## 4. 初始化 MySQL

`prod` / `dev` / `mysql` profile 需要：

```sql
CREATE DATABASE IF NOT EXISTS campus_material
  DEFAULT CHARACTER SET utf8mb4
  COLLATE utf8mb4_general_ci;
```

```bash
mysql -u <user> -p campus_material < sql/schema.sql
mysql -u <user> -p campus_material < sql/seed.sql
```

## 5. 核心功能

- 仪表盘与大屏：库存、预警、日志、趋势总览
- 仓储管理：库存查询、入库、出库、库位与仓库管理
- 申领闭环：草稿、提交、审批、出库、签收
- 调拨闭环：创建、提交、审批、执行、签收
- 智能预警：低库存、积压、临期、过期、异常出库
- 安全与审计：JWT、限流、安全头、登录日志、操作日志
- 通知与配置：站内通知、系统配置、观测接入

## 6. 演示账号

`screenshot` / E2E 数据集内置以下测试账号，密码均为 `Abc@123456`，仅用于本地联调：

- 管理员：`admin`
- 仓库管理员：`warehouse`
- 审批人员：`approver`
- 部门用户：`dept`

生产环境必须替换测试密码与所有密钥。

## 7. 常用验证命令

```bash
cd backend && mvn test
cd backend && mvn -Dtest=CoreFlowIntegrationTest test
cd frontend && npm run test:unit
cd frontend && npm run build
cd frontend && npm run test:e2e
```

安装 k6 后可执行：

```bash
k6 run tests/performance/login.js
k6 run tests/performance/inventory-list.js
k6 run tests/performance/warning-list.js
k6 run tests/performance/operation-log-list.js
```

Windows 若当前终端尚未刷新 `PATH`，可直接使用：

```powershell
& 'C:\Program Files\k6\k6.exe' run tests/performance/login.js
```
