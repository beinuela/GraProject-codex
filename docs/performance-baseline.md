# 性能基线记录

## 1. 说明

本文档记录本轮工程化整改收尾后的本地 k6 冒烟基线，用于后续回归对比。当前表格记录的是“整改后基线”，后续新增优化或回归检查时，应继续复用同一组脚本与字段。

采集时间：`2026-04-21`

采集环境：

- 后端：本地 `screenshot` profile
- 数据源：隔离 H2 数据文件
- 前置条件：`JWT_SECRET` 为有效随机密钥
- 客户端：`k6`
- 脚本目录：`tests/performance/`

## 2. 执行命令

```bash
k6 run tests/performance/login.js
k6 run tests/performance/inventory-list.js
k6 run tests/performance/warning-list.js
k6 run tests/performance/operation-log-list.js
```

Windows 若当前终端未刷新 `PATH`，可直接使用：

```powershell
& 'C:\Program Files\k6\k6.exe' run tests/performance/login.js
```

## 3. 当前基线

| 场景 | 脚本 | 吞吐 | 错误率 | p50 | p95 |
| --- | --- | --- | --- | --- | --- |
| 登录 | `tests/performance/login.js` | `0.93 req/s` | `0.00%` | `74.55 ms` | `78.20 ms` |
| 库存分页列表 | `tests/performance/inventory-list.js` | `1.93 req/s` | `0.00%` | `22.66 ms` | `96.51 ms` |
| 预警分页列表 | `tests/performance/warning-list.js` | `1.97 req/s` | `0.00%` | `13.89 ms` | `20.42 ms` |
| 操作日志分页列表 | `tests/performance/operation-log-list.js` | `1.98 req/s` | `0.00%` | `8.63 ms` | `17.41 ms` |

## 4. 解释约束

- 登录脚本默认负载已控制在限流阈值以内，避免把安全限流误判为性能回归。
- 本表仅用于同机型、同脚本、同数据集的相对对比，不承诺跨机器可复现的绝对 SLA。
- 若后续修改分页契约、索引或限流参数，应同步更新本表的日期与结果。
