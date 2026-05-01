# 开发者贡献指南

本文档约束本仓库的本地开发、测试、提交流程和回归标准。当前工程以“单机内网部署友好”为目标，不引入 Kubernetes、Redis 限流或分布式链路追踪。

## 0. 当前基线

截至 `2026-04-21`，当前仓库已验证：

- `cd backend && mvn test` 通过，共 `48` 个测试
- `cd frontend && npm run build`
- `cd frontend && npm run test:unit`
- `cd frontend && npm run test:e2e`
- `k6` 冒烟脚本已验证登录、库存分页、预警分页、操作日志分页

仍需人工准备的环境前置条件：

- 首次执行 E2E 前安装 Chromium：`cd frontend && npx playwright install chromium`
- 首次执行 k6 前安装客户端；Windows 可执行 `winget install -e --id GrafanaLabs.k6`
- 完整观测联调需要可用的 Docker daemon；真实 Sentry 上报需要 DSN

## 1. 环境准备

- JDK `17`
- Maven `3.8+`
- Node.js `18+`
- npm `9+`
- MySQL `8.0+`，或使用仓库内置的 H2 `screenshot` 数据集
- Playwright 浏览器依赖：首次执行 E2E 前运行 `cd frontend && npx playwright install chromium`
- k6：仅性能脚本执行需要，未安装时不影响常规开发

推荐先复制根目录的 [`.env.example`](./.env.example) 为 `.env`，所有启动方式都通过环境变量驱动，不修改源码配置文件。

## 2. 目录约定

- `backend/`：Spring Boot 后端服务、认证、安全、业务模块、集成测试
- `frontend/`：Vue 3 管理端、单测、Playwright E2E
- `sql/`：MySQL 初始化脚本与种子数据
- `docs/`：架构、版本策略、观测与部署文档
- `tests/performance/`：k6 性能脚本

## 3. 分支与提交约定

- 分支命名建议使用 `codex/<topic>`、`feature/<topic>`、`fix/<topic>`
- Commit 建议使用 `type(scope): summary`
- 常用 `type`：`feat`、`fix`、`docs`、`test`、`refactor`、`perf`、`chore`
- 单个提交尽量保持单一目的，不把文档、依赖升级和功能改造混在一起

示例：

```text
feat(inventory): add paged stock-out list
fix(auth): validate jwt secret on startup
docs(observability): add loki and sentry setup guide
```

## 4. 本地开发流程

1. 复制 `.env.example` 为 `.env`，填入数据库、JWT、Sentry 等变量。
2. 选择后端 profile：
   - `screenshot`：本地 H2 演示数据，适合快速联调和 E2E
   - `mysql`：连接真实 MySQL
3. 启动后端：

```bash
cd backend
mvn spring-boot:run
```

4. 启动前端：

```bash
cd frontend
npm install
npm run dev
```

## 5. 测试矩阵

### 后端

- 单元测试：

```bash
cd backend
mvn test
```

- 仅核心集成测试：

```bash
cd backend
mvn -Dtest=CoreFlowIntegrationTest test
```

### 前端

- 单元测试：

```bash
cd frontend
npm run test:unit
```

- 覆盖率：

```bash
cd frontend
npm run test:coverage
```

- 生产构建：

```bash
cd frontend
npm run build
```

- E2E 冒烟：

```bash
cd frontend
npm run test:e2e
```

### 性能脚本

安装 k6 后执行：

```bash
k6 run tests/performance/login.js
k6 run tests/performance/inventory-list.js
k6 run tests/performance/warning-list.js
k6 run tests/performance/operation-log-list.js
```

Windows 可安装：

```powershell
winget install -e --id GrafanaLabs.k6
```

若当前终端尚未刷新 `PATH`，可直接使用：

```powershell
& 'C:\Program Files\k6\k6.exe' run tests/performance/login.js
```

## 6. 环境变量约定

核心变量分组如下，完整示例见根目录 [`.env.example`](./.env.example)：

- 数据库：`DB_URL`、`DB_USERNAME`、`DB_PASSWORD`
- JWT：`JWT_SECRET`、`JWT_ACCESS_EXPIRE_MINUTES`、`JWT_REFRESH_EXPIRE_DAYS`
- 限流：`RATE_LIMIT_*`
- CORS：`CORS_ALLOWED_ORIGINS`
- 文档与日志：`KNIFE4J_ENABLE`、`SPRINGDOC_*`、`APP_LOG_DIR`
- 观测：`MANAGEMENT_SERVER_*`、`SENTRY_*`、`VITE_SENTRY_*`

规则：

- 非测试环境必须提供 `JWT_SECRET`，且长度不少于 32 字节
- 不在源码中写入真实数据库账号、密码、DSN 或可预测的 JWT 默认值
- 前端和后端的 Sentry 变量分离：后端用 `SENTRY_*`，前端用 `VITE_SENTRY_*`

## 7. 代码风格与实现约束

- 保持现有前后端目录结构，不做与需求无关的大规模重构
- 列表接口统一使用 `PageQuery` / `PageResult<T>`
- 高复杂度业务路径补“解释业务规则”的注释，不写低价值注释
- 所有敏感配置通过环境变量读取
- 普通读接口不加统一硬限流，高风险写接口和认证接口才做限流
- 纯 API 响应层的安全头由 Spring Boot 输出；HSTS 和前端静态资源 CSP 在 Nginx/HTTPS 终止层配置

## 8. 提交流程

1. 从最新主线创建功能分支。
2. 先完成代码与测试，再更新相关文档。
3. 提交前至少执行：
   - `mvn test`
   - `npm run test:unit`
   - `npm run build`
4. 涉及主链路改动时，再执行：
   - `npm run test:e2e`
   - 相关 `k6` 冒烟脚本
5. 提交描述中写清楚：
   - 改动目标
   - 风险点
   - 验证命令

## 9. 提交前回归检查清单

- 代码中不存在真实数据库凭据、生产 DSN 或弱 JWT 默认值
- 新增列表接口返回 `records / total / page / size`
- 前端分页保留筛选条件，空态和异常态不回退
- 登录、刷新和高风险接口限流返回一致错误码
- API 响应可见 `X-Content-Type-Options`、`X-Frame-Options`、`Referrer-Policy`、`Permissions-Policy`
- Actuator/Prometheus 只绑定本机管理地址，不直接暴露公网
- 文档中的命令与当前仓库脚本一致

## 10. 常见问题

### 1. 后端启动直接失败，提示 JWT 密钥无效

- 检查 `.env` 中是否设置了 `JWT_SECRET`
- 确认长度不少于 32 字节

### 2. 登录总是 401 或 429

- 401：优先检查演示数据是否已导入、密码是否正确
- 429：检查 `RATE_LIMIT_*` 配置是否过小，或同一 IP/用户名是否被连续重试

### 3. Playwright 无法执行

- 先确认 `npx` 可用
- 然后执行 `cd frontend && npx playwright install chromium`
- `npm run test:e2e` 会自动拉起 `screenshot` profile 后端、使用独立 H2 数据文件，并在 Windows 上清理子进程树

### 4. Prometheus 抓不到指标

- 检查 `MANAGEMENT_SERVER_PORT` 是否启动
- 确认只在本机地址暴露，例如 `127.0.0.1:18080`
