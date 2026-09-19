package com.guide.common.util;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch.core.BulkRequest;
import co.elastic.clients.elasticsearch.core.search.Hit;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.guide.common.config.ElasticsearchProperties;
import com.guide.common.model.ChunkHit;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * ES chunk 索引读写工具（管全文检索与术语聚合）。
 * 写入方：kb（唯一写入口，与 pgvector 双写）；读取方：rag（BM25 关键词召回）。
 * 中文分词用 analysis-ik（索引 ik_max_word / 检索 ik_smart）。
 * 可用性策略：ES 不可用时不抛异常打断主链路——召回退化为空列表（由 rag 侧融合兜底）。
 */
@Slf4j
@Component
public class EsChunkUtil {

    private final ElasticsearchClient client;
    private final ElasticsearchProperties properties;

    public EsChunkUtil(ElasticsearchClient client, ElasticsearchProperties properties) {
        this.client = client;
        this.properties = properties;
    }

    /** 建索引（不存在时），含中文分词 mapping；幂等 */
    public void ensureIndex() {
        String index = properties.getChunkIndex();
        try {
            boolean exists = client.indices().exists(e -> e.index(index)).value();
            if (exists) {
                return;
            }
            client.indices().create(c -> c.index(index).mappings(m -> m
                    .properties("chunk_id", p -> p.keyword(k -> k))
                    .properties("dept_id", p -> p.keyword(k -> k))
                    .properties("title", p -> p.text(t -> t.analyzer("ik_max_word").searchAnalyzer("ik_smart")))
                    .properties("content", p -> p.text(t -> t.analyzer("ik_max_word").searchAnalyzer("ik_smart")))
                    .properties("medical_terms", p -> p.keyword(k -> k))));
            log.info("ES 索引已创建：{}", index);
        } catch (IOException | RuntimeException e) {
            log.warn("ES 索引创建失败（{}）：{}", index, e.getMessage());
        }
    }

    /** 批量写入/覆盖 chunk（与 pgvector 同事务边界内的第二路） */
    public void indexChunks(List<ChunkDoc> docs) {
        if (docs.isEmpty()) {
            return;
        }
        String index = properties.getChunkIndex();
        try {
            BulkRequest.Builder builder = new BulkRequest.Builder();
            for (ChunkDoc doc : docs) {
                builder.operations(op -> op.index(idx -> idx.index(index).id(doc.chunkId()).document(doc)));
            }
            var response = client.bulk(builder.build());
            if (response.errors()) {
                log.warn("ES 批量写入部分失败，index={}", index);
            }
        } catch (IOException | RuntimeException e) {
            throw new IllegalStateException("ES 写入失败：" + e.getMessage(), e);
        }
    }

    /** 删除 chunk（删除补偿顺序：先删 ES → 再删向量 → 再删元数据） */
    public void deleteChunk(String chunkId) {
        try {
            client.delete(d -> d.index(properties.getChunkIndex()).id(chunkId));
        } catch (IOException | RuntimeException e) {
            throw new IllegalStateException("ES 删除失败：" + e.getMessage(), e);
        }
    }

    /**
     * BM25 关键词召回。ES 不可用/索引不存在时返回空列表（rag 侧退化为单路召回）。
     */
    public List<ChunkHit> searchChunks(String query, int topK) {
        try {
            var response = client.search(s -> s
                            .index(properties.getChunkIndex())
                            .size(topK)
                            .query(q -> q.multiMatch(m -> m.query(query).fields("title^2", "content"))),
                    ChunkDoc.class);
            List<ChunkHit> hits = new ArrayList<>();
            for (Hit<ChunkDoc> hit : response.hits().hits()) {
                ChunkDoc doc = hit.source();
                if (doc == null) {
                    continue;
                }
                hits.add(new ChunkHit(doc.chunkId(), doc.deptId(), doc.title(), doc.content(),
                        hit.score() == null ? 0d : hit.score()));
            }
            return hits;
        } catch (IOException | RuntimeException e) {
            log.warn("ES 召回失败，本次退化为单路召回：{}", e.getMessage());
            return List.of();
        }
    }

    /** ES chunk 文档（字段名与索引 mapping 对齐） */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record ChunkDoc(
            @JsonProperty("chunk_id") String chunkId,
            @JsonProperty("dept_id") String deptId,
            @JsonProperty("title") String title,
            @JsonProperty("content") String content,
            @JsonProperty("medical_terms") List<String> medicalTerms) {
    }
}
