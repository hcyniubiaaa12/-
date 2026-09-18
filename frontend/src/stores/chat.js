import { defineStore } from 'pinia'
import { ref } from 'vue'

// 会话状态 store：当前会话、消息列表、SSE 流式状态
export const useChatStore = defineStore('chat', () => {
  const sessionId = ref('')
  const messages = ref([])
  const streaming = ref(false)

  function reset() {
    sessionId.value = ''
    messages.value = []
    streaming.value = false
  }

  function addMessage(msg) {
    messages.value.push(msg)
  }

  return { sessionId, messages, streaming, reset, addMessage }
})
