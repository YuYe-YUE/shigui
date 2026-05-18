import { defineStore } from 'pinia'
import { ref } from 'vue'
import api from '../api'
import router from '../router'

export const useAuthStore = defineStore('auth', () => {
  // 刷新页面后从 localStorage 恢复 token，避免登录态立刻丢失。
  const token = ref(localStorage.getItem('adminToken') || '')

  // 登录：前端先 SHA-256 哈希再发送，避免密码明文出现在请求体中。
  async function login(username: string, password: string) {
    const hashed = await sha256(password)
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

/** 使用 Web Crypto API 计算 SHA-256 十六进制摘要 */
async function sha256(message: string): Promise<string> {
  const msgBuffer = new TextEncoder().encode(message)
  const hashBuffer = await crypto.subtle.digest('SHA-256', msgBuffer)
  return Array.from(new Uint8Array(hashBuffer))
    .map(b => b.toString(16).padStart(2, '0'))
    .join('')
}
