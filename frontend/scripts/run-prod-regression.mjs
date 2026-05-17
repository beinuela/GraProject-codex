import { chromium, expect, request } from '@playwright/test'
import { execFileSync, spawn, spawnSync } from 'node:child_process'
import fs from 'node:fs'
import path from 'node:path'

const frontendPort = 5173
const frontendUrl = `http://127.0.0.1:${frontendPort}`
const backendUrl = 'http://127.0.0.1:8080'
const managementPort = process.env.MANAGEMENT_SERVER_PORT || '18080'
const managementAddress = process.env.MANAGEMENT_SERVER_ADDRESS || '127.0.0.1'
const healthUrl = `http://${managementAddress}:${managementPort}/actuator/health`
const defaultCorsOrigins = 'http://localhost:5173,http://127.0.0.1:5173,http://localhost:4173,http://127.0.0.1:4173'
const headed = !process.argv.includes('--headless')
const repoRoot = path.resolve(process.cwd(), '..')
const timestamp = stamp()
const runStartedAt = new Date()
const outputRoot = path.resolve(repoRoot, 'output', 'formal-env-test-report', `prod-regression-${timestamp}`)
const screenshotDir = path.resolve(outputRoot, 'screenshots')
const logDir = path.resolve(outputRoot, 'logs')
const reportPath = path.resolve(outputRoot, 'report.md')
const resultPath = path.resolve(outputRoot, 'results.json')
const viteCommand = path.resolve(process.cwd(), 'node_modules/.bin', process.platform === 'win32' ? 'vite.cmd' : 'vite')

fs.mkdirSync(screenshotDir, { recursive: true })
fs.mkdirSync(logDir, { recursive: true })

const processes = []
const results = []
const screenshotArtifacts = []
const createdRecords = {
  categoryName: '',
  supplierName: '',
  stockInRemark: '',
  applyId: null,
  applyReason: '',
  stockOutId: null,
  stockOutRemark: '',
  transferId: null,
  transferReason: '',
  deliveryId: null,
  deliveryReceiver: '',
  eventId: null,
  eventTitle: '',
  aiWarningId: null,
  aiSource: '',
  aiRiskLevel: ''
}
const runTag = `PROD-${timestamp}`

let browserInfo = {
  engine: 'unknown',
  headed
}
let dbVerification = {
  status: 'not-run'
}

const accounts = {
  admin: { username: 'admin', password: 'Abc@123456' },
  warehouse: { username: 'warehouse', password: 'Abc@123456' },
  dept: { username: 'dept', password: 'Abc@123456' },
  approver: { username: 'approver', password: 'Abc@123456' },
  purchaser: { username: 'purchaser', password: 'Abc@123456' },
  dispatcher: { username: 'dispatcher', password: 'Abc@123456' }
}

const baseData = {
  primaryWarehouse: null,
  secondaryWarehouse: null,
  primaryMaterial: null,
  dispatcherId: null
}

const formatToken = (arg) => {
  const value = String(arg)
  return /\s/.test(value) ? `"${value.replace(/"/g, '\\"')}"` : value
}

const commandString = (command, args) => [command, ...args].map(formatToken).join(' ')

const startProcess = (label, command, args, options = {}) => {
  const stdoutPath = path.resolve(logDir, `${label}.out.log`)
  const stderrPath = path.resolve(logDir, `${label}.err.log`)
  const stdout = fs.openSync(stdoutPath, 'a')
  const stderr = fs.openSync(stderrPath, 'a')
  const child = process.platform === 'win32'
    ? spawn(process.env.ComSpec || 'cmd.exe', ['/d', '/s', '/c', commandString(command, args)], {
      stdio: ['ignore', stdout, stderr],
      ...options
    })
    : spawn(command, args, {
      stdio: ['ignore', stdout, stderr],
      ...options
    })
  processes.push(child)
  return child
}

const waitForUrl = async (url, timeoutMs = 180000) => {
  const deadline = Date.now() + timeoutMs
  let lastError
  while (Date.now() < deadline) {
    try {
      const response = await fetch(url, { method: 'GET' })
      if (response.ok || [401, 403, 405].includes(response.status)) {
        return
      }
      lastError = new Error(`Unexpected status ${response.status} for ${url}`)
    } catch (error) {
      lastError = error
    }
    await sleep(1500)
  }
  throw lastError || new Error(`Timed out waiting for ${url}`)
}

const stopProcessTree = (child) => {
  if (!child?.pid) return
  try {
    if (process.platform === 'win32') {
      execFileSync('taskkill', ['/PID', String(child.pid), '/T', '/F'], { stdio: 'ignore' })
    } else {
      child.kill('SIGTERM')
    }
  } catch (_) {
    // ignore cleanup errors
  }
}

const stopProcesses = () => {
  for (const child of [...processes].reverse()) {
    stopProcessTree(child)
  }
}

const sleep = (ms) => new Promise(resolve => setTimeout(resolve, ms))

function stamp() {
  const now = new Date()
  const yyyy = now.getFullYear()
  const mm = String(now.getMonth() + 1).padStart(2, '0')
  const dd = String(now.getDate()).padStart(2, '0')
  const hh = String(now.getHours()).padStart(2, '0')
  const mi = String(now.getMinutes()).padStart(2, '0')
  const ss = String(now.getSeconds()).padStart(2, '0')
  return `${yyyy}${mm}${dd}-${hh}${mi}${ss}`
}

const formatSqlDate = (value) => {
  const yyyy = value.getFullYear()
  const mm = String(value.getMonth() + 1).padStart(2, '0')
  const dd = String(value.getDate()).padStart(2, '0')
  const hh = String(value.getHours()).padStart(2, '0')
  const mi = String(value.getMinutes()).padStart(2, '0')
  const ss = String(value.getSeconds()).padStart(2, '0')
  return `${yyyy}-${mm}-${dd} ${hh}:${mi}:${ss}`
}

const recordResult = (name, status, extra = {}) => {
  results.push({
    name,
    status,
    ...extra
  })
}

const runCase = async (name, fn) => {
  const startedAt = Date.now()
  try {
    const detail = await fn()
    recordResult(name, 'passed', {
      durationMs: Date.now() - startedAt,
      detail: detail || ''
    })
    return detail
  } catch (error) {
    recordResult(name, 'failed', {
      durationMs: Date.now() - startedAt,
      error: error instanceof Error ? (error.stack || error.message) : String(error)
    })
    return null
  }
}

const skipCase = (name, reason) => {
  recordResult(name, 'skipped', { reason })
}

const capture = async (page, name, note = '') => {
  const fileName = `${String(screenshotArtifacts.length + 1).padStart(2, '0')}-${sanitize(name)}.png`
  const filePath = path.resolve(screenshotDir, fileName)
  await page.screenshot({ path: filePath, fullPage: true })
  screenshotArtifacts.push({
    name,
    note,
    path: filePath
  })
  return filePath
}

const sanitize = (value) => String(value).replace(/[^a-zA-Z0-9-_]+/g, '-').replace(/-+/g, '-').replace(/^-|-$/g, '').toLowerCase()

const parsePayload = async (response, label) => {
  if (!response.ok()) {
    throw new Error(`${label} 返回 HTTP ${response.status()}`)
  }
  const payload = await response.json()
  if (!payload || ![0, 200].includes(payload.code)) {
    throw new Error(`${label} 返回业务失败: ${payload?.message || 'unknown error'}`)
  }
  return payload.data
}

const parseApiJson = async (response, label) => {
  if (!response.ok()) {
    throw new Error(`${label} 返回 HTTP ${response.status()}`)
  }
  const payload = await response.json()
  if (!payload || ![0, 200].includes(payload.code)) {
    throw new Error(`${label} 返回业务失败: ${payload?.message || 'unknown error'}`)
  }
  return payload.data
}

const launchBrowser = async () => {
  try {
    const browser = await chromium.launch({
      channel: 'chrome',
      headless: !headed,
      slowMo: headed ? 120 : 0,
      args: ['--window-size=1440,1024']
    })
    browserInfo = { engine: 'chrome', headed }
    return browser
  } catch (error) {
    const browser = await chromium.launch({
      headless: !headed,
      slowMo: headed ? 120 : 0,
      args: ['--window-size=1440,1024']
    })
    browserInfo = {
      engine: 'chromium-fallback',
      headed,
      fallbackReason: error instanceof Error ? error.message : String(error)
    }
    return browser
  }
}

const newUiSession = async (browser, accountKey) => {
  const account = accounts[accountKey]
  const context = await browser.newContext({
    viewport: { width: 1440, height: 1024 }
  })
  const page = await context.newPage()
  await page.goto(`${frontendUrl}/login`, { waitUntil: 'domcontentloaded' })
  await page.getByPlaceholder('用户名').fill(account.username)
  await page.getByPlaceholder('密码').fill(account.password)
  await page.getByRole('button', { name: '登录系统' }).click()
  await expect(page).toHaveURL(/dashboard/, { timeout: 30000 })
  await expect(page.locator('.shell-context__title')).toHaveText('系统首页', { timeout: 30000 })
  return { context, page }
}

const closeUiSession = async (session) => {
  await session?.context?.close()
}

const openRoute = async (page, route, title) => {
  await page.goto(`${frontendUrl}${route}`, { waitUntil: 'domcontentloaded' })
  await expect(page.locator('.shell-context__title')).toHaveText(title, { timeout: 30000 })
  await sleep(700)
}

const openDialog = async (page, buttonName, titleText) => {
  await page.getByRole('button', { name: buttonName }).click()
  const dialog = page.locator('.el-dialog:visible').last()
  await expect(dialog.getByText(titleText)).toBeVisible({ timeout: 15000 })
  return dialog
}

const closeDialog = async (page) => {
  const dialog = page.locator('.el-dialog:visible').last()
  if (await dialog.count()) {
    const closeButton = dialog.getByRole('button', { name: '取消' })
    if (await closeButton.count()) {
      await closeButton.click()
    } else {
      await page.keyboard.press('Escape')
    }
  }
}

const getRowByText = (page, text) => page.locator('.el-table__row').filter({ hasText: text }).first()

const getRowId = async (row) => {
  await expect(row).toBeVisible({ timeout: 20000 })
  const idText = (await row.locator('td').first().innerText()).trim()
  const id = Number(idText)
  if (!Number.isFinite(id)) {
    throw new Error(`无法从表格首列解析 ID: ${idText}`)
  }
  return id
}

const clickRowButton = async (row, buttonName) => {
  await row.scrollIntoViewIfNeeded()
  await row.getByRole('button', { name: buttonName }).click()
}

const pickOption = async (page, selectTrigger, optionName, exact = true) => {
  await selectTrigger.click()
  await page.getByRole('option', { name: optionName, exact }).click()
}

const createApiSession = async (accountKey) => {
  const account = accounts[accountKey]
  const ctx = await request.newContext({ baseURL: backendUrl })
  const loginResponse = await ctx.post('/api/auth/login', {
    data: {
      username: account.username,
      password: account.password
    }
  })
  const loginData = await parseApiJson(loginResponse, `${accountKey} 登录`)
  const token = loginData.accessToken || loginData.token
  const authed = await request.newContext({
    baseURL: backendUrl,
    extraHTTPHeaders: {
      Authorization: `Bearer ${token}`
    }
  })
  const me = await parseApiJson(await authed.get('/api/auth/me'), `${accountKey} 资料`)
  return { ctx: authed, me }
}

const prepareBaseData = async () => {
  const adminApi = await createApiSession('admin')
  const warehouses = await parseApiJson(await adminApi.ctx.get('/api/warehouse/list'), '仓库列表')
  const materials = await parseApiJson(await adminApi.ctx.get('/api/material/info'), '物资列表')
  const dispatcherApi = await createApiSession('dispatcher')
  baseData.primaryWarehouse = warehouses.find(item => /总仓|科学/.test(item.warehouseName || '')) || warehouses[0]
  baseData.secondaryWarehouse = warehouses.find(item => item.id !== baseData.primaryWarehouse?.id) || warehouses[0]
  baseData.primaryMaterial = materials.find(item => /口罩/.test(item.materialName || '')) || materials[0]
  baseData.dispatcherId = dispatcherApi.me?.id
  await adminApi.ctx.dispose()
  await dispatcherApi.ctx.dispose()
  if (!baseData.primaryWarehouse || !baseData.secondaryWarehouse || !baseData.primaryMaterial) {
    throw new Error('无法从真实库加载基础仓库或物资数据')
  }
}

const createStockInViaApi = async () => {
  const purchaserApi = await createApiSession('purchaser')
  const stockInRemark = `${runTag}-stockin`
  createdRecords.stockInRemark = stockInRemark
  const payload = {
    warehouseId: baseData.primaryWarehouse.id,
    sourceType: 'PURCHASE',
    remark: stockInRemark,
    items: [
      {
        materialId: baseData.primaryMaterial.id,
        quantity: 12,
        batchNo: `BATCH-${timestamp}`,
        productionDate: '2026-05-01',
        expireDate: '2027-05-01'
      }
    ]
  }
  await parseApiJson(await purchaserApi.ctx.post('/api/inventory/stock-in', { data: payload }), '创建入库单')
  await purchaserApi.ctx.dispose()
}

const createApplyViaApi = async () => {
  const deptApi = await createApiSession('dept')
  const applyReason = `${runTag}-apply`
  createdRecords.applyReason = applyReason
  const payload = {
    deptId: 5,
    urgencyLevel: 1,
    reason: applyReason,
    scenario: `${runTag}-scenario`,
    items: [
      {
        materialId: baseData.primaryMaterial.id,
        applyQty: 2
      }
    ]
  }
  const applyData = await parseApiJson(await deptApi.ctx.post('/api/apply', { data: payload }), '创建申领单')
  createdRecords.applyId = applyData?.id || null
  await deptApi.ctx.dispose()
}

const verifyUiLoginAndRoutes = async (browser) => {
  const session = await newUiSession(browser, 'admin')
  const { page } = session
  await capture(page, 'login-dashboard', '管理员登录后首页')
  await openRoute(page, '/rbac/users', '用户管理')
  await openRoute(page, '/rbac/roles', '角色管理')
  await openRoute(page, '/rbac/depts', '部门管理')
  await openRoute(page, '/campus/list', '校区管理')
  await openRoute(page, '/material/info', '物资档案')
  await openRoute(page, '/warehouse/list', '仓库管理')
  await openRoute(page, '/warehouse/location', '库位管理')
  await openRoute(page, '/inventory/list', '库存查询')
  await capture(page, 'inventory-overview', '库存查询页面')
  await page.goto(`${frontendUrl}/bigscreen`, { waitUntil: 'domcontentloaded' })
  await sleep(1000)
  await capture(page, 'bigscreen', '指挥大屏')
  await openRoute(page, '/log/operation', '操作日志')
  await openRoute(page, '/log/login', '登录日志')
  await openRoute(page, '/notification/list', '通知中心')
  await openRoute(page, '/config/list', '系统配置')
  await openRoute(page, '/security/policy', '安全策略')
  await capture(page, 'security-policy', '安全策略页面')
  await page.getByRole('button', { name: '刷新策略' }).click()
  await page.getByRole('button', { name: '一键手动清理' }).click()
  await sleep(1200)
  await page.locator('.shell-user').click()
  await page.getByText('退出登录').click()
  await expect(page).toHaveURL(/login/, { timeout: 30000 })
  await closeUiSession(session)
}

const verifyRoleRedirect = async (browser) => {
  const session = await newUiSession(browser, 'dept')
  const { page } = session
  await page.goto(`${frontendUrl}/rbac/users`, { waitUntil: 'domcontentloaded' })
  await expect(page).toHaveURL(/dashboard/, { timeout: 30000 })
  await closeUiSession(session)
}

const runPurchaserCrudAndStockInCheck = async (browser) => {
  createdRecords.categoryName = `${runTag}-分类`
  createdRecords.supplierName = `${runTag}-供应商`

  let session = await newUiSession(browser, 'admin')
  let page = session.page
  await openRoute(page, '/material/category', '分类管理')
  let dialog = await openDialog(page, '新增分类', '新增分类')
  await dialog.locator('input').nth(0).fill(createdRecords.categoryName)
  await dialog.locator('input').nth(1).fill(`CAT-${timestamp}`)
  await dialog.locator('textarea').fill('真实环境回归创建的分类数据')
  await dialog.getByRole('button', { name: '保存' }).click()
  await expect(page.getByText(createdRecords.categoryName)).toBeVisible({ timeout: 20000 })
  await capture(page, 'category-list', '分类管理页面和新增分类')
  await closeUiSession(session)

  session = await newUiSession(browser, 'purchaser')
  page = session.page
  await openRoute(page, '/supplier/list', '供应商管理')
  dialog = await openDialog(page, '新增供应商', '新增供应商')
  const supplierFields = dialog.locator('input')
  await supplierFields.nth(0).fill(createdRecords.supplierName)
  await supplierFields.nth(1).fill('测试联系人')
  await supplierFields.nth(2).fill('13800000000')
  await supplierFields.nth(3).fill(`prod-${timestamp}@example.com`)
  await dialog.locator('textarea').nth(0).fill('防疫物资、应急物资')
  await dialog.locator('textarea').nth(1).fill('真实环境回归创建的供应商')
  await dialog.getByRole('button', { name: '保存' }).click()
  await expect(page.getByText(createdRecords.supplierName)).toBeVisible({ timeout: 20000 })
  await capture(page, 'supplier-list', '供应商页面和新增记录')

  await createStockInViaApi()
  await openRoute(page, '/inventory/stock-in', '入库管理')
  await capture(page, 'stock-in-records', '真实环境入库记录')
  await closeUiSession(session)
}

const runApplyApproveStockOutReceiveFlow = async (browser) => {
  await createApplyViaApi()

  let session = await newUiSession(browser, 'dept')
  let page = session.page
  await openRoute(page, '/apply/list', '申领审批')
  let row = getRowByText(page, createdRecords.applyReason)
  createdRecords.applyId = await getRowId(row)
  await clickRowButton(row, '提交')
  await expect(getRowByText(page, createdRecords.applyReason).getByText('SUBMITTED')).toBeVisible({ timeout: 20000 })
  await capture(page, 'apply-submitted', '申领单提交后状态')
  await closeUiSession(session)

  session = await newUiSession(browser, 'approver')
  page = session.page
  await openRoute(page, '/apply/list', '申领审批')
  row = getRowByText(page, createdRecords.applyReason)
  await clickRowButton(row, '审批')
  await expect(getRowByText(page, createdRecords.applyReason).getByText('APPROVED')).toBeVisible({ timeout: 20000 })
  await closeUiSession(session)

  session = await newUiSession(browser, 'warehouse')
  page = session.page
  await openRoute(page, '/inventory/stock-out', '出库管理')
  const dialog = await openDialog(page, '新建出库单', '新建出库单')
  await dialog.locator('input[role="spinbutton"]').first().fill(String(createdRecords.applyId))
  await pickOption(page, dialog.locator('.el-form-item').nth(1).locator('.el-select'), baseData.primaryWarehouse.warehouseName, false)
  createdRecords.stockOutRemark = `${runTag}-stockout`
  await dialog.locator('textarea').fill(createdRecords.stockOutRemark)
  await pickOption(page, dialog.locator('.collection-editor .el-select').first(), baseData.primaryMaterial.materialName, false)
  await dialog.locator('.collection-editor input[role="spinbutton"]').first().fill('1')
  await dialog.getByRole('button', { name: '提交出库' }).click()
  row = getRowByText(page, createdRecords.stockOutRemark)
  createdRecords.stockOutId = await getRowId(row)
  await capture(page, 'stock-out-records', '关联申领的出库记录')
  await closeUiSession(session)
}

const runTransferFlow = async (browser) => {
  const session = await newUiSession(browser, 'warehouse')
  const { page } = session
  await openRoute(page, '/transfer/list', '调拨管理')
  const dialog = await openDialog(page, '新建调拨', '新建调拨')
  await pickOption(page, dialog.locator('.el-form-item').nth(0).locator('.el-select'), baseData.secondaryWarehouse.warehouseName, false)
  await pickOption(page, dialog.locator('.collection-editor .el-select').first(), baseData.primaryMaterial.materialName, false)
  await dialog.locator('.collection-editor input[role="spinbutton"]').first().fill('1')
  await dialog.getByRole('button', { name: '智能推荐' }).click()
  await expect(page.getByText('推荐调度方案')).toBeVisible({ timeout: 20000 })
  await capture(page, 'transfer-recommendation', '调拨智能推荐结果')
  await page.getByRole('button', { name: '应用此仓' }).first().click()
  createdRecords.transferReason = `${runTag}-transfer`
  await dialog.locator('textarea').fill(createdRecords.transferReason)
  await dialog.getByRole('button', { name: '提交申请' }).click()
  let row = getRowByText(page, createdRecords.transferReason)
  createdRecords.transferId = await getRowId(row)
  await clickRowButton(row, '提交')
  await expect(getRowByText(page, createdRecords.transferReason).getByText('SUBMITTED')).toBeVisible({ timeout: 20000 })
  await closeUiSession(session)

  let approverSession = await newUiSession(browser, 'approver')
  let approverPage = approverSession.page
  await openRoute(approverPage, '/transfer/list', '调拨管理')
  row = getRowByText(approverPage, createdRecords.transferReason)
  await clickRowButton(row, '审批')
  await expect(getRowByText(approverPage, createdRecords.transferReason).getByText('APPROVED')).toBeVisible({ timeout: 20000 })
  await closeUiSession(approverSession)

  let warehouseSession = await newUiSession(browser, 'warehouse')
  let warehousePage = warehouseSession.page
  await openRoute(warehousePage, '/transfer/list', '调拨管理')
  row = getRowByText(warehousePage, createdRecords.transferReason)
  await clickRowButton(row, '执行')
  await expect(getRowByText(warehousePage, createdRecords.transferReason).getByText('OUTBOUND')).toBeVisible({ timeout: 20000 })
  row = getRowByText(warehousePage, createdRecords.transferReason)
  await clickRowButton(row, '签收')
  await expect(getRowByText(warehousePage, createdRecords.transferReason).getByText('RECEIVED')).toBeVisible({ timeout: 20000 })
  await capture(warehousePage, 'transfer-received', '调拨执行并签收完成')
  await closeUiSession(warehouseSession)
}

const runDeliveryFlow = async (browser) => {
  const session = await newUiSession(browser, 'dispatcher')
  const { page } = session
  createdRecords.deliveryReceiver = `${runTag}-收货人`
  await openRoute(page, '/delivery/list', '配送派单')
  const dialog = await openDialog(page, '生成配送任务', '生成配送任务')
  const formItems = dialog.locator('.el-form-item')
  await formItems.nth(0).locator('input[role="spinbutton"]').fill(String(createdRecords.applyId))
  await formItems.nth(1).locator('input[role="spinbutton"]').fill(String(createdRecords.stockOutId))
  await formItems.nth(2).locator('input').fill(createdRecords.deliveryReceiver)
  await formItems.nth(3).locator('input').fill('13900000000')
  await formItems.nth(4).locator('input').fill('科学校区行政楼 A101')
  await dialog.locator('textarea').fill('真实环境配送回归任务')
  await dialog.getByRole('button', { name: '保存' }).click()
  let row = getRowByText(page, createdRecords.deliveryReceiver)
  createdRecords.deliveryId = await getRowId(row)
  await clickRowButton(row, '派单')
  await page.getByRole('button', { name: '确认派单' }).click()
  row = getRowByText(page, createdRecords.deliveryReceiver)
  await clickRowButton(row, '开始配送')
  await openRoute(page, '/delivery/list', '配送派单')
  row = getRowByText(page, createdRecords.deliveryReceiver)
  await clickRowButton(row, '签收')
  await openRoute(page, '/delivery/list', '配送派单')
  await expect(getRowByText(page, createdRecords.deliveryReceiver).getByText('已签收')).toBeVisible({ timeout: 20000 })
  await openRoute(page, '/apply/list', '申领审批')
  await expect(getRowByText(page, createdRecords.applyReason).getByText('RECEIVED')).toBeVisible({ timeout: 20000 })
  await capture(page, 'delivery-signed', '配送签收后申领同步闭环')
  await closeUiSession(session)
}

const runWarningAiFlow = async (browser) => {
  const session = await newUiSession(browser, 'warehouse')
  const { page } = session
  await openRoute(page, '/warning/list', '预警中心')
  await page.getByRole('button', { name: '手动扫描' }).click()
  await sleep(1500)
  const firstAiButton = page.getByRole('button', { name: 'AI 分析' }).first()
  await expect(firstAiButton).toBeVisible({ timeout: 20000 })
  const row = page.locator('.el-table__row').filter({ has: firstAiButton }).first()
  createdRecords.aiWarningId = await getRowId(row)
  await firstAiButton.click()
  await expect(page.getByText('AI 预警分析')).toBeVisible({ timeout: 30000 })
  await capture(page, 'warning-ai-analysis', 'DeepSeek 预警分析弹窗')
  const sourceText = await page.locator('.shell-dialog__body').getByText(/DeepSeek|规则回退/).first().innerText()
  createdRecords.aiSource = sourceText.includes('DeepSeek') ? 'LLM' : 'RULE_FALLBACK'
  createdRecords.aiRiskLevel = await page.locator('.shell-dialog__body').getByText(/低风险|中风险|高风险|极高风险/).first().innerText()
  await page.getByRole('button', { name: '关闭' }).click()
  const handleButton = row.getByRole('button', { name: '处理' })
  if (await handleButton.count()) {
    await handleButton.click()
    await page.locator('.el-message-box__input input').fill('真实环境回归已处理')
    await page.getByRole('button', { name: '确定' }).click()
  }
  await closeUiSession(session)
}

const runEventAndAnalyticsFlow = async (browser) => {
  const session = await newUiSession(browser, 'admin')
  const { page } = session
  createdRecords.eventTitle = `${runTag}-event`
  await openRoute(page, '/event/list', '事件管理')
  let dialog = await openDialog(page, '上报事件', '上报事件')
  const eventFormItems = dialog.locator('.el-form-item')
  await eventFormItems.nth(0).locator('input').fill(createdRecords.eventTitle)
  await pickOption(page, dialog.locator('.el-form-item').nth(1).locator('.el-select'), '公共卫生')
  await pickOption(page, dialog.locator('.el-form-item').nth(2).locator('.el-select'), '四级（一般）')
  await dialog.locator('textarea').fill('真实环境回归事件上报测试')
  await eventFormItems.nth(4).locator('input').fill('科学校区后勤仓')
  await dialog.getByRole('button', { name: '提交' }).click()
  let row = getRowByText(page, createdRecords.eventTitle)
  createdRecords.eventId = await getRowId(row)
  await clickRowButton(row, '处理')
  row = getRowByText(page, createdRecords.eventTitle)
  await clickRowButton(row, '关闭')
  await page.locator('.el-message-box__input input').fill('真实环境回归已完成处置')
  await page.getByRole('button', { name: '确定' }).click()
  await capture(page, 'event-closed', '事件管理闭环记录')

  await openRoute(page, '/analytics/charts', '统计分析')
  await sleep(1500)
  await capture(page, 'analytics-dashboard', '统计分析与智能建议')
  await closeUiSession(session)
}

const verifyAiDatabaseArtifacts = () => {
  const mysqlCommand = spawnSync(process.platform === 'win32' ? 'where' : 'which', ['mysql'], { encoding: 'utf8' })
  if (mysqlCommand.status !== 0) {
    dbVerification = {
      status: 'skipped',
      reason: 'mysql 命令行未安装或不可用'
    }
    return
  }

  const envFile = path.resolve(repoRoot, '.env')
  if (!fs.existsSync(envFile)) {
    dbVerification = {
      status: 'skipped',
      reason: '.env 不存在，无法解析真实库连接信息'
    }
    return
  }

  const envMap = parseEnvFile(envFile)
  const jdbcUrl = envMap.DB_URL || 'jdbc:mysql://127.0.0.1:3306/campus_material'
  const parsed = parseJdbcUrl(jdbcUrl)
  const username = envMap.DB_USERNAME || ''
  const password = envMap.DB_PASSWORD || ''
  if (!username || !parsed.database) {
    dbVerification = {
      status: 'skipped',
      reason: '数据库用户名或数据库名缺失，跳过 AI 审计表校验'
    }
    return
  }

  const startedAt = formatSqlDate(runStartedAt)
  const sql = [
    `select count(*) from ai_analysis_task where created_at >= '${startedAt}';`,
    `select count(*) from ai_call_log where created_at >= '${startedAt}';`,
    `select coalesce(result_source, '') from ai_analysis_task where created_at >= '${startedAt}' order by id desc limit 1;`
  ].join(' ')
  const args = ['-h', parsed.host, '-P', String(parsed.port), '-u', username, `--password=${password}`, '-D', parsed.database, '-N', '-e', sql]
  const result = spawnSync('mysql', args, { encoding: 'utf8' })
  if (result.status !== 0) {
    dbVerification = {
      status: 'failed',
      error: result.stderr || result.stdout || 'mysql 查询失败'
    }
    return
  }

  const [taskCount, logCount, latestSource] = String(result.stdout || '').trim().split(/\r?\n/)
  dbVerification = {
    status: 'passed',
    taskCount: Number(taskCount || 0),
    logCount: Number(logCount || 0),
    latestSource: latestSource || ''
  }
}

const parseEnvFile = (filePath) => {
  const content = fs.readFileSync(filePath, 'utf8')
  const result = {}
  for (const rawLine of content.split(/\r?\n/)) {
    const line = rawLine.trim()
    if (!line || line.startsWith('#')) continue
    const index = line.indexOf('=')
    if (index === -1) continue
    const key = line.slice(0, index).trim()
    let value = line.slice(index + 1).trim()
    if ((value.startsWith('"') && value.endsWith('"')) || (value.startsWith("'") && value.endsWith("'"))) {
      value = value.slice(1, -1)
    }
    result[key] = value
  }
  return result
}

const parseJdbcUrl = (jdbcUrl) => {
  const matched = jdbcUrl.match(/^jdbc:mysql:\/\/([^:/?#]+)(?::(\d+))?\/([^?]+)/i)
  if (!matched) {
    return { host: '127.0.0.1', port: 3306, database: 'campus_material' }
  }
  return {
    host: matched[1],
    port: Number(matched[2] || 3306),
    database: matched[3]
  }
}

const writeArtifacts = () => {
  const summary = {
    timestamp,
    frontendUrl,
    backendUrl,
    healthUrl,
    browserInfo,
    results,
    screenshots: screenshotArtifacts,
    createdRecords,
    dbVerification
  }
  fs.writeFileSync(resultPath, JSON.stringify(summary, null, 2), 'utf8')

  const markdown = [
    '# 真实环境全功能回归测试报告',
    '',
    `- 执行时间: ${formatSqlDate(runStartedAt)}`,
    `- 前端地址: ${frontendUrl}`,
    `- 后端地址: ${backendUrl}`,
    `- 健康检查: ${healthUrl}`,
    `- 浏览器引擎: ${browserInfo.engine}`,
    `- 可视模式: ${browserInfo.headed ? 'headed' : 'headless'}`,
    '',
    '## 用例结果',
    ...results.map(item => {
      if (item.status === 'passed') return `- [PASS] ${item.name}${item.detail ? `: ${item.detail}` : ''}`
      if (item.status === 'skipped') return `- [SKIP] ${item.name}: ${item.reason}`
      return `- [FAIL] ${item.name}: ${item.error || 'unknown error'}`
    }),
    '',
    '## 关键写入摘要',
    `- 分类: ${createdRecords.categoryName || '未创建'}`,
    `- 供应商: ${createdRecords.supplierName || '未创建'}`,
    `- 入库备注: ${createdRecords.stockInRemark || '未创建'}`,
    `- 申领单: ${createdRecords.applyId || '未创建'} / ${createdRecords.applyReason || '-'}`,
    `- 出库单: ${createdRecords.stockOutId || '未创建'} / ${createdRecords.stockOutRemark || '-'}`,
    `- 调拨单: ${createdRecords.transferId || '未创建'} / ${createdRecords.transferReason || '-'}`,
    `- 配送单: ${createdRecords.deliveryId || '未创建'} / ${createdRecords.deliveryReceiver || '-'}`,
    `- 事件: ${createdRecords.eventId || '未创建'} / ${createdRecords.eventTitle || '-'}`,
    `- 预警 AI: warningId=${createdRecords.aiWarningId || '-'}, source=${createdRecords.aiSource || '-'}, risk=${createdRecords.aiRiskLevel || '-'}`,
    '',
    '## AI 审计表校验',
    `- 状态: ${dbVerification.status}`,
    ...(dbVerification.status === 'passed'
      ? [
          `- ai_analysis_task 新增记录: ${dbVerification.taskCount}`,
          `- ai_call_log 新增记录: ${dbVerification.logCount}`,
          `- 最新结果来源: ${dbVerification.latestSource || '-'}`
        ]
      : [dbVerification.reason || dbVerification.error || '未执行']),
    '',
    '## 截图产物',
    ...screenshotArtifacts.map(item => `- ${item.name}: ${item.path}`),
    '',
    '## 日志目录',
    `- ${logDir}`
  ].join('\n')

  fs.writeFileSync(reportPath, markdown, 'utf8')
}

process.on('SIGINT', () => {
  stopProcesses()
  process.exit(130)
})

process.on('SIGTERM', () => {
  stopProcesses()
  process.exit(143)
})

let browser

try {
  startProcess('backend-prod', 'mvn', ['-f', '../backend/pom.xml', 'spring-boot:run'], {
    cwd: process.cwd(),
    env: {
      ...process.env,
      SPRING_PROFILES_ACTIVE: 'prod',
      MANAGEMENT_SERVER_ADDRESS: managementAddress,
      MANAGEMENT_SERVER_PORT: managementPort,
      CORS_ALLOWED_ORIGINS: process.env.CORS_ALLOWED_ORIGINS || defaultCorsOrigins
    }
  })

  startProcess('frontend-vite', viteCommand, ['--host', '127.0.0.1', '--port', String(frontendPort)], {
    cwd: process.cwd(),
    env: {
      ...process.env,
      VITE_API_TARGET: process.env.VITE_API_TARGET || backendUrl
    }
  })

  await Promise.all([
    waitForUrl(healthUrl),
    waitForUrl(frontendUrl)
  ])

  await runCase('加载真实环境基础数据', prepareBaseData)
  browser = await launchBrowser()
  await runCase('管理员登录与全局路由冒烟', () => verifyUiLoginAndRoutes(browser))
  await runCase('普通申领角色越权跳转校验', () => verifyRoleRedirect(browser))
  await runCase('采购基础数据与入库校验', () => runPurchaserCrudAndStockInCheck(browser))
  await runCase('申领审批与出库衔接闭环', () => runApplyApproveStockOutReceiveFlow(browser))
  await runCase('调拨推荐与执行签收闭环', () => runTransferFlow(browser))
  if (!createdRecords.applyId || !createdRecords.stockOutId) {
    skipCase('配送派单与签收闭环', '缺少申领单或出库单，跳过配送流程')
  } else {
    await runCase('配送派单与签收闭环', () => runDeliveryFlow(browser))
  }
  await runCase('预警扫描与 AI 分析', () => runWarningAiFlow(browser))
  await runCase('事件闭环与统计分析校验', () => runEventAndAnalyticsFlow(browser))
  verifyAiDatabaseArtifacts()
} finally {
  if (browser) {
    await browser.close()
  }
  stopProcesses()
  writeArtifacts()
}
