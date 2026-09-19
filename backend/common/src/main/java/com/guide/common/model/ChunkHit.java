package com.guide.common.model;

/**
 * 检索命中的知识片段（pgvector 向量召回 / ES 关键词召回的公共载体）。
 * rag 层做 RRF 融合与重排都基于本类型，不感知底层存储。
 *
 * @param chunkId 片段 id（对应 MySQL kb_chunk.id）
 * @param deptId  所属科室 id
 * @param title   片段标题（溯源引用用）
 * @param content 片段正文
 * @param score   本路召回的分数（向量=余弦相似度，ES=BM25 相关度；仅用于排序/展示，跨路不可比）
 */
public record ChunkHit(String chunkId, String deptId, String title, String content, double score) {
}
