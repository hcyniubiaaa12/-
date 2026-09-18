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
        <button class="a-btn">添加词条</button>
        <button class="a-btn a-btn--ghost">批量导入</button>
        <button class="a-btn a-btn--ghost">导出备份</button>
      </div>
    </div>

    <!-- 账号管理 -->
    <table v-if="tab === 'user'" class="a-table">
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
        <tr v-for="u in users" :key="u.username">
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
          <td>{{ u.createdAt }}</td>
          <td>
            <div class="a-table__ops">
              <button class="a-btn a-btn--ghost">{{ u.status === 'normal' ? '封禁' : '解封' }}</button>
            </div>
          </td>
        </tr>
      </tbody>
    </table>

    <!-- 敏感词库 -->
    <table v-else class="a-table">
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
        <tr v-for="w in words" :key="w.word">
          <td>{{ w.word }}</td>
          <td>
            <span class="a-tag" :class="w.type === 'banned' ? 'a-tag--warn' : ''">
              {{ w.type === 'banned' ? '禁止词' : '观察词' }}
            </span>
          </td>
          <td>{{ w.hit }}</td>
          <td>
            <span class="a-tag" :class="w.enabled ? 'a-tag--ok' : 'a-tag--plain'">
              {{ w.enabled ? '启用中' : '已停用' }}
            </span>
          </td>
          <td>
            <div class="a-table__ops">
              <button class="a-btn a-btn--ghost">编辑</button>
              <button class="a-btn a-btn--ghost">{{ w.enabled ? '停用' : '启用' }}</button>
              <button v-if="w.type === 'watch'" class="a-btn">转禁止词</button>
              <button class="a-btn a-btn--danger">删除</button>
            </div>
          </td>
        </tr>
      </tbody>
    </table>
  </section>
</template>

<script setup>
import { ref } from 'vue'

const tab = ref('user')

// —— 假数据（枚举值取英文小写，见《数据库设计.md》§0）——
const users = [
  { username: 'admin', nickname: '系统管理员', role: 'admin', status: 'normal', createdAt: '2026-09-01' },
  { username: 'zhangsan', nickname: '张三', role: 'patient', status: 'normal', createdAt: '2026-09-10' },
  { username: 'lisi', nickname: '李四', role: 'patient', status: 'banned', createdAt: '2026-09-12' },
  { username: 'wangwu', nickname: '王五', role: 'patient', status: 'normal', createdAt: '2026-09-15' }
]

const words = [
  { word: '滚', type: 'banned', hit: 12, enabled: true },
  { word: '骗人的医院', type: 'banned', hit: 3, enabled: true },
  { word: '投诉', type: 'watch', hit: 27, enabled: true },
  { word: '退钱', type: 'watch', hit: 8, enabled: false }
]
</script>
