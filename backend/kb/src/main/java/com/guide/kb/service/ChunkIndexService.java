package com.guide.kb.service;

import com.guide.common.util.EsChunkUtil;
import com.guide.common.util.PgVectorUtil;
import com.guide.kb.dto.ChunkInput;
import com.guide.kb.entity.KbChunk;
import com.guide.kb.mapper.KbChunkMapper;
import com.guide.llm.client.EmbeddingModel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

/**
 * chunk 入库服务：kb 是**唯一**写向量库与 ES chunk 索引的入口（上传流水线 + 回流同步 + 种子语料）。
 * 双写边界：MySQL 事实 + pgvector 向量 + ES 全文，同一方法内顺序写入（MySQL → 向量 → ES），
 * MySQL 侧失败整体回滚；向量/ES 失败向上抛出触发回滚，遗留的半写由删除补偿顺序收敛
 * （先删 ES → 再删向量 → 再删元数据，见链路 B 对齐点三层防线）。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ChunkIndexService {

    private final KbChunkMapper chunkMapper;
    private final EmbeddingModel embeddingModel;
    private final PgVectorUtil pgVectorUtil;
    private final EsChunkUtil esChunkUtil;

    /**
     * 批量入库：向量化一次批调（摊薄成本），再逐条写 MySQL + pgvector + ES。
     *
     * @param docId  所属文档 id（回流合成 chunk 指向系统级「回流知识文档」容器）
     * @param deptId 所属科室 id（向量召回过滤与推荐校验用）
     */
    @Transactional(rollbackFor = Exception.class)
    public List<KbChunk> indexChunks(String docId, String deptId, List<ChunkInput> inputs) {
        if (inputs == null || inputs.isEmpty()) {
            return List.of();
        }
        List<String> texts = inputs.stream().map(this::embeddingText).toList();
        List<float[]> vectors = embeddingModel.embedBatch(texts);
        if (vectors.size() != inputs.size()) {
            throw new IllegalStateException("向量条数与切片数不一致：" + vectors.size() + "/" + inputs.size());
        }

        List<KbChunk> saved = new ArrayList<>(inputs.size());
        List<EsChunkUtil.ChunkDoc> docs = new ArrayList<>(inputs.size());
        for (int i = 0; i < inputs.size(); i++) {
            ChunkInput input = inputs.get(i);
            KbChunk chunk = new KbChunk();
            chunk.setDocId(docId);
            chunk.setTitle(input.title());
            chunk.setContent(input.content());
            chunk.setSeq(input.seq());
            chunkMapper.insert(chunk);

            pgVectorUtil.upsertChunkVector(chunk.getId(), deptId, vectors.get(i));
            docs.add(new EsChunkUtil.ChunkDoc(chunk.getId(), deptId, input.title(), input.content(), input.terms()));
            saved.add(chunk);
        }
        esChunkUtil.indexChunks(docs);
        log.info("chunk 入库完成：docId={} deptId={} 条数={}", docId, deptId, saved.size());
        return saved;
    }

    /** 删除补偿：先删 ES → 再删向量 → 再删元数据（顺序保证一致性收敛） */
    @Transactional(rollbackFor = Exception.class)
    public void deleteChunk(String chunkId) {
        esChunkUtil.deleteChunk(chunkId);
        pgVectorUtil.deleteChunkVector(chunkId);
        chunkMapper.deleteById(chunkId);
    }

    /** 向量化文本：标题 + 正文（标题也在语义里，提升召回质量） */
    private String embeddingText(ChunkInput input) {
        String title = input.title() == null ? "" : input.title();
        return title.isEmpty() ? input.content() : title + "。" + input.content();
    }
}
