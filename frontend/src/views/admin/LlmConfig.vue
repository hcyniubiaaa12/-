<template>
  <section class="a-grid" style="grid-template-columns: 1fr 1fr">
    <!-- 模型接入 -->
    <div class="a-panel">
      <div class="a-panel__head">
        <span class="a-panel__title">模型接入</span>
        <span class="a-panel__hint">密钥存 application-local.yml，不进库</span>
      </div>

      <div class="a-field">
        <label class="a-field__label">对话模型（DeepSeek）</label>
        <input class="a-field__input" :value="cfg.deepseek.model" readonly />
        <div class="a-field__hint">
          {{ cfg.deepseek.baseUrl }} ·
          <span class="a-tag" :class="cfg.deepseek.keySet ? 'a-tag--ok' : 'a-tag--warn'">
            {{ cfg.deepseek.keySet ? '密钥已配置' : '密钥未配置' }}
          </span>
        </div>
      </div>

      <div class="a-field">
        <label class="a-field__label">向量模型（阿里 text-embedding-v3）</label>
        <input class="a-field__input" :value="cfg.embedding.model" readonly />
        <div class="a-field__hint">
          维度 {{ cfg.embedding.dim }} · 与 pgvector 表结构一致，换模型需重建向量
        </div>
      </div>

      <div class="a-field">
        <label class="a-field__label">重排模型（阿里 gte-rerank-v2）</label>
        <input class="a-field__input" :value="cfg.rerank.model" readonly />
        <div class="a-field__hint">检索链路：召回 → RRF 融合 → 重排 Top-N</div>
      </div>

      <button class="a-btn">连通性测试</button>
    </div>

    <!-- 检索参数（sys_config 运行时字典表） -->
    <div class="a-panel">
      <div class="a-panel__head">
        <span class="a-panel__title">检索与聚合参数</span>
        <span class="a-panel__hint">存 sys_config，管理端可调</span>
      </div>

      <div class="a-field">
        <label class="a-field__label">向量召回 Top-K</label>
        <input v-model="cfg.retrieve.topK" class="a-field__input" type="number" />
        <div class="a-field__hint">pgvector 与 ES 各召回条数</div>
      </div>

      <div class="a-field">
        <label class="a-field__label">重排后 Top-N</label>
        <input v-model="cfg.retrieve.topN" class="a-field__input" type="number" />
        <div class="a-field__hint">送入 Prompt 的切片数</div>
      </div>

      <div class="a-field">
        <label class="a-field__label">聚合归桶相似度阈值</label>
        <input v-model="cfg.aggregate.threshold" class="a-field__input" type="number" step="0.01" />
        <div class="a-field__hint">语义归桶余弦相似度下限（默认 0.85）</div>
      </div>

      <div class="a-field">
        <label class="a-field__label">低置信度分流阈值</label>
        <input v-model="cfg.aggregate.lowConfidence" class="a-field__input" type="number" step="0.01" />
        <div class="a-field__hint">低于此值不进聚合，进知识盲区榜（默认 0.5）</div>
      </div>

      <div class="a-field">
        <label class="a-field__label">术语人工审核开关</label>
        <div class="a-chips" style="margin: 0">
          <button class="a-chip" :class="{ 'a-chip--on': cfg.term.manualReview }" @click="cfg.term.manualReview = true">
            开启
          </button>
          <button class="a-chip" :class="{ 'a-chip--on': !cfg.term.manualReview }" @click="cfg.term.manualReview = false">
            关闭
          </button>
        </div>
      </div>

      <button class="a-btn" @click="toast = true">保存配置</button>
    </div>

    <div v-if="toast" class="a-toast" @click="toast = false">配置已保存（模板演示）</div>
  </section>
</template>

<script setup>
import { ref } from 'vue'

const toast = ref(false)

// —— 假数据：模型配置 + sys_config 参数 ——
const cfg = ref({
  deepseek: { model: 'deepseek-chat', baseUrl: 'https://api.deepseek.com', keySet: true },
  embedding: { model: 'text-embedding-v3', dim: 1024 },
  rerank: { model: 'gte-rerank-v2' },
  retrieve: { topK: 20, topN: 5 },
  aggregate: { threshold: 0.85, lowConfidence: 0.5 },
  term: { manualReview: true }
})
</script>
