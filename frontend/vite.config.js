import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'

// /api 代理到后端（admin 启动器 8080）
export default defineConfig({
  plugins: [vue()],
  server: {
    port: 5173,
    proxy: {
      '/api': {
        target: 'http://localhost:8080',
        changeOrigin: true
      }
    }
  }
})
