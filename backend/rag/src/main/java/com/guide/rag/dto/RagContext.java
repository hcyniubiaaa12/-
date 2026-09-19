package com.guide.rag.dto;

import com.guide.common.model.ChunkHit;

import java.util.List;

/**
 * RAG 检索产出：送入 Prompt 的精排片段 + 证据快照素材。
 *
 * @param query          原始查询
 * @param rewrittenQuery 改写后查询（未改写时等于原查询）
 * @param chunks         精排后 Top-N 片段，顺序即注号顺序（注1 = chunks[0]）
 * @param vectorHits     向量路召回条数（诊断用）
 * @param esHits         关键词路召回条数（诊断用）
 */
public record RagContext(
        String query,
        String rewrittenQuery,
        List<ChunkHit> chunks,
        int vectorHits,
        int esHits) {

    public boolean isEmpty() {
        return chunks.isEmpty();
    }
}
