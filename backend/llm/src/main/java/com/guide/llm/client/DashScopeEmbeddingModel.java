package com.guide.llm.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.guide.common.api.ErrorCode;
import com.guide.common.exception.BizException;
import com.guide.llm.config.LlmProperties;
import com.guide.llm.support.HttpJson;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 阿里 text-embedding-v3（DashScope OpenAI 兼容模式 /compatible-mode/v1/embeddings）。
 * 批量分批发送（摊薄成本），维度固定由配置给出——须与 pgvector 建表 VECTOR(n) 一致。
 */
@Slf4j
@Component
public class DashScopeEmbeddingModel implements EmbeddingModel {

    private final LlmProperties properties;
    private final HttpJson http;

    public DashScopeEmbeddingModel(LlmProperties properties, HttpJson http) {
        this.properties = properties;
        this.http = http;
    }

    @Override
    public float[] embed(String text) {
        return embedBatch(List.of(text)).get(0);
    }

    @Override
    public List<float[]> embedBatch(List<String> texts) {
        requireKey();
        if (texts == null || texts.isEmpty()) {
            return List.of();
        }
        // 批大小下限兜底：配 0/负数会让下面的 for 步进为 0 变成死循环（不断发真实请求）
        int batchSize = Math.max(1, properties.getDashscope().getEmbeddingBatchSize());
        List<float[]> vectors = new ArrayList<>(texts.size());
        for (int from = 0; from < texts.size(); from += batchSize) {
            int to = Math.min(from + batchSize, texts.size());
            List<String> batch = texts.subList(from, to).stream().map(this::normalize).toList();
            vectors.addAll(embedOneBatch(batch));
        }
        return vectors;
    }

    private List<float[]> embedOneBatch(List<String> batch) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("model", properties.getDashscope().getEmbeddingModel());
        body.put("input", batch);
        body.put("dimensions", properties.getDashscope().getEmbeddingDimension());
        body.put("encoding_format", "float");

        JsonNode response = http.postJson(url(), authHeaders(), body);
        JsonNode data = response.path("data");
        if (!data.isArray() || data.size() != batch.size()) {
            throw new BizException(ErrorCode.LLM_CALL_FAILED,
                    "embedding 返回条数与入参不一致：" + data.size() + "/" + batch.size());
        }
        // 按 index 回填，保证与入参顺序一致
        float[][] ordered = new float[batch.size()][];
        for (JsonNode item : data) {
            int index = item.path("index").asInt(-1);
            if (index < 0 || index >= batch.size()) {
                throw new BizException(ErrorCode.LLM_CALL_FAILED, "embedding 返回越界下标：" + index);
            }
            JsonNode embedding = item.path("embedding");
            float[] vector = new float[embedding.size()];
            for (int i = 0; i < embedding.size(); i++) {
                vector[i] = (float) embedding.get(i).asDouble();
            }
            validateDimension(vector);
            ordered[index] = vector;
        }
        for (int i = 0; i < ordered.length; i++) {
            if (ordered[i] == null) {
                // 上游漏返回某条（重复/缺失下标）：明确报错，别让 NPE 冒到上层
                throw new BizException(ErrorCode.LLM_CALL_FAILED, "embedding 未返回第 " + i + " 条的向量");
            }
        }
        return List.of(ordered);
    }

    /** 单条文本预处理：去换行、按配置截断（防超 token 上限） */
    private String normalize(String text) {
        String cleaned = text == null ? "" : text.replaceAll("\\s+", " ").trim();
        int maxChars = properties.getDashscope().getEmbeddingMaxChars();
        return cleaned.length() > maxChars ? cleaned.substring(0, maxChars) : cleaned;
    }

    private void validateDimension(float[] vector) {
        int expected = properties.getDashscope().getEmbeddingDimension();
        if (vector.length != expected) {
            throw new BizException(ErrorCode.LLM_CALL_FAILED,
                    "embedding 维度 " + vector.length + " 与配置 " + expected + " 不一致（pgvector 建表维度需同步）");
        }
    }

    private Map<String, String> authHeaders() {
        return Map.of("Authorization", "Bearer " + properties.getDashscope().getApiKey());
    }

    private String url() {
        String base = properties.getDashscope().getBaseUrl();
        return (base.endsWith("/") ? base.substring(0, base.length() - 1) : base) + "/compatible-mode/v1/embeddings";
    }

    private void requireKey() {
        if (!StringUtils.hasText(properties.getDashscope().getApiKey())) {
            throw new BizException(ErrorCode.LLM_NOT_CONFIGURED, "llm.dashscope.api-key 为空");
        }
    }
}
