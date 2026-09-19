<template>
  <div class="patient-root p-reg">
    <!-- 顶栏：病历抬头式 -->
    <header class="p-topbar">
      <div class="p-topbar__title">模拟挂号</div>
      <div class="p-topbar__sub">就 诊 科 室 · 选 定</div>
    </header>

    <main class="p-reg__body">
      <!-- 成功态（register_success；埋点由后端挂号接口补记） -->
      <section v-if="done" class="p-card p-reg__done">
        <div class="p-eyebrow">挂 号 成 功</div>
        <div class="p-card__dept">{{ booked.deptName }}</div>
        <p class="p-card__note">
          已为您登记就诊科室。请携此信息前往门诊导医台取号。
        </p>
        <div class="p-card__foot">
          <span class="p-eyebrow">就 诊 信 息</span>
          <p class="p-cite"><span class="p-cite__no">科室</span>　{{ booked.deptName }}</p>
          <p class="p-cite"><span class="p-cite__no">位置</span>　{{ booked.location }}</p>
          <p class="p-cite"><span class="p-cite__no">状态</span>　register_success</p>
        </div>
        <button class="p-btn p-btn--ghost" style="margin-top: 14px" @click="backToChat">
          返回继续咨询
        </button>
      </section>

      <!-- 选科室态 -->
      <template v-else>
        <!-- 缺少导诊记录（直接访问本页）／加载或挂号失败：说清原因与下一步 -->
        <div v-if="!recordId" class="p-error p-reg__tip">
          缺少导诊记录，请先在对话页完成一次分诊再来挂号。
          <button class="p-error__retry" @click="backToChat">返回对话页</button>
        </div>
        <div v-else-if="error" class="p-error p-reg__tip">
          {{ error.message }}
          <button v-if="error.action === 'reload'" class="p-error__retry" @click="loadDepts">
            重新加载
          </button>
        </div>

        <div class="p-eyebrow" style="margin-bottom: 10px">科 室 列 表 · 全 部 启 用</div>

        <p v-if="loading" class="p-empty">正在加载科室…</p>
        <p v-else-if="!depts.length" class="p-empty">暂无可挂号科室，请稍后重试</p>
        <div v-else class="p-deptlist">
          <button
            v-for="d in depts"
            :key="d.id"
            class="p-dept"
            :class="{ 'p-dept--rec': d.id === recDeptId, 'p-dept--on': d.id === selected?.id }"
            @click="pick(d)"
          >
            <span>
              {{ d.name }}
              <span class="p-cite p-dept__loc">{{ d.location }}</span>
            </span>
            <span v-if="d.id === recDeptId" class="p-dept__hint">推 荐</span>
          </button>
        </div>

        <button
          class="p-btn"
          style="margin-top: 18px"
          :disabled="!selected || submitting || !recordId"
          @click="confirm"
        >
          确认挂号
        </button>
      </template>
    </main>

    <!-- 步进流程条 -->
    <nav class="p-steps">
      <span class="p-steps__item p-steps__item--done"><span class="p-steps__no">1</span>导诊结论</span>
      <span class="p-steps__link" />
      <span class="p-steps__item" :class="done ? 'p-steps__item--done' : 'p-steps__item--now'">
        <span class="p-steps__no">2</span>模拟挂号
      </span>
      <span class="p-steps__link" />
      <span class="p-steps__item" :class="{ 'p-steps__item--done': done, 'p-steps__item--now': !done }">
        <span class="p-steps__no">3</span>确认完成
      </span>
    </nav>
  </div>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { listDepts, confirmRegister } from '../../api/chat'
import { track } from '../../utils/track'
import '../../styles/patient.css'

const route = useRoute()
const router = useRouter()

// recordId 是挂号与埋点的唯一凭证：由对话页结论卡跳转时带上
const recordId = route.query.recordId || ''
// 推荐科室 id：结论卡带来的 top1 科室，列表内 teal 实心高亮 + 「推 荐」标记
const recDeptId = route.query.deptId || ''

const depts = ref([])
const selected = ref(null)
const loading = ref(false)
const submitting = ref(false)
const booked = ref(null)
const done = ref(false)
const error = ref(null) // { message, action: 'reload' | '' }

// sim_register 触点：同一次访问里每个科室只上报一次，反复切换不刷量
const trackedDeptIds = new Set()

async function loadDepts() {
  loading.value = true
  error.value = null
  try {
    depts.value = await listDepts()
    // 推荐科室默认选中；科室已停用（不在启用列表）则不预选，由患者自行选择
    selected.value = depts.value.find((d) => d.id === recDeptId) || null
  } catch (e) {
    depts.value = []
    selected.value = null
    error.value = { message: e.message || '科室列表加载失败，请重新加载', action: 'reload' }
  } finally {
    loading.value = false
  }
}

function pick(dept) {
  selected.value = dept
  if (recordId && !trackedDeptIds.has(dept.id)) {
    trackedDeptIds.add(dept.id)
    track('sim_register', { recordId, deptId: dept.id })
  }
}

async function confirm() {
  if (!selected.value || submitting.value || !recordId) return
  submitting.value = true
  error.value = null
  try {
    // 成功 = 写 actual_dept / 命中标记 / 会话置 closed；register_success 埋点由后端补记
    booked.value = await confirmRegister({ recordId, deptId: selected.value.id })
    done.value = true
  } catch (e) {
    error.value = { message: e.message || '挂号失败，请稍后重试', action: '' }
  } finally {
    submitting.value = false
  }
}

function backToChat() {
  router.push('/')
}

onMounted(loadDepts)
</script>

<style scoped>
.p-reg {
  display: flex;
  flex-direction: column;
  height: 100vh;
}
.p-reg__body {
  flex: 1;
  overflow-y: auto;
  padding: 18px 16px;
}
.p-reg__done .p-card__dept { margin-top: 2px; }
/* 页面内提示：与科室列表保持 12px 间距，不挤在一起 */
.p-reg__tip { margin-bottom: 12px; }
.p-dept__loc { display: block; font-size: 11.5px; }
/* 选中态实心 teal 底上，位置文字改用半透明白，保证可读 */
.p-dept--on .p-dept__loc { color: rgba(255, 255, 255, .82); }
/* 确认按钮禁用态（未选科室 / 提交中 / 缺记录） */
.p-reg .p-btn:disabled {
  background: var(--line);
  color: var(--ink-2);
  cursor: not-allowed;
}
</style>
