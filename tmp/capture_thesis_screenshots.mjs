import { execFileSync, spawn } from 'node:child_process'
import { createRequire } from 'node:module'
import fs from 'node:fs'
import path from 'node:path'

const repoRoot = path.resolve(process.cwd(), '..')
const require = createRequire(path.resolve(process.cwd(), 'package.json'))
const { chromium } = require('@playwright/test')
const outDir = path.resolve(repoRoot, 'output/doc/runtime-screenshots')
fs.mkdirSync(outDir, { recursive: true })

const frontendPort = 4173
const frontendUrl = `http://127.0.0.1:${frontendPort}`
const backendUrl = 'http://127.0.0.1:18080/actuator/health'
const e2eDbPath = path.resolve(repoRoot, 'backend/tmp/h2', `campus_material_screenshot_${Date.now()}`)
const e2eJdbcUrl = `jdbc:h2:file:${e2eDbPath.replace(/\\/g, '/')};DB_CLOSE_ON_EXIT=FALSE;MODE=MySQL;CASE_INSENSITIVE_IDENTIFIERS=TRUE`
const viteCommand = path.resolve(process.cwd(), 'node_modules/.bin', process.platform === 'win32' ? 'vite.cmd' : 'vite')

const processes = []
const formatToken = (arg) => {
  const value = String(arg)
  return /\s/.test(value) ? `"${value.replace(/"/g, '\\"')}"` : value
}

const startProcess = (command, args, options = {}) => {
  const child = process.platform === 'win32'
    ? spawn(process.env.ComSpec || 'cmd.exe', ['/d', '/s', '/c', [command, ...args].map(formatToken).join(' ')], {
      stdio: 'inherit',
      ...options
    })
    : spawn(command, args, { stdio: 'inherit', ...options })
  processes.push(child)
  return child
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
    // ignore shutdown races
  }
}

const stopProcesses = () => {
  for (const child of [...processes].reverse()) {
    stopProcessTree(child)
  }
}

const waitForUrl = async (url, timeoutMs = 120000) => {
  const deadline = Date.now() + timeoutMs
  let lastError
  while (Date.now() < deadline) {
    try {
      const response = await fetch(url)
      if (response.ok || [401, 403, 405].includes(response.status)) return
      lastError = new Error(`Unexpected status ${response.status} for ${url}`)
    } catch (error) {
      lastError = error
    }
    await new Promise(resolve => setTimeout(resolve, 1500))
  }
  throw lastError || new Error(`Timed out waiting for ${url}`)
}

const login = async (page, username) => {
  await page.goto(`${frontendUrl}/login`, { waitUntil: 'networkidle' })
  await page.getByPlaceholder('用户名').fill(username)
  await page.getByPlaceholder('密码').fill('Abc@123456')
  await page.getByRole('button', { name: '登录系统' }).click()
  await page.waitForURL(/dashboard/, { timeout: 30000 })
}

const screenshotPage = async (page, route, name) => {
  await page.goto(`${frontendUrl}${route}`, { waitUntil: 'networkidle' })
  await page.screenshot({ path: path.join(outDir, name), fullPage: true })
}

try {
  startProcess('mvn', ['-f', '../backend/pom.xml', 'spring-boot:run'], {
    cwd: process.cwd(),
    env: {
      ...process.env,
      SPRING_PROFILES_ACTIVE: 'screenshot',
      JWT_SECRET: process.env.JWT_SECRET || 'SCREENSHOT_JWT_SECRET_MUST_BE_AT_LEAST_32_BYTES',
      MANAGEMENT_SERVER_ADDRESS: '127.0.0.1',
      MANAGEMENT_SERVER_PORT: '18080',
      CORS_ALLOWED_ORIGINS: 'http://localhost:5173,http://127.0.0.1:5173,http://localhost:4173,http://127.0.0.1:4173',
      SPRING_DATASOURCE_URL: e2eJdbcUrl
    }
  })

  startProcess(viteCommand, ['--host', '127.0.0.1', '--port', String(frontendPort)], {
    cwd: process.cwd(),
    env: {
      ...process.env,
      VITE_API_TARGET: 'http://127.0.0.1:8080'
    }
  })

  await Promise.all([waitForUrl(backendUrl), waitForUrl(frontendUrl)])

  const browser = await chromium.launch()
  const page = await browser.newPage({ viewport: { width: 1440, height: 960 } })

  await page.goto(`${frontendUrl}/login`, { waitUntil: 'networkidle' })
  await page.screenshot({ path: path.join(outDir, 'fig_6_2_login.png'), fullPage: true })

  await login(page, 'admin')
  await screenshotPage(page, '/dashboard', 'fig_6_8_dashboard.png')
  await screenshotPage(page, '/analytics/charts', 'fig_6_9_analytics.png')

  await page.evaluate(() => localStorage.clear())
  await login(page, 'dept')
  await screenshotPage(page, '/apply/list', 'fig_6_3_apply.png')

  await page.evaluate(() => localStorage.clear())
  await login(page, 'warehouse')
  await screenshotPage(page, '/inventory/list', 'fig_6_4_inventory.png')
  await screenshotPage(page, '/transfer/list', 'fig_6_6_transfer.png')
  await screenshotPage(page, '/warning/list', 'fig_6_7_warning.png')

  await browser.close()
  console.log(`Screenshots saved to ${outDir}`)
} finally {
  stopProcesses()
}
