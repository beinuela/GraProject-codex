import { execFileSync, spawn } from 'node:child_process'
import path from 'node:path'

const frontendPort = 4173
const frontendUrl = `http://127.0.0.1:${frontendPort}`
const managementPort = process.env.MANAGEMENT_SERVER_PORT || '18080'
const managementAddress = process.env.MANAGEMENT_SERVER_ADDRESS || '127.0.0.1'
const backendUrl = `http://${managementAddress}:${managementPort}/actuator/health`
const headed = process.argv.includes('--headed')
const defaultCorsOrigins = 'http://localhost:5173,http://127.0.0.1:5173,http://localhost:4173,http://127.0.0.1:4173'
const e2eDbPath = path.resolve(process.cwd(), '../backend/tmp/h2', `campus_material_e2e_${Date.now()}`)
const e2eJdbcUrl = `jdbc:h2:file:${e2eDbPath.replace(/\\/g, '/')};DB_CLOSE_ON_EXIT=FALSE;MODE=MySQL;CASE_INSENSITIVE_IDENTIFIERS=TRUE`
const viteCommand = path.resolve(process.cwd(), 'node_modules/.bin', process.platform === 'win32' ? 'vite.cmd' : 'vite')
const playwrightCommand = path.resolve(process.cwd(), 'node_modules/.bin', process.platform === 'win32' ? 'playwright.cmd' : 'playwright')

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
    : spawn(command, args, {
      stdio: 'inherit',
      ...options
    })
  processes.push(child)
  return child
}

const waitForUrl = async (url, timeoutMs = 120000) => {
  const deadline = Date.now() + timeoutMs
  let lastError
  while (Date.now() < deadline) {
    try {
      const response = await fetch(url, { method: 'GET' })
      if (response.ok || response.status === 401 || response.status === 403 || response.status === 405) {
        return
      }
      lastError = new Error(`Unexpected status ${response.status} for ${url}`)
    } catch (error) {
      lastError = error
    }
    await new Promise(resolve => setTimeout(resolve, 1500))
  }
  throw lastError || new Error(`Timed out waiting for ${url}`)
}

const stopProcessTree = (child) => {
  if (!child?.pid) {
    return
  }

  try {
    if (process.platform === 'win32') {
      execFileSync('taskkill', ['/PID', String(child.pid), '/T', '/F'], { stdio: 'ignore' })
    } else {
      child.kill('SIGTERM')
    }
  } catch (_) {
    // ignore cleanup errors during test shutdown
  }
}

const stopProcesses = () => {
  for (const child of [...processes].reverse()) {
    stopProcessTree(child)
  }
}

process.on('SIGINT', () => {
  stopProcesses()
  process.exit(130)
})

process.on('SIGTERM', () => {
  stopProcesses()
  process.exit(143)
})

try {
  startProcess('mvn', ['-f', '../backend/pom.xml', 'spring-boot:run'], {
    cwd: process.cwd(),
    env: {
      ...process.env,
      SPRING_PROFILES_ACTIVE: 'screenshot',
      JWT_SECRET: process.env.JWT_SECRET || 'E2E_TEST_JWT_SECRET_MUST_BE_AT_LEAST_32_BYTES_LONG',
      MANAGEMENT_SERVER_ADDRESS: managementAddress,
      MANAGEMENT_SERVER_PORT: managementPort,
      CORS_ALLOWED_ORIGINS: process.env.CORS_ALLOWED_ORIGINS || defaultCorsOrigins,
      SPRING_DATASOURCE_URL: process.env.SPRING_DATASOURCE_URL || e2eJdbcUrl
    }
  })

  startProcess(viteCommand, ['--host', '127.0.0.1', '--port', String(frontendPort)], {
    cwd: process.cwd(),
    env: {
      ...process.env,
      VITE_API_TARGET: process.env.VITE_API_TARGET || 'http://127.0.0.1:8080'
    }
  })

  await Promise.all([
    waitForUrl(backendUrl),
    waitForUrl(frontendUrl)
  ])

  const playwrightArgs = ['test']
  if (headed) {
    playwrightArgs.push('--headed')
  }

  await new Promise((resolve, reject) => {
    const child = process.platform === 'win32'
      ? spawn(process.env.ComSpec || 'cmd.exe', ['/d', '/s', '/c', [playwrightCommand, ...playwrightArgs].map(formatToken).join(' ')], {
        stdio: 'inherit',
        cwd: process.cwd()
      })
      : spawn(playwrightCommand, playwrightArgs, {
        stdio: 'inherit',
        cwd: process.cwd()
      })
    child.on('exit', (code) => {
      if (code === 0) {
        resolve()
      } else {
        reject(new Error(`Playwright exited with code ${code}`))
      }
    })
  })
} finally {
  stopProcesses()
}
