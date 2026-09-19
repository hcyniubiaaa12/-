package com.guide.llm.client;

import java.util.List;

/**
 * 向量模型接口（LLM 适配层唯一出口之一）：阿里 text-embedding-v3。
 * 写入方 kb（入库流水线 / 回流合成 chunk）、读取方 rag（查询向量化）。
 */
public interface EmbeddingModel {

    /** 单条文本向量化 */
    float[] embed(String text);

    /** 批量文本向量化（顺序与入参一致；内部按批大小自动分批） */
    List<float[]> embedBatch(List<String> texts);
}
