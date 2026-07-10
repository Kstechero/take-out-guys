import { defineStore } from 'pinia'
import request from '@/utils/request'

export const useAuthStore = defineStore('auth', {
  state: () => ({
    token: localStorage.getItem('sky_admin_token') || '',
    name: localStorage.getItem('sky_admin_name') || '管理员'
  }),
  actions: {
    async login(username: string, password: string) {
      const res: any = await request.post('/employee/login', { username, password })
      this.token = res.data.token
      this.name = res.data.name || username
      localStorage.setItem('sky_admin_token', this.token)
      localStorage.setItem('sky_admin_name', this.name)
    },
    async logout() {
      try { await request.post('/employee/logout') } finally {
        this.token = ''; localStorage.removeItem('sky_admin_token'); localStorage.removeItem('sky_admin_name')
      }
    }
  }
})
