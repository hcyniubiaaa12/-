import { defineStore } from 'pinia'
import { ref, computed } from 'vue'

// 登录态 store
export const useUserStore = defineStore('user', () => {
  const token = ref(localStorage.getItem('token') || '')
  const role = ref(localStorage.getItem('role') || '')
  const nickname = ref(localStorage.getItem('nickname') || '')

  const isLogin = computed(() => !!token.value)
  const isAdmin = computed(() => role.value === 'ADMIN')

  function setLogin(data) {
    token.value = data.token
    role.value = data.role
    nickname.value = data.nickname
    localStorage.setItem('token', data.token)
    localStorage.setItem('role', data.role)
    localStorage.setItem('nickname', data.nickname)
  }

  function logout() {
    token.value = ''
    role.value = ''
    nickname.value = ''
    localStorage.removeItem('token')
    localStorage.removeItem('role')
    localStorage.removeItem('nickname')
  }

  return { token, role, nickname, isLogin, isAdmin, setLogin, logout }
})
