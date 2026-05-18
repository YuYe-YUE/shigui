import { sha256 } from 'js-sha256'
import { defineStore } from 'pinia'
import { ref } from 'vue'
import api from '../api'
import router from '../router'

export const useAuthStore = defineStore('auth', () => {
  const token = ref(localStorage.getItem('adminToken') || '')

  // 登录：前端先 SHA-256 哈希再发送，避免密码明文出现在请求体中。
  async function login(username: string, password: string) {
    const hashed = sha256(password)
    const res = await api.post('/api/admin/login', { username, password: hashed })
    if (res.data.code !== 200 || !res.data.data) {
      throw new Error(res.data.message || '登录失败')
    }
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
