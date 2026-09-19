import http from './http'

// 管理端接口（/api/admin/**，仅 ROLE_ADMIN）

// —— 用户管理 ——
export function pageUsers(params) {
  return http.get('/admin/users', { params })
}

export function banUser(id) {
  return http.post(`/admin/users/${id}/ban`)
}

export function unbanUser(id) {
  return http.post(`/admin/users/${id}/unban`)
}

// —— 敏感词库 ——
export function pageWords(params) {
  return http.get('/admin/sensitive-words', { params })
}

export function addWord(data) {
  return http.post('/admin/sensitive-words', data)
}

// 批量导入：text 一行一词，返回 { imported, skipped }
export function importWords(data) {
  return http.post('/admin/sensitive-words/import', data)
}

export function toggleWord(id) {
  return http.post(`/admin/sensitive-words/${id}/toggle`)
}

export function convertWordToBanned(id) {
  return http.post(`/admin/sensitive-words/${id}/to-banned`)
}

export function deleteWord(id) {
  return http.delete(`/admin/sensitive-words/${id}`)
}
