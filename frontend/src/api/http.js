import axios from 'axios'
import { ElMessage } from 'element-plus'

const http = axios.create({
  baseURL: '/',
  timeout: 15000
})

let isRefreshing = false
let waitQueue = []

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
    if (payload && payload.code !== 0) {
      ElMessage.error(payload.message || '请求失败')
      return Promise.reject(new Error(payload.message || '请求失败'))
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
          if (!payload || payload.code !== 0) {
            throw new Error(payload?.message || '刷新登录态失败')
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
          if (location.pathname !== '/login') {
            location.href = '/login'
          }
          return Promise.reject(refreshErr)
        })
        .finally(() => {
          isRefreshing = false
        })
    }

    ElMessage.error(error?.response?.data?.message || error.message || '网络错误')
    return Promise.reject(error)
  }
)

export default http
