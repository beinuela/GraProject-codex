# 单机部署说明

本文档面向单机内网部署，不依赖 Kubernetes、Redis 或分布式链路追踪。

## 0. 当前交付状态

截至 `2026-04-21`，仓库侧已交付：

- 后端管理端口、Prometheus 指标、Logback JSON 日志、Sentry 降级接入
- `ops/docker-compose.observability.yml` 与对应 `Prometheus / Promtail / Grafana` 最小配置
- 安全加固与分页契约改造后的可部署版本

本机已完成的部署相关验收：

- 非测试环境弱 `JWT_SECRET` 启动失败
- 登录与刷新限流命中返回一致 `429`
- API 响应安全头可见
- Prometheus 可抓取 `campus_auth_login_total`
- Loki 可检索到 JSON 日志
- Grafana 已自动装载 Prometheus 与 Loki 数据源

仍需人工前置条件：

- 启动 Docker Desktop 或等价 Docker daemon
- 如需真实异常上报，提供 `SENTRY_DSN` / `VITE_SENTRY_DSN`
- 如需生产演练，仍需准备 MySQL、Nginx 和 HTTPS 证书

## 1. 部署目标

- 前端静态资源由 Nginx 提供
- 后端 Spring Boot 监听业务端口 `8080`
- Actuator/Prometheus 监听本机管理端口 `18080`
- 监控栈采用 `Prometheus + Grafana + Loki + Sentry`
- 所有敏感配置通过环境变量提供

## 2. 基础要求

- JDK `17+`
- Maven `3.8+`
- Node.js `18+`
- MySQL `8.0+`
- Nginx `1.22+`
- Docker / Docker Compose（若部署观测栈）

## 3. 环境变量

先复制 `.env.example` 为 `.env`，至少填写以下变量：

- `SPRING_PROFILES_ACTIVE=prod`
- `DB_URL`
- `DB_USERNAME`
- `DB_PASSWORD`
- `JWT_SECRET`
- `CORS_ALLOWED_ORIGINS`
- `APP_LOG_DIR`
- `MANAGEMENT_SERVER_ADDRESS=127.0.0.1`
- `MANAGEMENT_SERVER_PORT=18080`
- `SENTRY_DSN`（未配置时自动关闭）

## 4. 数据库初始化

```sql
CREATE DATABASE IF NOT EXISTS campus_material
  DEFAULT CHARACTER SET utf8mb4
  COLLATE utf8mb4_general_ci;
```

```bash
mysql -u <user> -p campus_material < sql/schema.sql
mysql -u <user> -p campus_material < sql/seed.sql
```

## 5. 后端部署

```bash
cd backend
mvn clean package
java -jar target/campus-backend-1.0.0.jar
```

建议使用 systemd 或 NSSM 等守护方式托管进程。关键约束：

- `JWT_SECRET` 未配置或长度不足 32 字节时，非测试环境会直接启动失败
- 管理端口只绑定本机地址，不对公网开放
- 日志默认输出到 `APP_LOG_DIR/campus-backend.log`

## 6. 前端部署

```bash
cd frontend
npm install
npm run build
```

将 `frontend/dist/` 部署到 Nginx 站点目录。

## 7. Nginx 反向代理与安全头

业务 API 安全头由应用层输出；`HSTS` 和前端静态资源 `CSP` 在 HTTPS 终止层配置。示例：

```nginx
server {
    listen 443 ssl http2;
    server_name campus-material.intranet;

    root /srv/campus-material/frontend/dist;
    index index.html;

    add_header Strict-Transport-Security "max-age=31536000; includeSubDomains" always;
    add_header Content-Security-Policy "default-src 'self'; connect-src 'self' https://sentry.example.com; img-src 'self' data:; style-src 'self' 'unsafe-inline'; script-src 'self'; font-src 'self' data:" always;

    location / {
        try_files $uri $uri/ /index.html;
    }

    location /api/ {
        proxy_pass http://127.0.0.1:8080;
        proxy_http_version 1.1;
        proxy_set_header Host $host;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto https;
        proxy_set_header X-Real-IP $remote_addr;
    }
}
```

说明：

- `HSTS` 只在 HTTPS 终止层启用
- 前端静态资源的 CSP 不在 Spring Boot 中硬编码
- 若要让限流基于真实来源 IP，请同时设置 `RATE_LIMIT_TRUST_FORWARD_HEADERS=true`

## 8. 最小观测部署示例

仓库已内置最小观测配置文件：

- `ops/docker-compose.observability.yml`
- `ops/prometheus/prometheus.yml`
- `ops/promtail/config.yml`
- `ops/grafana/provisioning/datasources/datasources.yml`

启动命令：

```bash
docker compose -f ops/docker-compose.observability.yml up -d
```

对应的 Prometheus 抓取地址应指向：

- Docker Desktop / Windows 默认示例：`http://host.docker.internal:18080/actuator/prometheus`
- 若 Prometheus 与后端在同一主机网络命名空间，改为 `http://127.0.0.1:18080/actuator/prometheus`

更完整的抓取与日志采集配置见 [docs/observability.md](./docs/observability.md)。

## 9. 验收清单

- `curl http://127.0.0.1:8080/api/auth/login` 能返回业务响应
- `http://127.0.0.1:18080/actuator/health` 可用
- Prometheus 能抓到 `/actuator/prometheus`
- Loki 能检索到 JSON 日志
- Grafana 已加载 Prometheus 和 Loki 数据源
- 前后端 Sentry 在配置 `DSN` 后能收到测试异常
- 登录、刷新和 `/api/warning/scan` 触发限流时返回一致错误码

## 10. 常见问题

### 1. 后端无法启动

- 检查 `JWT_SECRET` 是否为空或长度不足 32 字节
- 检查 MySQL 连接变量是否已注入

### 2. Nginx 转发后限流全部命中同一个 IP

- 设置 `RATE_LIMIT_TRUST_FORWARD_HEADERS=true`
- 确认 Nginx 已传递 `X-Forwarded-For`

### 3. 看不到 Prometheus 指标

- 确认管理端口绑定的是本机地址
- 确认 Prometheus 抓取的是 `18080` 而不是 `8080`
