<template>
  <section class="a-panel">
    <div class="a-panel__head">
      <span class="a-panel__title">待审核 · 错误模式聚合桶</span>
      <span class="a-panel__hint">点击行展开证据快照与根因归因</span>
    </div>

    <table class="a-table">
      <thead>
        <tr>
          <th>错误方向（推荐 → 实际）</th>
          <th>代表样本</th>
          <th>累计次数</th>
          <th>状态</th>
          <th>操作</th>
        </tr>
      </thead>
      <tbody>
        <template v-for="b in buckets" :key="b.id">
          <tr style="cursor: pointer" @click="toggle(b.id)">
            <td>{{ b.direction }}</td>
            <td>{{ b.sample }}</td>
            <td>{{ b.count }}</td>
            <td>
              <span class="a-tag" :class="b.tagClass">{{ b.status }}</span>
            </td>
            <td>
              <div class="a-table__ops" @click.stop>
                <button class="a-btn" @click="toggle(b.id)">审核</button>
                <button class="a-btn a-btn--ghost">忽略</button>
              </div>
            </td>
          </tr>

          <!-- 展开子行：证据快照 + 根因多选 -->
          <tr v-if="open === b.id" class="a-subrow">
            <td colspan="5">
              <div style="padding: 6px 2px 12px">
                <div class="a-eyebrow" style="margin-bottom: 6px">证据快照</div>
                <div class="ev">
                  <div v-for="(e, i) in b.evidence" :key="i" class="ev__row">
                    <span class="ev__no">{{ e.rank }}</span>
                    <span class="ev__title">{{ e.title }}</span>
                    <span class="ev__score">{{ e.score }}</span>
                  </div>
                </div>
                <p class="a-panel__hint" style="margin-top: 8px">
                  模型原始输出：{{ b.rawOutput }}
                </p>

                <div class="a-eyebrow" style="margin: 14px 0 0">根因归因（多选）</div>
                <div class="a-chips">
                  <button
                    v-for="c in causes"
                    :key="c"
                    class="a-chip"
                    :class="{ 'a-chip--on': picked.includes(c) }"
                    @click="pick(c)"
                  >
                    {{ c }}
                  </button>
                </div>

                <div class="a-table__ops">
                  <button class="a-btn">回流知识库（approve）</button>
                  <button class="a-btn a-btn--ghost">保存归因</button>
                  <button class="a-btn a-btn--ghost">驳回</button>
                </div>
              </div>
            </td>
          </tr>
        </template>
      </tbody>
    </table>
  </section>
</template>

<script setup>
import { ref } from 'vue'

const open = ref(null)
const picked = ref([])

// 根因选项清单：前端硬编码（预定义 7 项，后端原样存取小写 key）
const causes = ['切分破碎', '解析遗漏', '检索失败', '局部映射缺失', '模型未依据检索', '结构化失败', '患者挂错']

// —— 假数据：聚合桶（方向 + 锚点 + 证据） ——
const buckets = [
  {
    id: 'b1',
    direction: '神经内科 → 心血管内科',
    sample: '太阳穴跳痛，伴心慌、活动后加重',
    count: 4,
    status: 'pending',
    tagClass: 'a-tag--warn',
    rawOutput: '{"dept":"神经内科","confidence":0.58}',
    evidence: [
      { rank: 1, title: '神经内科科室介绍', score: '0.71' },
      { rank: 2, title: '偏头痛的典型表现', score: '0.66' },
      { rank: 3, title: '心源性头痛鉴别', score: '0.62' }
    ]
  },
  {
    id: 'b2',
    direction: '消化内科 → 呼吸内科',
    sample: '半夜反酸烧心，平躺加重',
    count: 3,
    status: 'pending',
    tagClass: 'a-tag--warn',
    rawOutput: '{"dept":"消化内科","confidence":0.64}',
    evidence: [
      { rank: 1, title: '胃食管反流病诊疗', score: '0.74' },
      { rank: 2, title: '夜间咳嗽的鉴别', score: '0.61' }
    ]
  },
  {
    id: 'b3',
    direction: '骨科 → 神经内科',
    sample: '颈肩僵硬伴手指放射性麻木',
    count: 3,
    status: 'approved',
    tagClass: 'a-tag--ok',
    rawOutput: '{"dept":"骨科","confidence":0.69}',
    evidence: [{ rank: 1, title: '颈椎病分型', score: '0.78' }]
  }
]

function toggle(id) {
  open.value = open.value === id ? null : id
  picked.value = []
}

function pick(c) {
  const i = picked.value.indexOf(c)
  i >= 0 ? picked.value.splice(i, 1) : picked.value.push(c)
}
</script>

<style scoped>
.a-eyebrow {
  font-size: 11px;
  letter-spacing: .14em;
  color: var(--ink-2);
}
.ev { display: flex; flex-direction: column; gap: 4px; }
.ev__row { display: flex; gap: 10px; font-size: 12px; }
.ev__no {
  width: 18px;
  color: var(--blue);
  font-weight: 600;
}
.ev__title { flex: 1; }
.ev__score { color: var(--ink-2); }
</style>
