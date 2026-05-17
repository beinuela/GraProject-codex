# 真实环境全功能回归测试报告

- 执行时间: 2026-05-15 11:58:31
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

Locator: getByText('PROD-20260515-115831-stockin')
Expected: visible
Timeout: 20000ms
Error: element(s) not found

Call log:
  - Expect "to.be.visible" with timeout 20000ms
  - waiting for getByText('PROD-20260515-115831-stockin')

    at Proxy.<anonymous> (D:\idea+\project\GraProject-codex\frontend\node_modules\playwright\lib\matchers\expect.js:213:24)
    at runPurchaserCrudAndStockInCheck (file:///D:/idea+/project/GraProject-codex/frontend/scripts/run-prod-regression.mjs:468:62)
    at async runCase (file:///D:/idea+/project/GraProject-codex/frontend/scripts/run-prod-regression.mjs:170:20)
    at async file:///D:/idea+/project/GraProject-codex/frontend/scripts/run-prod-regression.mjs:833:3
- [PASS] 申领审批与出库签收闭环
- [PASS] 调拨推荐与执行签收闭环
- [FAIL] 配送派单与签收闭环: Error: expect(locator).toBeVisible() failed

Locator: locator('.el-table__row').filter({ hasText: 'PROD-20260515-115831-收货人' }).first().getByText('已签收')
Expected: visible
Timeout: 20000ms
Error: element(s) not found

Call log:
  - Expect "to.be.visible" with timeout 20000ms
  - waiting for locator('.el-table__row').filter({ hasText: 'PROD-20260515-115831-收货人' }).first().getByText('已签收')

    at Proxy.<anonymous> (D:\idea+\project\GraProject-codex\frontend\node_modules\playwright\lib\matchers\expect.js:213:24)
    at runDeliveryFlow (file:///D:/idea+/project/GraProject-codex/frontend/scripts/run-prod-regression.mjs:584:86)
    at async runCase (file:///D:/idea+/project/GraProject-codex/frontend/scripts/run-prod-regression.mjs:170:20)
    at async file:///D:/idea+/project/GraProject-codex/frontend/scripts/run-prod-regression.mjs:839:5
- [PASS] 预警扫描与 AI 分析
- [PASS] 事件闭环与统计分析校验

## 关键写入摘要
- 分类: PROD-20260515-115831-分类
- 供应商: PROD-20260515-115831-供应商
- 入库备注: PROD-20260515-115831-stockin
- 申领单: 5 / PROD-20260515-115831-apply
- 出库单: 3 / PROD-20260515-115831-stockout
- 调拨单: 4 / PROD-20260515-115831-transfer
- 配送单: 3 / PROD-20260515-115831-收货人
- 事件: 5 / PROD-20260515-115831-event
- 预警 AI: warningId=9, source=LLM, risk=低风险

## AI 审计表校验
- 状态: passed
- ai_analysis_task 新增记录: 1
- ai_call_log 新增记录: 1
- 最新结果来源: LLM

## 截图产物
- login-dashboard: D:\idea+\project\GraProject-codex\output\formal-env-test-report\prod-regression-20260515-115831\screenshots\01-login-dashboard.png
- inventory-overview: D:\idea+\project\GraProject-codex\output\formal-env-test-report\prod-regression-20260515-115831\screenshots\02-inventory-overview.png
- bigscreen: D:\idea+\project\GraProject-codex\output\formal-env-test-report\prod-regression-20260515-115831\screenshots\03-bigscreen.png
- security-policy: D:\idea+\project\GraProject-codex\output\formal-env-test-report\prod-regression-20260515-115831\screenshots\04-security-policy.png
- category-list: D:\idea+\project\GraProject-codex\output\formal-env-test-report\prod-regression-20260515-115831\screenshots\05-category-list.png
- supplier-list: D:\idea+\project\GraProject-codex\output\formal-env-test-report\prod-regression-20260515-115831\screenshots\06-supplier-list.png
- apply-submitted: D:\idea+\project\GraProject-codex\output\formal-env-test-report\prod-regression-20260515-115831\screenshots\07-apply-submitted.png
- stock-out-records: D:\idea+\project\GraProject-codex\output\formal-env-test-report\prod-regression-20260515-115831\screenshots\08-stock-out-records.png
- apply-received: D:\idea+\project\GraProject-codex\output\formal-env-test-report\prod-regression-20260515-115831\screenshots\09-apply-received.png
- transfer-recommendation: D:\idea+\project\GraProject-codex\output\formal-env-test-report\prod-regression-20260515-115831\screenshots\10-transfer-recommendation.png
- transfer-received: D:\idea+\project\GraProject-codex\output\formal-env-test-report\prod-regression-20260515-115831\screenshots\11-transfer-received.png
- warning-ai-analysis: D:\idea+\project\GraProject-codex\output\formal-env-test-report\prod-regression-20260515-115831\screenshots\12-warning-ai-analysis.png
- event-closed: D:\idea+\project\GraProject-codex\output\formal-env-test-report\prod-regression-20260515-115831\screenshots\13-event-closed.png
- analytics-dashboard: D:\idea+\project\GraProject-codex\output\formal-env-test-report\prod-regression-20260515-115831\screenshots\14-analytics-dashboard.png

## 日志目录
- D:\idea+\project\GraProject-codex\output\formal-env-test-report\prod-regression-20260515-115831\logs