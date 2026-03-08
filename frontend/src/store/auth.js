import { defineStore } from 'pinia'
import { apiGet, apiPost } from '../api'

export const useAuthStore = defineStore('auth', {
  state: () => ({
    token: localStorage.getItem('token') || '',
    user: null,
    menus: []
  }),
  actions: {
    async login(username, password) {
      const data = await apiPost('/api/auth/login', { username, password })
      this.token = data.token
      localStorage.setItem('token', data.token)
      await this.loadProfile()
    },
    logout() {
      this.token = ''
      this.user = null
      this.menus = []
      localStorage.removeItem('token')
    },
    async loadProfile() {
      if (!this.token) return
      this.user = await apiGet('/api/auth/me')
      this.menus = await apiGet('/api/auth/menus')
    }
  }
})
