import { defineStore } from 'pinia'
import { apiGet, apiPost } from '../api'

export const useAuthStore = defineStore('auth', {
  state: () => ({
    token: localStorage.getItem('token') || '',
    refreshToken: localStorage.getItem('refreshToken') || '',
    user: null,
    menus: []
  }),
  actions: {
    async login(username, password) {
      const data = await apiPost('/api/auth/login', { username, password })
      const accessToken = data.accessToken || data.token
      this.token = accessToken
      this.refreshToken = data.refreshToken || ''
      localStorage.setItem('token', accessToken)
      if (this.refreshToken) {
        localStorage.setItem('refreshToken', this.refreshToken)
      } else {
        localStorage.removeItem('refreshToken')
      }
      if (data.roleCode) localStorage.setItem('roleCode', data.roleCode)
      if (data.username) localStorage.setItem('username', data.username)
      localStorage.setItem('realName', data.realName || '')
      await this.loadProfile()
    },
    async logout() {
      try {
        if (this.token) {
          await apiPost('/api/auth/logout', {})
        }
      } catch (_) {
        // ignore
      }
      this.token = ''
      this.refreshToken = ''
      this.user = null
      this.menus = []
      localStorage.removeItem('token')
      localStorage.removeItem('refreshToken')
      localStorage.removeItem('roleCode')
      localStorage.removeItem('username')
      localStorage.removeItem('realName')
    },
    async loadProfile() {
      if (!this.token) return
      this.user = await apiGet('/api/auth/me')
      this.menus = await apiGet('/api/auth/menus')
    }
  }
})
