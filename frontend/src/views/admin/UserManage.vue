<template>
  <section class="a-panel">
    <div class="a-tabs" style="margin-bottom: 16px">
      <button
        class="a-tab"
        :class="{ 'a-tab--on': tab === 'user' }"
        @click="tab = 'user'"
      >
        账号管理
      </button>
      <button
        class="a-tab"
        :class="{ 'a-tab--on': tab === 'word' }"
        @click="tab = 'word'"
      >
        敏感词库
      </button>

      <!-- 敏感词库顶部操作 -->
      <div v-if="tab === 'word'" class="a-table__ops" style="margin-left: auto; align-self: center">
        <button class="a-btn" @click="wordPanel = wordPanel === 'add' ? '' : 'add'">添加词条</button>
        <button class="a-btn a-btn--ghost" @click="wordPanel = wordPanel === 'import' ? '' : 'import'">批量导入</button>
        <button class="a-btn a-btn--ghost" @click="exportWords">导出备份</button>
      </div>
    </div>

    <!-- 账号管理 -->
    <template v-if="tab === 'user'">
      <div class="a-table__ops" style="margin-bottom: 12px">
        <input
          v-model="userQuery"
          class="a-input"
          placeholder="用户名 / 昵称搜索"
          @keydown.enter="loadUsers(1)"
        />
        <button class="a-btn a-btn--ghost" @click="loadUsers(1)">搜索</button>
      </div>

      <table class="a-table">
        <thead>
          <tr>
            <th>用户名</th>
            <th>昵称</th>
            <th>角色</th>
            <th>状态</th>
            <th>注册时间</th>
            <th>操作</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="u in users" :key="u.id">
            <td>{{ u.username }}</td>
            <td>{{ u.nickname }}</td>
            <td>
              <span class="a-tag" :class="u.role === 'admin' ? '' : 'a-tag--plain'">{{ u.role }}</span>
            </td>
            <td>
              <span class="a-tag" :class="u.status === 'normal' ? 'a-tag--ok' : 'a-tag--warn'">
                {{ u.status === 'normal' ? '正常' : '已封禁' }}
              </span>
            </td>
            <td>{{ fmtDate(u.createdAt) }}</td>
            <td>
              <div class="a-table__ops">
                <button
                  class="a-btn a-btn--ghost"
                  :class="{ 'a-btn--danger': u.status === 'normal' }"
                  @click="toggleBan(u)"
                >
                  {{ u.status === 'normal' ? '封禁' : '解封' }}
                </button>
              </div>
            </td>
          </tr>
          <tr v-if="!users.length">
            <td colspan="6" style="text-align: center; color: var(--a-ink-2, #64748b)">无匹配账号</td>
          </tr>
        </tbody>
      </table>
      <Pager :current="userPage.current" :pages="userPage.pages" @go="loadUsers" />
    </template>

    <!-- 敏感词库 -->
    <template v-else>
      <!-- 添加词条（行内面板） -->
      <div v-if="wordPanel === 'add'" class="a-inline-panel">
        <input v-model="wordForm.word" class="a-input" placeholder="敏感词" @keydown.enter="submitAdd" />
        <select v-model="wordForm.type" class="a-input">
          <option value="banned">禁止词</option>
          <option value="watch">观察词</option>
        </select>
        <button class="a-btn" @click="submitAdd">保存</button>
        <button class="a-btn a-btn--ghost" @click="wordPanel = ''">取消</button>
      </div>

      <!-- 批量导入（行内面板：txt 一行一词，自动去重） -->
      <div v-if="wordPanel === 'import'" class="a-inline-panel" style="flex-direction: column; align-items: stretch">
        <textarea
          v-model="importText"
          class="a-input"
          rows="6"
          placeholder="一行一词，粘贴 txt 内容即可；自动去空行、去重、跳过已存在"
        />
        <div class="a-table__ops" style="margin-top: 8px">
          <select v-model="wordForm.type" class="a-input">
            <option value="banned">导入为禁止词</option>
            <option value="watch">导入为观察词</option>
          </select>
          <button class="a-btn" @click="submitImport">导入</button>
          <button class="a-btn a-btn--ghost" @click="wordPanel = ''">取消</button>
        </div>
      </div>

      <table class="a-table">
        <thead>
          <tr>
            <th>词</th>
            <th>类型</th>
            <th>命中次数</th>
            <th>状态</th>
            <th>操作</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="w in words" :key="w.id">
            <td>{{ w.word }}</td>
            <td>
              <span class="a-tag" :class="w.type === 'banned' ? 'a-tag--warn' : ''">
                {{ w.type === 'banned' ? '禁止词' : '观察词' }}
              </span>
            </td>
            <td>{{ w.hitCount }}</td>
            <td>
              <span class="a-tag" :class="w.enabled ? 'a-tag--ok' : 'a-tag--plain'">
                {{ w.enabled ? '启用中' : '已停用' }}
              </span>
            </td>
            <td>
              <div class="a-table__ops">
                <button class="a-btn a-btn--ghost" @click="onToggle(w)">{{ w.enabled ? '停用' : '启用' }}</button>
                <button v-if="w.type === 'watch'" class="a-btn" @click="onConvert(w)">转禁止词</button>
                <button class="a-btn a-btn--danger" @click="onDelete(w)">删除</button>
              </div>
            </td>
          </tr>
          <tr v-if="!words.length">
            <td colspan="5" style="text-align: center; color: var(--a-ink-2, #64748b)">词库为空</td>
          </tr>
        </tbody>
      </table>
      <Pager :current="wordPage.current" :pages="wordPage.pages" @go="loadWords" />
    </template>

    <!-- 提示条（与 Review 页同款 a-toast） -->
    <div v-if="toast" class="a-toast">{{ toast }}</div>
  </section>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import {
  pageUsers, banUser, unbanUser,
  pageWords, addWord, importWords, toggleWord, convertWordToBanned, deleteWord
} from '../../api/admin'
import Pager from './Pager.vue'

const tab = ref('user')
const toast = ref('')
let toastTimer
function showToast(msg) {
  toast.value = msg
  clearTimeout(toastTimer)
  toastTimer = setTimeout(() => (toast.value = ''), 2200)
}

function fmtDate(s) {
  return s ? String(s).slice(0, 10) : ''
}

function errText(e) {
  return e?.message || '操作失败，请稍后重试'
}

// —— 账号管理 ——
const users = ref([])
const userPage = reactive({ current: 1, pages: 1 })
const userQuery = ref('')

async function loadUsers(pageNo = userPage.current) {
  try {
    const data = await pageUsers({ current: pageNo, size: 10, keyword: userQuery.value || undefined })
    users.value = data.records
    userPage.current = data.current
    userPage.pages = data.pages
  } catch (e) {
    showToast(errText(e))
  }
}

async function toggleBan(u) {
  try {
    if (u.status === 'normal') {
      await banUser(u.id)
      u.status = 'banned'
      showToast(`已封禁 ${u.username}（登录时即被拒绝）`)
    } else {
      await unbanUser(u.id)
      u.status = 'normal'
      showToast(`已解封 ${u.username}`)
    }
  } catch (e) {
    showToast(errText(e))
  }
}

// —— 敏感词库 ——
const words = ref([])
const wordPage = reactive({ current: 1, pages: 1 })
const wordPanel = ref('') // '' | 'add' | 'import'
const wordForm = reactive({ word: '', type: 'banned' })
const importText = ref('')

async function loadWords(pageNo = wordPage.current) {
  try {
    const data = await pageWords({ current: pageNo, size: 10 })
    words.value = data.records
    wordPage.current = data.current
    wordPage.pages = data.pages
  } catch (e) {
    showToast(errText(e))
  }
}

async function submitAdd() {
  if (!wordForm.word.trim()) {
    showToast('请输入敏感词')
    return
  }
  try {
    await addWord({ word: wordForm.word.trim(), type: wordForm.type })
    showToast('词条已添加')
    wordForm.word = ''
    wordPanel.value = ''
    loadWords(1)
  } catch (e) {
    showToast(errText(e))
  }
}

async function submitImport() {
  if (!importText.value.trim()) {
    showToast('请粘贴要导入的内容')
    return
  }
  try {
    const r = await importWords({ text: importText.value, type: wordForm.type })
    showToast(`导入 ${r.imported} 条，跳过重复 ${r.skipped} 条`)
    importText.value = ''
    wordPanel.value = ''
    loadWords(1)
  } catch (e) {
    showToast(errText(e))
  }
}

async function onToggle(w) {
  try {
    await toggleWord(w.id)
    w.enabled = w.enabled ? 0 : 1
    showToast(w.enabled ? '已启用' : '已停用（停用词不参与入口校验）')
  } catch (e) {
    showToast(errText(e))
  }
}

async function onConvert(w) {
  try {
    await convertWordToBanned(w.id)
    w.type = 'banned'
    showToast('已转为禁止词')
  } catch (e) {
    showToast(errText(e))
  }
}

async function onDelete(w) {
  if (!confirm(`删除敏感词「${w.word}」？`)) return
  try {
    await deleteWord(w.id)
    showToast('已删除')
    loadWords(wordPage.current)
  } catch (e) {
    showToast(errText(e))
  }
}

// 导出备份：拉全量生成 txt 下载（总体架构 6.2 备份要求）
async function exportWords() {
  try {
    const data = await pageWords({ current: 1, size: 9999 })
    const lines = data.records.map((w) => w.word).join('\n')
    const blob = new Blob([lines], { type: 'text/plain;charset=utf-8' })
    const a = document.createElement('a')
    a.href = URL.createObjectURL(blob)
    a.download = 'sensitive-words-backup.txt'
    a.click()
    URL.revokeObjectURL(a.href)
    showToast(`已导出 ${data.records.length} 条`)
  } catch (e) {
    showToast(errText(e))
  }
}

onMounted(() => {
  loadUsers(1)
  loadWords(1)
})
</script>
