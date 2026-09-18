<template>
  <div class="admin-layout">
    <aside class="sidebar">
      <nav>
        <router-link to="/admin/dashboard">看板</router-link>
        <router-link to="/admin/kb">知识库</router-link>
        <router-link to="/admin/review">审核</router-link>
        <router-link to="/admin/llm">LLM 配置</router-link>
        <router-link to="/admin/users">用户管理</router-link>
      </nav>
    </aside>
    <main class="content">
      <router-view />
    </main>
  </div>
</template>

<script setup>
</script>

<style scoped>
.admin-layout {
  height: 100%;
  display: flex;
}
.sidebar {
  width: 212px;
  background: #3b82c4;
}
.sidebar nav {
  display: flex;
  flex-direction: column;
}
.sidebar a {
  color: #fff;
  text-decoration: none;
  padding: 14px 20px;
  font-size: 13px;
}
.content {
  flex: 1;
  padding: 24px 28px;
}
</style>
