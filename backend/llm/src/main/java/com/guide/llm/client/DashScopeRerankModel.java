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
 * 阿里 gte-rerank-v2（DashScope 原生接口 /api/v1/services/rerank/text-rerank/text-rerank）。
 * 输入 = 查询 + 候选正文，输出 = 相关度降序的下标与分数。
 */
@Slf4j
@Component
public class DashScopeRerankModel implements RerankModel {

    /** 单条候选截断长度（字符），与 embedding 侧同口径防超长 */
    private static final int MAX_DOC_CHARS = 2000;

    private final LlmProperties properties;
    private final HttpJson http;

    public DashScopeRerankModel(LlmProperties properties, HttpJson http) {
        this.properties = properties;
        this.http = http;
    }

    @Override
    public List<RerankHit> rerank(String query, List<String> documents, int topN) {
        requireKey();
        if (documents == null || documents.isEmpty()) {
            return List.of();
        }
        Map<String, Object> input = new LinkedHashMap<>();
        input.put("query", query);
        input.put("documents", documents.stream().map(this::truncate).toList());

        Map<String, Object> parameters = new LinkedHashMap<>();
        parameters.put("return_documents", false);
        parameters.put("top_n", Math.min(topN, documents.size()));

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("model", properties.getDashscope().getRerankModel());
        body.put("input", input);
        body.put("parameters", parameters);

        JsonNode results = http.postJson(url(), authHeaders(), body).path("output").path("results");
        if (!results.isArray()) {
            throw new BizException(ErrorCode.LLM_CALL_FAILED, "rerank 返回结构异常");
        }
        List<RerankHit> hits = new ArrayList<>(results.size());
        for (JsonNode item : results) {
            hits.add(new RerankHit(item.path("index").asInt(), item.path("relevance_score").asDouble()));
        }
        return hits;
    }

    private String truncate(String doc) {
        String cleaned = doc == null ? "" : doc.replaceAll("\\s+", " ").trim();
        return cleaned.length() > MAX_DOC_CHARS ? cleaned.substring(0, MAX_DOC_CHARS) : cleaned;
    }

    private Map<String, String> authHeaders() {
        return Map.of("Authorization", "Bearer " + properties.getDashscope().getApiKey());
    }

    private String url() {
        String base = properties.getDashscope().getBaseUrl();
        return (base.endsWith("/") ? base.substring(0, base.length() - 1) : base)
                + "/api/v1/services/rerank/text-rerank/text-rerank";
    }

    private void requireKey() {
        if (!StringUtils.hasText(properties.getDashscope().getApiKey())) {
            throw new BizException(ErrorCode.LLM_NOT_CONFIGURED, "llm.dashscope.api-key 为空");
        }
    }
}
