<template>
  <div class="patient-root p-reg">
    <!-- 顶栏：病历抬头式 -->
    <header class="p-topbar">
      <div class="p-topbar__title">模拟挂号</div>
      <div class="p-topbar__sub">就 诊 科 室 · 选 定</div>
    </header>

    <main class="p-reg__body">
      <!-- 成功态（register_success） -->
      <section v-if="done" class="p-card p-reg__done">
        <div class="p-eyebrow">挂 号 成 功</div>
        <div class="p-card__dept">{{ selected.name }}</div>
        <p class="p-card__note">
          已为您登记就诊科室。请携此信息前往门诊导医台取号。
        </p>
        <div class="p-card__foot">
          <span class="p-eyebrow">就 诊 信 息</span>
          <p class="p-cite"><span class="p-cite__no">科室</span>　{{ selected.name }}</p>
          <p class="p-cite"><span class="p-cite__no">位置</span>　{{ selected.location }}</p>
          <p class="p-cite"><span class="p-cite__no">状态</span>　register_success</p>
        </div>
        <button class="p-btn p-btn--ghost" style="margin-top: 14px" @click="backToChat">
          返回继续咨询
        </button>
      </section>

      <!-- 选科室态 -->
      <template v-else>
        <div class="p-eyebrow" style="margin-bottom: 10px">科 室 列 表 · 全 部 启 用</div>
        <div class="p-deptlist">
          <button
            v-for="d in depts"
            :key="d.name"
            class="p-dept"
            :class="{ 'p-dept--rec': d.name === recDept, 'p-dept--on': d.name === selected.name }"
            @click="selected = d"
          >
            <span>
              {{ d.name }}
              <span class="p-cite" style="display: block; font-size: 11.5px">{{ d.location }}</span>
            </span>
            <span v-if="d.name === recDept" class="p-dept__hint">推 荐</span>
          </button>
        </div>

        <button class="p-btn" style="margin-top: 18px" :disabled="!selected.name" @click="confirm">
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
import { ref, computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import '../../styles/patient.css'

const route = useRoute()
const router = useRouter()

const recDept = computed(() => route.query.dept || '心血管内科')

// —— 假数据：dept 表（停用科室不进本列表）——
const depts = [
  { name: '心血管内科', location: '门诊楼 3F 东区' },
  { name: '呼吸内科', location: '门诊楼 3F 西区' },
  { name: '消化内科', location: '门诊楼 4F 东区' },
  { name: '骨科', location: '门诊楼 2F 东区' },
  { name: '神经内科', location: '门诊楼 4F 西区' },
  { name: '皮肤科', location: '门诊楼 5F 南区' }
]

const selected = ref(depts.find((d) => d.name === recDept.value) || { name: '', location: '' })
const done = ref(false)

function confirm() {
  // 真实实现：POST 挂号确认 → register_success 埋点 + guide_record 同事务写入
  done.value = true
}

function backToChat() {
  router.push('/')
}
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
</style>
