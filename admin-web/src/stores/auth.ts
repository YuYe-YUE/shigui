import { defineStore } from 'pinia'
import { ref } from 'vue'
import api from '../api'
import router from '../router'

export const useAuthStore = defineStore('auth', () => {
  // 刷新页面后从 localStorage 恢复 token，避免登录态立刻丢失。
  const token = ref(localStorage.getItem('adminToken') || '')

  async function login(username: string, password: string) {
    const res = await api.post('/api/admin/login', { username, password })
    token.value = res.data.data
    localStorage.setItem('adminToken', token.value)
    router.push('/dashboard')
  }

  function logout() {
    // 退出登录时同时清理内存状态和持久化 token。
    token.value = ''
    localStorage.removeItem('adminToken')
    router.push('/login')
  }

  return { token, login, logout }
})
