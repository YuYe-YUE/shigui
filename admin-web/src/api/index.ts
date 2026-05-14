import axios from 'axios'
import { ElMessage } from 'element-plus'

// 后端本地开发地址；后续部署时可改成环境变量。
const api = axios.create({ baseURL: 'http://127.0.0.1:8080' })

api.interceptors.request.use((config) => {
  // 后端 Sa-Token 默认读取 satoken 请求头。
  const token = localStorage.getItem('adminToken')
  if (token) config.headers.satoken = token
  return config
})

api.interceptors.response.use(
  (res) => {
    if (res.data.code !== 200) {
      ElMessage.error(res.data.message || '请求失败')
      return Promise.reject(new Error(res.data.message || '请求失败'))
    }
    return res
  },
  (err) => {
    // token 失效时清理本地登录态，避免用户继续停留在受保护页面。
    if (err.response?.status === 401) {
      localStorage.removeItem('adminToken')
      window.location.href = '/login'
      return Promise.reject(err)
    }
    ElMessage.error('网络错误')
    return Promise.reject(err)
  }
)

export default api
