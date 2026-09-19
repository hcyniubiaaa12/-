<template>
  <nav v-if="pages > 1" class="a-pager">
    <span>第 {{ current }} / {{ pages }} 页</span>
    <button class="a-pager__btn" :disabled="current <= 1" @click="$emit('go', current - 1)">上一页</button>
    <button class="a-pager__btn" :disabled="current >= pages" @click="$emit('go', current + 1)">下一页</button>
  </nav>
</template>

<script setup>
// 通用分页器：父组件传 current/pages，@go(pageNo) 触发加载
defineProps({
  current: { type: Number, default: 1 },
  pages: { type: Number, default: 1 }
})
defineEmits(['go'])
</script>
