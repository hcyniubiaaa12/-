package com.guide.common.util;

import com.guide.common.api.ErrorCode;
import com.guide.common.exception.BizException;
import com.guide.common.model.ChunkHit;
import com.guide.common.config.PgVectorConfig;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * pgvector 读写工具（管语义）。
 * 写入方：kb（chunk 向量，唯一 RAG 语料入口）、feedback（聚类锚点向量）；
 * 读取方：rag（向量召回）、feedback（语义归桶）。
 * 表结构见《数据库设计.md》4.1：kb_chunk_vec / cluster_bucket_vec，余弦距离 HNSW 索引。
 */
@Slf4j
@Component
public class PgVectorUtil {

    private final JdbcTemplate jdbcTemplate;

    public PgVectorUtil(@Qualifier(PgVectorConfig.PG_VECTOR_JDBC_TEMPLATE) JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    /** 写入/覆盖 chunk 向量（回流重入、重新入库走同一条 upsert） */
    public void upsertChunkVector(String chunkId, String deptId, float[] embedding) {
        jdbcTemplate.update("""
                INSERT INTO kb_chunk_vec (chunk_id, dept_id, embedding)
                VALUES (?, ?, CAST(? AS vector))
                ON CONFLICT (chunk_id) DO UPDATE SET dept_id = EXCLUDED.dept_id, embedding = EXCLUDED.embedding
                """, chunkId, deptId, toLiteral(embedding));
    }

    /** 删除 chunk 向量（删除补偿：先删 ES → 再删向量 → 再删元数据） */
    public void deleteChunkVector(String chunkId) {
        jdbcTemplate.update("DELETE FROM kb_chunk_vec WHERE chunk_id = ?", chunkId);
    }

    /**
     * 向量召回：余弦相似度 Top-K。停用科室不做过滤——chunk 留库可召回，
     * 但绝不入推荐（科室校验在 chat 层按 enabled 拦截，见链路 A 对齐点）。
     *
     * <p>只返回 id / 科室 / 分数：切片正文在 MySQL（管事实），跨库不能 JOIN，
     * 由 rag 层融合后经 ChunkTextProvider 端口批量回填标题与正文。
     */
    public List<ChunkHit> searchChunks(float[] queryVector, int topK) {
        String literal = toLiteral(queryVector);
        return jdbcTemplate.query("""
                        SELECT chunk_id, dept_id, 1 - (embedding <=> CAST(? AS vector)) AS score
                        FROM kb_chunk_vec
                        ORDER BY embedding <=> CAST(? AS vector)
                        LIMIT ?
                        """,
                (rs, rowNum) -> new ChunkHit(
                        rs.getString("chunk_id"),
                        rs.getString("dept_id"),
                        null,
                        null,
                        rs.getDouble("score")),
                literal, literal, topK);
    }

    /** 写入聚类锚点向量（feedback 直写，开新桶时调用） */
    public void upsertBucketVector(String bucketId, String recDeptId, String actualDeptId, float[] embedding) {
        jdbcTemplate.update("""
                INSERT INTO cluster_bucket_vec (bucket_id, rec_dept_id, actual_dept_id, embedding)
                VALUES (?, ?, ?, CAST(? AS vector))
                ON CONFLICT (bucket_id) DO UPDATE SET embedding = EXCLUDED.embedding
                """, bucketId, recDeptId, actualDeptId, toLiteral(embedding));
    }

    /**
     * 语义归桶：在【同方向】候选桶内找余弦相似度最高者（方向精确字段先锁死，歧义空间小）。
     *
     * @return 命中的桶 id 与相似度；无候选桶返回 null
     */
    public BucketHit searchBucketByDirection(String recDeptId, String actualDeptId, float[] queryVector) {
        List<BucketHit> hits = jdbcTemplate.query("""
                        SELECT bucket_id, 1 - (embedding <=> CAST(? AS vector)) AS score
                        FROM cluster_bucket_vec
                        WHERE rec_dept_id = ? AND actual_dept_id = ?
                        ORDER BY embedding <=> CAST(? AS vector)
                        LIMIT 1
                        """,
                (rs, rowNum) -> new BucketHit(rs.getString("bucket_id"), rs.getDouble("score")),
                toLiteral(queryVector), recDeptId, actualDeptId, toLiteral(queryVector));
        return hits.isEmpty() ? null : hits.get(0);
    }

    /** 向量字面量：pgvector 接受 '[0.1,0.2,...]' 文本形式 */
    private String toLiteral(float[] embedding) {
        if (embedding == null || embedding.length == 0) {
            throw new BizException(ErrorCode.INTERNAL_ERROR, "向量为空，无法写入 pgvector");
        }
        StringBuilder sb = new StringBuilder(embedding.length * 8).append('[');
        for (int i = 0; i < embedding.length; i++) {
            if (i > 0) {
                sb.append(',');
            }
            sb.append(embedding[i]);
        }
        return sb.append(']').toString();
    }

    /** 归桶命中结果 */
    public record BucketHit(String bucketId, double score) {
    }
}
