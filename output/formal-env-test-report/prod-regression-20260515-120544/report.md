# 真实环境全功能回归测试报告

- 执行时间: 2026-05-15 12:05:44
- 前端地址: http://127.0.0.1:5173
- 后端地址: http://127.0.0.1:8080
- 健康检查: http://127.0.0.1:18080/actuator/health
- 浏览器引擎: chrome
- 可视模式: headed

## 用例结果
- [PASS] 加载真实环境基础数据
- [PASS] 管理员登录与全局路由冒烟
- [PASS] 普通申领角色越权跳转校验
- [PASS] 采购基础数据与入库校验
- [PASS] 申领审批与出库衔接闭环
- [PASS] 调拨推荐与执行签收闭环
- [PASS] 配送派单与签收闭环
- [PASS] 预警扫描与 AI 分析
- [PASS] 事件闭环与统计分析校验

## 关键写入摘要
- 分类: PROD-20260515-120544-分类
- 供应商: PROD-20260515-120544-供应商
- 入库备注: PROD-20260515-120544-stockin
- 申领单: 7 / PROD-20260515-120544-apply
- 出库单: 5 / PROD-20260515-120544-stockout
- 调拨单: 6 / PROD-20260515-120544-transfer
- 配送单: 5 / PROD-20260515-120544-收货人
- 事件: 7 / PROD-20260515-120544-event
- 预警 AI: warningId=11, source=LLM, risk=低风险

## AI 审计表校验
- 状态: passed
- ai_analysis_task 新增记录: 1
- ai_call_log 新增记录: 1
- 最新结果来源: LLM

## 截图产物
- login-dashboard: D:\idea+\project\GraProject-codex\output\formal-env-test-report\prod-regression-20260515-120544\screenshots\01-login-dashboard.png
- inventory-overview: D:\idea+\project\GraProject-codex\output\formal-env-test-report\prod-regression-20260515-120544\screenshots\02-inventory-overview.png
- bigscreen: D:\idea+\project\GraProject-codex\output\formal-env-test-report\prod-regression-20260515-120544\screenshots\03-bigscreen.png
- security-policy: D:\idea+\project\GraProject-codex\output\formal-env-test-report\prod-regression-20260515-120544\screenshots\04-security-policy.png
- category-list: D:\idea+\project\GraProject-codex\output\formal-env-test-report\prod-regression-20260515-120544\screenshots\05-category-list.png
- supplier-list: D:\idea+\project\GraProject-codex\output\formal-env-test-report\prod-regression-20260515-120544\screenshots\06-supplier-list.png
- stock-in-records: D:\idea+\project\GraProject-codex\output\formal-env-test-report\prod-regression-20260515-120544\screenshots\07-stock-in-records.png
- apply-submitted: D:\idea+\project\GraProject-codex\output\formal-env-test-report\prod-regression-20260515-120544\screenshots\08-apply-submitted.png
- stock-out-records: D:\idea+\project\GraProject-codex\output\formal-env-test-report\prod-regression-20260515-120544\screenshots\09-stock-out-records.png
- transfer-recommendation: D:\idea+\project\GraProject-codex\output\formal-env-test-report\prod-regression-20260515-120544\screenshots\10-transfer-recommendation.png
- transfer-received: D:\idea+\project\GraProject-codex\output\formal-env-test-report\prod-regression-20260515-120544\screenshots\11-transfer-received.png
- delivery-signed: D:\idea+\project\GraProject-codex\output\formal-env-test-report\prod-regression-20260515-120544\screenshots\12-delivery-signed.png
- warning-ai-analysis: D:\idea+\project\GraProject-codex\output\formal-env-test-report\prod-regression-20260515-120544\screenshots\13-warning-ai-analysis.png
- event-closed: D:\idea+\project\GraProject-codex\output\formal-env-test-report\prod-regression-20260515-120544\screenshots\14-event-closed.png
- analytics-dashboard: D:\idea+\project\GraProject-codex\output\formal-env-test-report\prod-regression-20260515-120544\screenshots\15-analytics-dashboard.png

## 日志目录
- D:\idea+\project\GraProject-codex\output\formal-env-test-report\prod-regression-20260515-120544\logs