<template>
  <div class="admin-root">
    <div class="a-shell">
      <!-- 侧栏：蓝色实底，选中态深蓝，待办 coral 药丸徽标 -->
      <aside class="a-side">
        <div class="a-side__brand">
          智能导诊 · 控制台
          <small>OPERATIONS CONSOLE</small>
        </div>
        <nav class="a-nav">
          <router-link
            v-for="item in navs"
            :key="item.path"
            :to="item.path"
            class="a-nav__item"
            :class="{ 'a-nav__item--on': route.path.startsWith(item.path) }"
          >
            {{ item.label }}
            <span v-if="item.badge" class="a-nav__badge">{{ item.badge }}</span>
          </router-link>
        </nav>
      </aside>

      <!-- 主区 -->
      <div class="a-main">
        <header class="a-topbar">
          <div>
            <div class="a-topbar__title">{{ route.meta.title || '控制台' }}</div>
            <div class="a-topbar__sub">{{ route.meta.sub || '' }}</div>
          </div>
          <div class="a-topbar__ops">
            <button class="a-btn a-btn--ghost">近 7 天</button>
            <span class="a-topbar__user">{{ user.nickname || '管理员' }}</span>
          </div>
        </header>

        <div class="a-content">
          <router-view />
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { useRoute } from 'vue-router'
import { useUserStore } from '../../stores/user'
import '../../styles/admin.css'

const route = useRoute()
const user = useUserStore()

// 待办徽标由审核队列实时数据驱动（当前为模板假数据）
const navs = [
  { path: '/admin/dashboard', label: '数据看板', badge: 0 },
  { path: '/admin/kb', label: '知识库管理', badge: 0 },
  { path: '/admin/review', label: '审核队列', badge: 3 },
  { path: '/admin/users', label: '用户管理', badge: 0 },
  { path: '/admin/llm', label: 'LLM 配置', badge: 0 }
]
</script>
