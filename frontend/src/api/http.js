import axios from 'axios'
import router from '../router'

// HTTP 统一封装：拦截器带 JWT、401 跳登录
const http = axios.create({
  baseURL: '/api',
  timeout: 15000
})

http.interceptors.request.use((config) => {
  const token = localStorage.getItem('token')
  if (token) config.headers.Authorization = `Bearer ${token}`
  return config
})

http.interceptors.response.use(
  (res) => res.data,
  (err) => {
    if (err.response?.status === 401) {
      localStorage.removeItem('token')
      router.push({ path: '/login', query: { redirect: router.currentRoute.value.fullPath } })
    }
    return Promise.reject(err)
  }
)

export default http
