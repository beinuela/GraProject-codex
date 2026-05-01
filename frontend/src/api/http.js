import axios from 'axios'
import { ElMessage } from 'element-plus'
import { captureFrontendError } from '../monitoring'

const http = axios.create({
  baseURL: '/',
  timeout: 15000
})

let isRefreshing = false
let waitQueue = []
const SUCCESS_CODES = new Set([0, 200])

const flushQueue = (error, token = '') => {
  waitQueue.forEach((p) => {
    if (error) {
      p.reject(error)
    } else {
      p.resolve(token)
    }
  })
  waitQueue = []
}

http.interceptors.request.use((config) => {
  const token = localStorage.getItem('token')
  if (token) {
    config.headers.Authorization = `Bearer ${token}`
  }
  return config
})

http.interceptors.response.use(
  (resp) => {
    const payload = resp.data
    if (payload && !SUCCESS_CODES.has(payload.code)) {
      const message = resolveMessage(payload.code, payload.message)
      if (payload.code >= 500) {
        captureFrontendError(new Error(payload.message || '请求失败'), {
          phase: 'api-response',
          status: payload.code,
          url: resp?.config?.url
        })
      }
      ElMessage.error(message)
      return Promise.reject(new Error(message))
    }
    return payload.data
  },
  (error) => {
    const originalRequest = error?.config || {}
    const code = error?.response?.status

    if (
      code === 401 &&
      !originalRequest.__retry &&
      !String(originalRequest.url || '').includes('/api/auth/login') &&
      !String(originalRequest.url || '').includes('/api/auth/refresh')
    ) {
      const refreshToken = localStorage.getItem('refreshToken')
      if (!refreshToken) {
        localStorage.removeItem('token')
        localStorage.removeItem('refreshToken')
        if (location.pathname !== '/login') {
          location.href = '/login'
        }
        return Promise.reject(error)
      }

      if (isRefreshing) {
        return new Promise((resolve, reject) => {
          waitQueue.push({ resolve, reject })
        }).then((newToken) => {
          originalRequest.headers = originalRequest.headers || {}
          originalRequest.headers.Authorization = `Bearer ${newToken}`
          originalRequest.__retry = true
          return http(originalRequest)
        })
      }

      isRefreshing = true
      return axios
        .post('/api/auth/refresh', { refreshToken }, { baseURL: '/', timeout: 15000 })
        .then((resp) => {
          const payload = resp?.data
          if (!payload || !SUCCESS_CODES.has(payload.code)) {
            throw new Error(resolveMessage(payload?.code, payload?.message || '刷新登录态失败'))
          }
          const data = payload.data || {}
          const newAccessToken = data.accessToken || data.token
          const newRefreshToken = data.refreshToken || refreshToken
          localStorage.setItem('token', newAccessToken)
          localStorage.setItem('refreshToken', newRefreshToken)
          flushQueue(null, newAccessToken)

          originalRequest.headers = originalRequest.headers || {}
          originalRequest.headers.Authorization = `Bearer ${newAccessToken}`
          originalRequest.__retry = true
          return http(originalRequest)
        })
        .catch((refreshErr) => {
          flushQueue(refreshErr)
          localStorage.removeItem('token')
          localStorage.removeItem('refreshToken')
          ElMessage.error('登录已失效，请重新登录')
          if (location.pathname !== '/login') {
            location.href = '/login'
          }
          return Promise.reject(refreshErr)
        })
        .finally(() => {
          isRefreshing = false
        })
    }

    if (!code || code >= 500) {
      captureFrontendError(error, {
        phase: 'http-error',
        status: code || 0,
        url: originalRequest.url || ''
      })
    }

    ElMessage.error(resolveMessage(code, error?.response?.data?.message || error.message))
    return Promise.reject(error)
  }
)

function resolveMessage(status, message) {
  if (message) {
    return message
  }
  switch (status) {
    case 400:
      return '请求参数错误'
    case 401:
      return '登录已失效，请重新登录'
    case 403:
      return '无权限访问该资源'
    case 409:
      return '当前操作与业务状态冲突'
    case 500:
      return '服务器异常，请稍后重试'
    default:
      return status ? '请求失败' : '网络连接异常，请检查服务是否启动'
  }
}

export default http
