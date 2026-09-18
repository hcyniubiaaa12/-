<template>
  <section class="a-panel">
    <div class="a-tabs" style="margin-bottom: 16px">
      <button
        v-for="t in tabs"
        :key="t.key"
        class="a-tab"
        :class="{ 'a-tab--on': tab === t.key }"
        @click="tab = t.key"
      >
        {{ t.label }}
      </button>
      <button class="a-btn" style="margin-left: auto; align-self: center">上传文档</button>
    </div>

    <!-- 科室蓝本 -->
    <table v-if="tab === 'dept'" class="a-table">
      <thead>
        <tr>
          <th>科室</th>
          <th>位置</th>
          <th>切片数</th>
          <th>状态</th>
          <th>操作</th>
        </tr>
      </thead>
      <tbody>
        <tr v-for="d in depts" :key="d.name">
          <td>{{ d.name }}</td>
          <td>{{ d.location }}</td>
          <td>{{ d.chunks }}</td>
          <td>
            <span class="a-tag" :class="d.enabled ? 'a-tag--ok' : 'a-tag--plain'">
              {{ d.enabled ? '启用中' : '已停用' }}
            </span>
          </td>
          <td>
            <div class="a-table__ops">
              <button class="a-btn a-btn--ghost">编辑</button>
              <button class="a-btn a-btn--ghost">{{ d.enabled ? '停用' : '启用' }}</button>
            </div>
          </td>
        </tr>
      </tbody>
    </table>

    <!-- 文档 -->
    <table v-else-if="tab === 'doc'" class="a-table">
      <thead>
        <tr>
          <th>文档标题</th>
          <th>所属科室</th>
          <th>入库进度</th>
          <th>状态</th>
          <th>操作</th>
        </tr>
      </thead>
      <tbody>
        <tr v-for="doc in docs" :key="doc.title">
          <td>{{ doc.title }}</td>
          <td>{{ doc.dept }}</td>
          <td style="width: 180px">
            <div class="a-progress">
              <div
                class="a-progress__fill"
                :class="doc.status === 'failed' ? 'a-progress__fill--warn' : doc.status === 'done' ? 'a-progress__fill--ok' : ''"
                :style="{ width: doc.pct + '%' }"
              />
            </div>
            <span class="a-panel__hint">{{ doc.done }} / {{ doc.total }}</span>
          </td>
          <td>
            <span class="a-tag" :class="doc.tagClass">{{ doc.statusText }}</span>
          </td>
          <td>
            <div class="a-table__ops">
              <button class="a-btn a-btn--ghost">查看切片</button>
              <button v-if="doc.status === 'failed'" class="a-btn a-btn--ghost">重新处理</button>
              <button class="a-btn a-btn--danger">删除</button>
            </div>
          </td>
        </tr>
      </tbody>
    </table>

    <!-- 症状交叉映射台账 -->
    <table v-else-if="tab === 'map'" class="a-table">
      <thead>
        <tr>
          <th>症状</th>
          <th>主科室</th>
          <th>交叉科室</th>
          <th>来源</th>
        </tr>
      </thead>
      <tbody>
        <tr v-for="m in mappings" :key="m.symptom">
          <td>{{ m.symptom }}</td>
          <td>{{ m.main }}</td>
          <td>{{ m.cross }}</td>
          <td>
            <span class="a-tag" :class="m.source === 'feedback' ? 'a-tag--ok' : 'a-tag--plain'">
              {{ m.source }}
            </span>
          </td>
        </tr>
      </tbody>
    </table>

    <!-- 术语白名单 -->
    <table v-else class="a-table">
      <thead>
        <tr>
          <th>术语</th>
          <th>类型</th>
          <th>来源</th>
          <th>状态</th>
          <th>操作</th>
        </tr>
      </thead>
      <tbody>
        <tr v-for="t in terms" :key="t.term">
          <td>{{ t.term }}</td>
          <td>
            <span class="a-tag">{{ t.type }}</span>
          </td>
          <td>{{ t.source }}</td>
          <td>
            <span class="a-tag" :class="t.enabled ? 'a-tag--ok' : 'a-tag--warn'">
              {{ t.enabled ? '已生效' : '待审核' }}
            </span>
          </td>
          <td>
            <div class="a-table__ops">
              <button v-if="!t.enabled" class="a-btn">确认启用</button>
              <button class="a-btn a-btn--danger">停用</button>
            </div>
          </td>
        </tr>
      </tbody>
    </table>
  </section>
</template>

<script setup>
import { ref } from 'vue'

const tabs = [
  { key: 'dept', label: '科室蓝本' },
  { key: 'doc', label: '文档入库' },
  { key: 'map', label: '映射台账' },
  { key: 'term', label: '术语白名单' }
]
const tab = ref('dept')

// —— 假数据 ——
const depts = [
  { name: '心血管内科', location: '门诊楼 3F 东区', chunks: 42, enabled: true },
  { name: '呼吸内科', location: '门诊楼 3F 西区', chunks: 36, enabled: true },
  { name: '消化内科', location: '门诊楼 4F 东区', chunks: 51, enabled: true },
  { name: '骨科', location: '门诊楼 2F 东区', chunks: 38, enabled: true },
  { name: '神经内科', location: '门诊楼 4F 西区', chunks: 29, enabled: true },
  { name: '皮肤科', location: '门诊楼 5F 南区', chunks: 24, enabled: false }
]

const docs = [
  { title: '心血管内科诊疗规范.docx', dept: '心血管内科', done: 42, total: 42, pct: 100, status: 'done', statusText: '已完成', tagClass: 'a-tag--ok' },
  { title: '胸痛鉴别手册.pdf', dept: '心血管内科', done: 28, total: 28, pct: 100, status: 'done', statusText: '已完成', tagClass: 'a-tag--ok' },
  { title: '骨关节疾病分诊指南.docx', dept: '骨科', done: 15, total: 38, pct: 39, status: 'parsing', statusText: '解析中', tagClass: '' },
  { title: '皮肤科常见病图鉴.pdf', dept: '皮肤科', done: 0, total: 0, pct: 0, status: 'failed', statusText: '解析失败', tagClass: 'a-tag--warn' }
]

const mappings = [
  { symptom: '颈肩僵硬伴手指麻木', main: '骨科', cross: '神经内科', source: 'feedback' },
  { symptom: '反酸烧心', main: '消化内科', cross: '心血管内科', source: 'feedback' },
  { symptom: '太阳穴跳痛', main: '神经内科', cross: '心血管内科', source: 'manual' },
  { symptom: '活动后胸闷', main: '心血管内科', cross: '呼吸内科', source: 'init' }
]

const terms = [
  { term: '胸', type: 'part', source: 'llm_extract', enabled: true },
  { term: '胸闷', type: 'symptom', source: 'llm_extract', enabled: true },
  { term: '放射性麻木', type: 'symptom', source: 'llm_extract', enabled: false },
  { term: '反酸', type: 'symptom', source: 'manual', enabled: true }
]
</script>
