<template>
  <div class="patient-root p-chat">
    <!-- 顶栏：病历抬头式 -->
    <header class="p-topbar">
      <div class="p-topbar__title">智能导诊</div>
      <div class="p-topbar__sub">分 诊 台 · 在 线</div>
    </header>

    <!-- 对话区 -->
    <main class="p-thread p-chat__thread">
      <template v-for="(m, i) in messages" :key="i">
        <!-- 用户消息：实心 teal 气泡 -->
        <div v-if="m.role === 'user'" class="p-user">{{ m.content }}</div>

        <!-- 追问消息：与普通回复同款 -->
        <div v-else-if="m.role === 'question'" class="p-ai">
          <div class="p-ai__tag">分 诊 助 理 · 追 问</div>
          <div class="p-ai__text p-ask">{{ m.content }}</div>
        </div>

        <!-- AI 回复：直排文字 + moss 小标签 -->
        <div v-else class="p-ai">
          <div class="p-ai__tag">分 诊 助 理</div>
          <div class="p-ai__text">{{ m.content }}</div>
        </div>
      </template>

      <!-- 推荐卡（签名元素 · 链路 A 结论单） -->
      <section v-if="result" class="p-card">
        <div class="p-card__head">
          <div>
            <div class="p-eyebrow">分 诊 结 论</div>
            <div class="p-card__dept">{{ result.dept }}</div>
          </div>
          <div class="p-card__conf" :class="{ 'p-card__conf--low': result.confidence < 0.5 }">
            {{ (result.confidence * 100).toFixed(0) }}%
          </div>
        </div>

        <!-- 置信度条列表 Top3 -->
        <div class="p-bars">
          <div
            v-for="(c, i) in result.top3"
            :key="c.name"
            class="p-bar"
            :class="{ 'p-bar--top': i === 0 }"
          >
            <span class="p-bar__name">{{ c.name }}</span>
            <span class="p-bar__track"><span class="p-bar__fill" :style="{ width: c.pct + '%' }" /></span>
            <span class="p-bar__pct">{{ c.pct }}%</span>
          </div>
        </div>

        <p class="p-card__note">{{ result.note }}</p>

        <!-- 脚注区：判断依据（1px dashed 上边框） -->
        <div class="p-card__foot">
          <span class="p-eyebrow">判 断 依 据</span>
          <p v-for="c in result.cites" :key="c.no" class="p-cite">
            <span class="p-cite__no">注{{ c.no }}</span>　{{ c.text }}
          </p>
        </div>

        <p v-if="result.confidence < 0.5" class="p-card__lowhint">
          信息有限，结果仅供参考，建议进一步咨询医生。
        </p>

        <button class="p-btn" style="margin-top: 14px" @click="goRegister">
          下一步 · 模拟挂号
        </button>
      </section>

      <!-- SSE error 态示例（line 描边块） -->
      <div v-if="showError" class="p-error">
        网络中断，请检查连接后重试。
        <button class="p-error__retry" @click="showError = false">重新发送</button>
      </div>
    </main>

    <!-- 步进流程条：① 导诊结论 → ② 模拟挂号 → ③ 确认完成 -->
    <nav class="p-steps">
      <span class="p-steps__item p-steps__item--now"><span class="p-steps__no">1</span>导诊结论</span>
      <span class="p-steps__link" />
      <span class="p-steps__item"><span class="p-steps__no">2</span>模拟挂号</span>
      <span class="p-steps__link" />
      <span class="p-steps__item"><span class="p-steps__no">3</span>确认完成</span>
    </nav>

    <!-- 输入区 -->
    <footer class="p-composer">
      <textarea
        v-model="draft"
        class="p-composer__input"
        rows="1"
        placeholder="说说哪里不舒服，我来帮您分诊"
        @keydown.enter.exact.prevent="send"
      />
      <button class="p-composer__send" @click="send">发送</button>
    </footer>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import '../../styles/patient.css'

const router = useRouter()

// —— 假数据：对话流 ——
const messages = ref([
  { role: 'user', content: '最近胸口闷，爬楼的时候特别明显，有点喘' },
  {
    role: 'question',
    content: '这个胸闷大概持续多久了？是压榨样的疼痛，还是单纯发闷？有没有向左肩或手臂放射？'
  },
  { role: 'user', content: '大概一周了，就是发闷，不疼，也没有放射' }
])

// —— 假数据：推荐卡（链路 A result 事件结构）——
const result = ref({
  dept: '心血管内科',
  confidence: 0.82,
  top3: [
    { name: '心血管内科', pct: 82 },
    { name: '呼吸内科', pct: 11 },
    { name: '消化内科', pct: 7 }
  ],
  note: '胸闷在活动后加重、休息可缓解，是心肌供血不足的典型表现，建议优先排查心脏来源。',
  cites: [
    { no: 1, text: '心血管内科科室介绍 · 劳力性胸闷的鉴别要点' },
    { no: 2, text: '胸痛鉴别手册 · 心源性胸痛的典型与非典型表现' }
  ]
})

const draft = ref('')
const showError = ref(false)

// 假交互：追加用户消息 + 模拟回复（真实实现走 SSE 四态）
function send() {
  const text = draft.value.trim()
  if (!text) return
  messages.value.push({ role: 'user', content: text })
  draft.value = ''
  setTimeout(() => {
    messages.value.push({
      role: 'ai',
      content: '已记录您补充的信息，正在结合知识库重新评估，请稍候。'
    })
  }, 900)
}

function goRegister() {
  router.push({ path: '/register', query: { dept: result.value.dept } })
}
</script>

<style scoped>
.p-chat {
  display: flex;
  flex-direction: column;
  height: 100vh;
}
.p-chat__thread {
  flex: 1;
  overflow-y: auto;
  padding-bottom: 18px;
}
</style>
