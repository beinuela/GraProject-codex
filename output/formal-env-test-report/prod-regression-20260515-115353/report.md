# 真实环境全功能回归测试报告

- 执行时间: 2026-05-15 11:53:53
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

Locator: getByText('PROD-20260515-115353-分类')
Expected: visible
Timeout: 20000ms
Error: element(s) not found

Call log:
  - Expect "to.be.visible" with timeout 20000ms
  - waiting for getByText('PROD-20260515-115353-分类')

    at Proxy.<anonymous> (D:\idea+\project\GraProject-codex\frontend\node_modules\playwright\lib\matchers\expect.js:213:24)
    at runPurchaserCrudAndStockInCheck (file:///D:/idea+/project/GraProject-codex/frontend/scripts/run-prod-regression.mjs:447:61)
    at async runCase (file:///D:/idea+/project/GraProject-codex/frontend/scripts/run-prod-regression.mjs:170:20)
    at async file:///D:/idea+/project/GraProject-codex/frontend/scripts/run-prod-regression.mjs:829:3
- [FAIL] 申领审批与出库签收闭环: Error: expect(locator).toBeVisible() failed

Locator: locator('.el-table__row').filter({ hasText: 'PROD-20260515-115353-apply' }).first().getByText('SUBMITTED')
Expected: visible
Timeout: 20000ms
Error: element(s) not found

Call log:
  - Expect "to.be.visible" with timeout 20000ms
  - waiting for locator('.el-table__row').filter({ hasText: 'PROD-20260515-115353-apply' }).first().getByText('SUBMITTED')

    at Proxy.<anonymous> (D:\idea+\project\GraProject-codex\frontend\node_modules\playwright\lib\matchers\expect.js:213:24)
    at runApplyApproveStockOutReceiveFlow (file:///D:/idea+/project/GraProject-codex/frontend/scripts/run-prod-regression.mjs:478:87)
    at async runCase (file:///D:/idea+/project/GraProject-codex/frontend/scripts/run-prod-regression.mjs:170:20)
    at async file:///D:/idea+/project/GraProject-codex/frontend/scripts/run-prod-regression.mjs:830:3
- [PASS] 调拨推荐与执行签收闭环
- [SKIP] 配送派单与签收闭环: 缺少申领单或出库单，跳过配送流程
- [PASS] 预警扫描与 AI 分析
- [PASS] 事件闭环与统计分析校验

## 关键写入摘要
- 分类: PROD-20260515-115353-分类
- 供应商: PROD-20260515-115353-供应商
- 入库备注: 未创建
- 申领单: 4 / PROD-20260515-115353-apply
- 出库单: 未创建 / -
- 调拨单: 3 / PROD-20260515-115353-transfer
- 配送单: 未创建 / -
- 事件: 4 / PROD-20260515-115353-event
- 预警 AI: warningId=7, source=LLM, risk=高风险

## AI 审计表校验
- 状态: passed
- ai_analysis_task 新增记录: 1
- ai_call_log 新增记录: 1
- 最新结果来源: LLM

## 截图产物
- login-dashboard: D:\idea+\project\GraProject-codex\output\formal-env-test-report\prod-regression-20260515-115353\screenshots\01-login-dashboard.png
- inventory-overview: D:\idea+\project\GraProject-codex\output\formal-env-test-report\prod-regression-20260515-115353\screenshots\02-inventory-overview.png
- bigscreen: D:\idea+\project\GraProject-codex\output\formal-env-test-report\prod-regression-20260515-115353\screenshots\03-bigscreen.png
- security-policy: D:\idea+\project\GraProject-codex\output\formal-env-test-report\prod-regression-20260515-115353\screenshots\04-security-policy.png
- transfer-recommendation: D:\idea+\project\GraProject-codex\output\formal-env-test-report\prod-regression-20260515-115353\screenshots\05-transfer-recommendation.png
- transfer-received: D:\idea+\project\GraProject-codex\output\formal-env-test-report\prod-regression-20260515-115353\screenshots\06-transfer-received.png
- warning-ai-analysis: D:\idea+\project\GraProject-codex\output\formal-env-test-report\prod-regression-20260515-115353\screenshots\07-warning-ai-analysis.png
- event-closed: D:\idea+\project\GraProject-codex\output\formal-env-test-report\prod-regression-20260515-115353\screenshots\08-event-closed.png
- analytics-dashboard: D:\idea+\project\GraProject-codex\output\formal-env-test-report\prod-regression-20260515-115353\screenshots\09-analytics-dashboard.png

## 日志目录
- D:\idea+\project\GraProject-codex\output\formal-env-test-report\prod-regression-20260515-115353\logs