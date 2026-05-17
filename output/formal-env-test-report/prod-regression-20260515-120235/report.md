# 真实环境全功能回归测试报告

- 执行时间: 2026-05-15 12:02:35
- 前端地址: http://127.0.0.1:5173
- 后端地址: http://127.0.0.1:8080
- 健康检查: http://127.0.0.1:18080/actuator/health
- 浏览器引擎: chrome
- 可视模式: headed

## 用例结果
- [PASS] 加载真实环境基础数据
- [PASS] 管理员登录与全局路由冒烟
- [PASS] 普通申领角色越权跳转校验
- [FAIL] 采购基础数据与入库校验: Error: expect(locator).toBeVisible() failed

Locator: locator('.el-table__row').first()
Expected: visible
Timeout: 20000ms
Error: element(s) not found

Call log:
  - Expect "to.be.visible" with timeout 20000ms
  - waiting for locator('.el-table__row').first()

    at Proxy.<anonymous> (D:\idea+\project\GraProject-codex\frontend\node_modules\playwright\lib\matchers\expect.js:213:24)
    at runPurchaserCrudAndStockInCheck (file:///D:/idea+/project/GraProject-codex/frontend/scripts/run-prod-regression.mjs:468:56)
    at async runCase (file:///D:/idea+/project/GraProject-codex/frontend/scripts/run-prod-regression.mjs:170:20)
    at async file:///D:/idea+/project/GraProject-codex/frontend/scripts/run-prod-regression.mjs:828:3
- [PASS] 申领审批与出库衔接闭环
- [PASS] 调拨推荐与执行签收闭环
- [PASS] 配送派单与签收闭环
- [PASS] 预警扫描与 AI 分析
- [PASS] 事件闭环与统计分析校验

## 关键写入摘要
- 分类: PROD-20260515-120235-分类
- 供应商: PROD-20260515-120235-供应商
- 入库备注: PROD-20260515-120235-stockin
- 申领单: 6 / PROD-20260515-120235-apply
- 出库单: 4 / PROD-20260515-120235-stockout
- 调拨单: 5 / PROD-20260515-120235-transfer
- 配送单: 4 / PROD-20260515-120235-收货人
- 事件: 6 / PROD-20260515-120235-event
- 预警 AI: warningId=10, source=LLM, risk=中风险

## AI 审计表校验
- 状态: passed
- ai_analysis_task 新增记录: 1
- ai_call_log 新增记录: 1
- 最新结果来源: LLM

## 截图产物
- login-dashboard: D:\idea+\project\GraProject-codex\output\formal-env-test-report\prod-regression-20260515-120235\screenshots\01-login-dashboard.png
- inventory-overview: D:\idea+\project\GraProject-codex\output\formal-env-test-report\prod-regression-20260515-120235\screenshots\02-inventory-overview.png
- bigscreen: D:\idea+\project\GraProject-codex\output\formal-env-test-report\prod-regression-20260515-120235\screenshots\03-bigscreen.png
- security-policy: D:\idea+\project\GraProject-codex\output\formal-env-test-report\prod-regression-20260515-120235\screenshots\04-security-policy.png
- category-list: D:\idea+\project\GraProject-codex\output\formal-env-test-report\prod-regression-20260515-120235\screenshots\05-category-list.png
- supplier-list: D:\idea+\project\GraProject-codex\output\formal-env-test-report\prod-regression-20260515-120235\screenshots\06-supplier-list.png
- apply-submitted: D:\idea+\project\GraProject-codex\output\formal-env-test-report\prod-regression-20260515-120235\screenshots\07-apply-submitted.png
- stock-out-records: D:\idea+\project\GraProject-codex\output\formal-env-test-report\prod-regression-20260515-120235\screenshots\08-stock-out-records.png
- transfer-recommendation: D:\idea+\project\GraProject-codex\output\formal-env-test-report\prod-regression-20260515-120235\screenshots\09-transfer-recommendation.png
- transfer-received: D:\idea+\project\GraProject-codex\output\formal-env-test-report\prod-regression-20260515-120235\screenshots\10-transfer-received.png
- delivery-signed: D:\idea+\project\GraProject-codex\output\formal-env-test-report\prod-regression-20260515-120235\screenshots\11-delivery-signed.png
- warning-ai-analysis: D:\idea+\project\GraProject-codex\output\formal-env-test-report\prod-regression-20260515-120235\screenshots\12-warning-ai-analysis.png
- event-closed: D:\idea+\project\GraProject-codex\output\formal-env-test-report\prod-regression-20260515-120235\screenshots\13-event-closed.png
- analytics-dashboard: D:\idea+\project\GraProject-codex\output\formal-env-test-report\prod-regression-20260515-120235\screenshots\14-analytics-dashboard.png

## 日志目录
- D:\idea+\project\GraProject-codex\output\formal-env-test-report\prod-regression-20260515-120235\logs