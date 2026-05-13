import { defineStore } from 'pinia'
import { ref } from 'vue'
import api from '../api'
import router from '../router'

export const useAuthStore = defineStore('auth', () => {
  const token = ref(localStorage.getItem('adminToken') || '')

  async function login(username: string, password: string) {
    const res = await api.post('/api/admin/login', { username, password })
    token.value = res.data.data
    localStorage.setItem('adminToken', token.value)
    router.push('/dashboard')
  }

  function logout() {
    token.value = ''
    localStorage.removeItem('adminToken')
    router.push('/login')
  }

  return { token, login, logout }
})
