<template>
  <section class="a-panel">
    <div class="a-tabs" style="margin-bottom: 16px">
      <button
        class="a-tab"
        :class="{ 'a-tab--on': tab === 'user' }"
        @click="switchTab('user')"
      >
        账号管理
      </button>
      <button
        class="a-tab"
        :class="{ 'a-tab--on': tab === 'word' }"
        @click="switchTab('word')"
      >
        敏感词库
      </button>

      <!-- 敏感词库顶部操作 -->
      <div v-if="tab === 'word'" class="a-table__ops" style="margin-left: auto; align-self: center">
        <el-button type="primary" size="small" @click="openAdd">添加词条</el-button>
        <el-button size="small" @click="openImport">批量导入</el-button>
        <el-button size="small" @click="exportWords">导出备份</el-button>
      </div>
    </div>

    <!-- 账号管理 -->
    <template v-if="tab === 'user'">
      <div class="a-table__ops" style="margin-bottom: 12px">
        <el-input
          v-model="userQuery"
          placeholder="用户名 / 昵称搜索"
          clearable
          style="width: 220px"
          @keyup.enter="loadUsers(1)"
        />
        <el-button size="small" @click="loadUsers(1)">搜索</el-button>
      </div>

      <el-table :data="users" v-loading="userLoading" empty-text="无匹配账号">
        <el-table-column prop="username" label="用户名" min-width="120" />
        <el-table-column prop="nickname" label="昵称" min-width="100" />
        <el-table-column label="角色" width="90">
          <template #default="{ row }">
            <el-tag :type="row.role === 'admin' ? 'primary' : 'info'" effect="plain" size="small">
              {{ row.role }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="90">
          <template #default="{ row }">
            <el-tag :type="row.status === 'normal' ? 'success' : 'warning'" size="small">
              {{ row.status === 'normal' ? '正常' : '已封禁' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="注册时间" width="110">
          <template #default="{ row }">{{ fmtDate(row.createdAt) }}</template>
        </el-table-column>
        <el-table-column label="操作" width="90">
          <template #default="{ row }">
            <el-button
              :type="row.status === 'normal' ? 'danger' : 'primary'"
              link
              size="small"
              @click="toggleBan(row)"
            >
              {{ row.status === 'normal' ? '封禁' : '解封' }}
            </el-button>
          </template>
        </el-table-column>
      </el-table>

      <el-pagination
        v-model:current-page="userPage.current"
        :page-size="userPage.size"
        :total="userPage.total"
        layout="total, prev, pager, next"
        background
        @current-change="loadUsers"
      />
    </template>

    <!-- 敏感词库 -->
    <template v-else>
      <el-table :data="words" v-loading="wordLoading" empty-text="词库为空">
        <el-table-column prop="word" label="词" min-width="140" />
        <el-table-column label="类型" width="90">
          <template #default="{ row }">
            <el-tag :type="row.type === 'banned' ? 'warning' : 'info'" size="small">
              {{ row.type === 'banned' ? '禁止词' : '观察词' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="hitCount" label="命中次数" width="90" />
        <el-table-column label="状态" width="90">
          <template #default="{ row }">
            <el-tag :type="row.enabled ? 'success' : 'info'" effect="plain" size="small">
              {{ row.enabled ? '启用中' : '已停用' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" min-width="180">
          <template #default="{ row }">
            <el-button link size="small" @click="onToggle(row)">
              {{ row.enabled ? '停用' : '启用' }}
            </el-button>
            <el-button v-if="row.type === 'watch'" type="primary" link size="small" @click="onConvert(row)">
              转禁止词
            </el-button>
            <el-button type="danger" link size="small" @click="onDelete(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>

      <el-pagination
        v-model:current-page="wordPage.current"
        :page-size="wordPage.size"
        :total="wordPage.total"
        layout="total, prev, pager, next"
        background
        @current-change="loadWords"
      />
    </template>

    <!-- 添加词条弹窗 -->
    <el-dialog v-model="addVisible" title="添加词条" width="420px">
      <el-form label-width="72px">
        <el-form-item label="敏感词">
          <el-input v-model="wordForm.word" placeholder="输入敏感词" @keyup.enter="submitAdd" />
        </el-form-item>
        <el-form-item label="类型">
          <el-select v-model="wordForm.type" style="width: 100%">
            <el-option label="禁止词（命中即拦截）" value="banned" />
            <el-option label="观察词（命中只记录）" value="watch" />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="addVisible = false">取消</el-button>
        <el-button type="primary" @click="submitAdd">保存</el-button>
      </template>
    </el-dialog>

    <!-- 批量导入弹窗 -->
    <el-dialog v-model="importVisible" title="批量导入" width="480px">
      <p style="font-size: 12.5px; color: var(--ink-2); margin-bottom: 10px">
        一行一词，自动去空行、去重、跳过已存在词条。
      </p>
      <el-input
        v-model="importText"
        type="textarea"
        :rows="7"
        placeholder="粘贴 txt 内容，一行一词"
      />
      <el-form label-width="72px" style="margin-top: 12px">
        <el-form-item label="导入类型">
          <el-select v-model="importType" style="width: 100%">
            <el-option label="禁止词（命中即拦截）" value="banned" />
            <el-option label="观察词（命中只记录）" value="watch" />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="importVisible = false">取消</el-button>
        <el-button type="primary" @click="submitImport">导入</el-button>
      </template>
    </el-dialog>
  </section>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  pageUsers, banUser, unbanUser,
  pageWords, addWord, importWords, toggleWord, convertWordToBanned, deleteWord
} from '../../api/admin'

const tab = ref('user')

function switchTab(t) {
  tab.value = t
}

function fmtDate(s) {
  return s ? String(s).slice(0, 10) : ''
}

function errText(e) {
  return e?.message || '操作失败，请稍后重试'
}

// —— 账号管理 ——
const users = ref([])
const userLoading = ref(false)
const userPage = reactive({ current: 1, size: 10, total: 0 })
const userQuery = ref('')

async function loadUsers(pageNo = userPage.current) {
  userLoading.value = true
  try {
    const data = await pageUsers({ current: pageNo, size: userPage.size, keyword: userQuery.value || undefined })
    users.value = data.records
    userPage.current = data.current
    userPage.total = data.total
  } catch (e) {
    ElMessage.error(errText(e))
  } finally {
    userLoading.value = false
  }
}

async function toggleBan(u) {
  const banning = u.status === 'normal'
  try {
    if (banning) {
      await ElMessageBox.confirm(`封禁后该账号将无法登录，确认封禁「${u.username}」？`, '封禁账号', {
        confirmButtonText: '封禁',
        cancelButtonText: '取消',
        type: 'warning'
      })
      await banUser(u.id)
      u.status = 'banned'
      ElMessage.success(`已封禁 ${u.username}（登录时即被拒绝）`)
    } else {
      await unbanUser(u.id)
      u.status = 'normal'
      ElMessage.success(`已解封 ${u.username}`)
    }
  } catch (e) {
    if (e !== 'cancel' && e !== 'close') ElMessage.error(errText(e))
  }
}

// —— 敏感词库 ——
const words = ref([])
const wordLoading = ref(false)
const wordPage = reactive({ current: 1, size: 10, total: 0 })
const addVisible = ref(false)
const importVisible = ref(false)
const wordForm = reactive({ word: '', type: 'banned' })
const importText = ref('')
const importType = ref('banned')

function openAdd() {
  wordForm.word = ''
  wordForm.type = 'banned'
  addVisible.value = true
}

function openImport() {
  importText.value = ''
  importType.value = 'banned'
  importVisible.value = true
}

async function loadWords(pageNo = wordPage.current) {
  wordLoading.value = true
  try {
    const data = await pageWords({ current: pageNo, size: wordPage.size })
    words.value = data.records
    wordPage.current = data.current
    wordPage.total = data.total
  } catch (e) {
    ElMessage.error(errText(e))
  } finally {
    wordLoading.value = false
  }
}

async function submitAdd() {
  if (!wordForm.word.trim()) {
    ElMessage.warning('请输入敏感词')
    return
  }
  try {
    await addWord({ word: wordForm.word.trim(), type: wordForm.type })
    ElMessage.success('词条已添加')
    addVisible.value = false
    loadWords(1)
  } catch (e) {
    ElMessage.error(errText(e))
  }
}

async function submitImport() {
  if (!importText.value.trim()) {
    ElMessage.warning('请粘贴要导入的内容')
    return
  }
  try {
    const r = await importWords({ text: importText.value, type: importType.value })
    ElMessage.success(`导入 ${r.imported} 条，跳过重复 ${r.skipped} 条`)
    importVisible.value = false
    loadWords(1)
  } catch (e) {
    ElMessage.error(errText(e))
  }
}

async function onToggle(w) {
  try {
    await toggleWord(w.id)
    w.enabled = w.enabled ? 0 : 1
    ElMessage.success(w.enabled ? '已启用' : '已停用（停用词不参与入口校验）')
  } catch (e) {
    ElMessage.error(errText(e))
  }
}

async function onConvert(w) {
  try {
    await convertWordToBanned(w.id)
    w.type = 'banned'
    ElMessage.success('已转为禁止词')
  } catch (e) {
    ElMessage.error(errText(e))
  }
}

async function onDelete(w) {
  try {
    await ElMessageBox.confirm(`删除敏感词「${w.word}」？`, '删除词条', {
      confirmButtonText: '删除',
      cancelButtonText: '取消',
      type: 'warning'
    })
    await deleteWord(w.id)
    ElMessage.success('已删除')
    loadWords(wordPage.current)
  } catch (e) {
    if (e !== 'cancel' && e !== 'close') ElMessage.error(errText(e))
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
    ElMessage.success(`已导出 ${data.records.length} 条`)
  } catch (e) {
    ElMessage.error(errText(e))
  }
}

onMounted(() => {
  loadUsers(1)
  loadWords(1)
})
</script>
