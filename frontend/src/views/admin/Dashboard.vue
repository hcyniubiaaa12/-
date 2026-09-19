<template>
  <!-- KPI 指标卡：环形 + 标签/数值/同比 -->
  <section class="a-kpis">
    <div v-for="k in kpis" :key="k.label" class="a-kpi">
      <div class="a-kpi__ring" :class="{ 'a-kpi__ring--warn': k.warn }">
        <svg width="56" height="56" viewBox="0 0 56 56">
          <circle cx="28" cy="28" r="24" fill="none" stroke="#EDF1F9" stroke-width="5" />
          <circle
            cx="28"
            cy="28"
            r="24"
            fill="none"
            :stroke="k.warn ? '#E06B4D' : '#4468B8'"
            stroke-width="5"
            stroke-linecap="round"
            :stroke-dasharray="`${(k.pct * 150.8).toFixed(1)} 150.8`"
          />
        </svg>
        <span class="a-kpi__ring-num">{{ k.ring }}</span>
      </div>
      <div>
        <div class="a-kpi__label">{{ k.label }}</div>
        <div class="a-kpi__value">{{ k.value }}</div>
        <div class="a-kpi__yoy" :class="k.trend">{{ k.yoy }}</div>
      </div>
    </div>
  </section>

  <!-- 双列图表面板 -->
  <section class="a-grid">
    <!-- 柱状趋势：蓝=命中 / coral=未命中 堆叠 -->
    <div class="a-panel">
      <div class="a-panel__head">
        <span class="a-panel__title">每日导诊量 · 命中构成</span>
        <span class="a-legend">
          <span><i class="a-legend__dot" style="background: #4468B8" />命中</span>
          <span><i class="a-legend__dot" style="background: #E06B4D" />未命中</span>
        </span>
      </div>
      <div class="a-chart">
        <div v-for="d in trend" :key="d.day" class="a-chart__col">
          <div class="a-chart__stack">
            <div class="a-chart__miss" :style="{ height: d.miss + '%' }" />
            <div class="a-chart__hit" :style="{ height: d.hit + '%' }" />
          </div>
          <div class="a-chart__label">{{ d.day }}</div>
        </div>
      </div>
    </div>

    <!-- 分布条：错误根因分布 -->
    <div class="a-panel">
      <div class="a-panel__head">
        <span class="a-panel__title">错误根因分布</span>
        <span class="a-panel__hint">按最新归因</span>
      </div>
      <div class="a-dist">
        <div v-for="r in rootCauses" :key="r.name" class="a-dist__row">
          <span class="a-dist__name">{{ r.name }}</span>
          <span class="a-dist__track">
            <span class="a-dist__fill" :style="{ width: r.pct + '%' }" />
          </span>
          <span class="a-dist__pct">{{ r.pct }}%</span>
        </div>
      </div>
    </div>
  </section>

  <!-- 全宽表格：最近导诊记录 -->
  <section class="a-panel">
    <div class="a-panel__head">
      <span class="a-panel__title">最近导诊记录</span>
      <span class="a-panel__hint">共 {{ records.length }} 条</span>
    </div>
    <table class="a-table">
      <thead>
        <tr>
          <th>时间</th>
          <th>主诉摘要</th>
          <th>推荐科室</th>
          <th>置信度</th>
          <th>实际科室</th>
          <th>结果</th>
        </tr>
      </thead>
      <tbody>
        <tr v-for="r in records" :key="r.time">
          <td>{{ r.time }}</td>
          <td>{{ r.symptom }}</td>
          <td>{{ r.rec }}</td>
          <td>{{ r.conf }}</td>
          <td>{{ r.actual || '—' }}</td>
          <td>
            <span class="a-tag" :class="r.tagClass">{{ r.result }}</span>
          </td>
        </tr>
      </tbody>
    </table>
  </section>
</template>

<script setup>
// —— 假数据：看板指标与趋势 ——
const kpis = [
  { label: 'Top-1 命中率', value: '78.4%', ring: '78%', pct: 0.784, yoy: '↑ 2.1% 环比', trend: 'a-kpi__yoy--up' },
  { label: 'Top-3 命中率', value: '91.2%', ring: '91%', pct: 0.912, yoy: '↑ 1.4% 环比', trend: 'a-kpi__yoy--up' },
  { label: '待审核', value: '3 条', ring: '3', pct: 0.3, warn: true, yoy: '↑ 新增 2 条', trend: 'a-kpi__yoy--down' },
  { label: '知识盲区', value: '7 条', ring: '7', pct: 0.42, warn: true, yoy: '— 持平', trend: '' }
]

const trend = [
  { day: '周一', hit: 62, miss: 8 },
  { day: '周二', hit: 71, miss: 6 },
  { day: '周三', hit: 58, miss: 11 },
  { day: '周四', hit: 76, miss: 5 },
  { day: '周五', hit: 68, miss: 9 },
  { day: '周六', hit: 44, miss: 6 },
  { day: '周日', hit: 38, miss: 4 }
]

const rootCauses = [
  { name: '检索失败', pct: 38 },
  { name: '切分破碎', pct: 21 },
  { name: '模型未依据检索', pct: 17 },
  { name: '局部映射缺失', pct: 13 },
  { name: '解析遗漏', pct: 8 },
  { name: '结构化失败', pct: 3 }
]

const records = [
  { time: '09-18 14:22', symptom: '胸口闷，爬楼加重', rec: '心血管内科', conf: '82%', actual: '心血管内科', result: 'top-1 命中', tagClass: 'a-tag--ok' },
  { time: '09-18 13:40', symptom: '右膝蹲起疼痛', rec: '骨科', conf: '76%', actual: '骨科', result: 'top-1 命中', tagClass: 'a-tag--ok' },
  { time: '09-18 11:05', symptom: '半夜反酸烧心', rec: '消化内科', conf: '64%', actual: '呼吸内科', result: 'top-3 命中', tagClass: '' },
  { time: '09-18 10:31', symptom: '太阳穴跳痛', rec: '神经内科', conf: '58%', actual: '心血管内科', result: '未命中', tagClass: 'a-tag--warn' },
  { time: '09-17 17:12', symptom: '手臂红疹发痒', rec: '皮肤科', conf: '88%', actual: '皮肤科', result: 'top-1 命中', tagClass: 'a-tag--ok' }
]
</script>
