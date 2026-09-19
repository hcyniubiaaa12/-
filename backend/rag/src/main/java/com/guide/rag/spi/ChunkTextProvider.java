package com.guide.rag.spi;

import java.util.Collection;
import java.util.Map;

/**
 * 切片正文读取端口（rag 定义、kb 实现——依赖倒置）。
 *
 * <p>为什么需要：pgvector 只存向量（管语义），切片标题与正文在 MySQL（管事实），
 * 跨库不能 JOIN。召回拿到 id 后由本端口批量回填正文，rag 因而不依赖任何业务模块
 * （依赖方向仍是 业务模块 → rag）。
 */
public interface ChunkTextProvider {

    /**
     * 批量读取切片正文。
     *
     * @param chunkIds 切片 id 集合
     * @return chunkId → 切片正文；已删除或不存在的 id 不出现在结果里（rag 侧据此丢弃脏向量）
     */
    Map<String, ChunkText> loadTexts(Collection<String> chunkIds);

    /** 切片正文（标题用于溯源引用） */
    record ChunkText(String title, String content) {
    }
}
