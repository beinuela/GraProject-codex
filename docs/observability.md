# 可观测性说明

## 0. 当前交付状态

截至 `2026-04-21`，仓库侧已完成：

- 后端 `health / info / metrics / prometheus` 管理端点
- `campus_*` 业务指标接入
- Logback JSON 滚动日志与 `requestId` 透传
- `ops/docker-compose.observability.yml` 及其最小 Prometheus / Promtail / Grafana 配置
- 前后端 Sentry 的环境变量化降级接入

本机已验证：

- `http://127.0.0.1:18080/actuator/health` 与 `/actuator/prometheus` 可访问
- Prometheus 可抓取 `campus_auth_login_total`
- Loki 可检索 JSON 日志
- Grafana 已自动装载 Prometheus 与 Loki 数据源

仍需人工前置条件：

- Docker daemon 需先启动
- 真实 Sentry 验收需要 `SENTRY_DSN` 与 `VITE_SENTRY_DSN`

## 1. 目标

本项目的单机观测体系拆为三条链路：

- 指标：Spring Boot Actuator + Micrometer + Prometheus
- 日志：Logback JSON + Rolling File + Promtail + Loki
- 错误：Sentry（后端 + 前端）

目标不是“全功能云原生平台”，而是让单机内网部署也具备可定位、可检索、可告警的最小闭环。

## 2. 组件总览

| 组件 | 作用 | 默认端口/位置 |
| --- | --- | --- |
| Spring Boot Actuator | 暴露 `health/info/metrics/prometheus` | `127.0.0.1:18080` |
| Prometheus | 抓取指标 | `9090` |
| Grafana | 展示面板与告警 | `3000` |
| Logback JSON 日志 | 结构化业务日志 | `${APP_LOG_DIR}/campus-backend.log` |
| Promtail | 采集日志文件 | 本地 agent |
| Loki | 日志存储与检索 | `3100` |
| Sentry | 前后端异常追踪 | 外部服务 |

## 3. 管理端点

后端启用以下端点：

- `/actuator/health`
- `/actuator/info`
- `/actuator/metrics`
- `/actuator/prometheus`

约束：

- 管理端口与业务端口分离
- `MANAGEMENT_SERVER_ADDRESS` 默认绑定 `127.0.0.1`
- 这些端点不作为业务 API 对外暴露

## 4. 业务指标

当前自定义业务指标包括：

- 登录成功次数
- 登录失败次数
- token 刷新失败次数
- 申领单提交次数
- 申领单审批次数
- 调拨执行次数
- 库存入库次数
- 库存出库次数
- 预警扫描耗时
- 预警扫描新增数量

建议 Grafana 至少做以下面板：

- 登录成功/失败趋势
- 刷新失败趋势
- 申领提交/审批趋势
- 调拨执行趋势
- 库存出入库趋势
- 预警扫描耗时 P50 / P95
- 5xx 错误数与 Sentry 事件数

## 5. 日志格式

后端日志采用 JSON 行格式，字段至少包含：

- `timestamp`
- `level`
- `module`
- `thread`
- `requestId`
- `userId`
- `method`
- `path`
- `message`
- `exception`

说明：

- `requestId` 通过自定义过滤器注入 MDC，并回写到响应头 `X-Request-Id`
- 日志文件默认路径：`${APP_LOG_DIR}/campus-backend.log`
- 归档目录：`${APP_LOG_DIR}/archive/`

## 6. Prometheus 抓取配置

仓库内默认文件位于 `ops/prometheus/prometheus.yml`。Docker Desktop / Windows 示例使用 `host.docker.internal:18080` 回抓宿主机管理端口；若 Prometheus 与后端共享主机网络，可改为 `127.0.0.1:18080`。

```yaml
global:
  scrape_interval: 15s

scrape_configs:
  - job_name: campus-backend
    metrics_path: /actuator/prometheus
    static_configs:
      - targets:
          - host.docker.internal:18080
```

## 7. Promtail 采集配置

仓库内默认文件位于 `ops/promtail/config.yml`。若后端日志目录不是 `backend/logs`，只需要同步修改挂载路径与 `__path__`。

```yaml
server:
  http_listen_port: 9080
  grpc_listen_port: 0

positions:
  filename: /tmp/promtail-positions.yml

clients:
  - url: http://127.0.0.1:3100/loki/api/v1/push

scrape_configs:
  - job_name: campus-backend
    static_configs:
      - targets:
          - localhost
        labels:
          job: campus-backend
          app: campus-backend
          __path__: /var/log/campus-backend/campus-backend.log
```

如果主机挂载目录不同，请只修改 `__path__`。

## 8. Sentry 接入

### 后端

环境变量：

- `SENTRY_DSN`
- `SENTRY_ENVIRONMENT`
- `SENTRY_TRACES_SAMPLE_RATE`

行为：

- 未配置 `SENTRY_DSN` 时自动关闭
- 上报未捕获异常
- 对关键业务异常和 5xx 进行主动采集

### 前端

环境变量：

- `VITE_SENTRY_DSN`
- `VITE_SENTRY_ENVIRONMENT`
- `VITE_SENTRY_TRACES_SAMPLE_RATE`

行为：

- 上报运行时异常
- 上报路由错误
- 上报接口错误摘要

## 9. 建议告警

单机内网环境下，建议先做以下基础告警：

- `health != UP`
- `http_server_requests_seconds_count{status=~"5.."}`
- `login_failure_count` 在 5 分钟内突增
- `warning_scan_duration` 明显高于基线
- Loki 中同类异常日志持续增长
- Sentry 新 issue 数量异常

## 10. 排障顺序

建议固定按以下顺序排查：

1. `health` 是否正常
2. Prometheus 是否还能抓到 `/actuator/prometheus`
3. Grafana 面板是否有指标断点
4. Loki 是否还能检索到对应 `requestId`
5. Sentry 是否有同一时间段的异常事件

## 11. 本机端口建议

| 服务 | 建议端口 |
| --- | --- |
| 后端业务 API | `8080` |
| Actuator | `18080` |
| Prometheus | `9090` |
| Grafana | `3000` |
| Loki | `3100` |
| Promtail HTTP | `9080` |

## 12. 运行验收

完成部署后，至少验证：

- `curl http://127.0.0.1:18080/actuator/health`
- `curl http://127.0.0.1:18080/actuator/prometheus`
- Grafana 可同时连上 Prometheus 和 Loki
- Loki 可检索到包含 `requestId` 的 JSON 日志
- 手动制造一个前端异常与一个后端异常，若已配置 `DSN`，Sentry 均能收到
