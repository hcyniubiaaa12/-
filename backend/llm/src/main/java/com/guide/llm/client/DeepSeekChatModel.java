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
import java.util.function.Consumer;

/**
 * DeepSeek 对话模型（OpenAI 兼容 /chat/completions）。
 * 流式与非流式共用同一请求体，只有 stream 开关不同。
 */
@Slf4j
@Component
public class DeepSeekChatModel implements ChatModel {

    private static final String DONE = "[DONE]";

    private final LlmProperties properties;
    private final HttpJson http;

    public DeepSeekChatModel(LlmProperties properties, HttpJson http) {
        this.properties = properties;
        this.http = http;
    }

    @Override
    public String chat(List<ChatMsg> messages) {
        requireKey();
        long start = System.currentTimeMillis();
        JsonNode response = http.postJson(url(), authHeaders(), requestBody(messages, false));
        String content = response.path("choices").path(0).path("message").path("content").asText("");
        if (log.isDebugEnabled()) {
            log.debug("对话模型（非流式）：model={} 消息={} 条｜耗时 {} ms｜输出 {} 字",
                    properties.getDeepseek().getModel(), messages.size(),
                    System.currentTimeMillis() - start, content.length());
        }
        return content;
    }

    @Override
    public String chatStream(List<ChatMsg> messages, Consumer<String> onDelta) {
        requireKey();
        long start = System.currentTimeMillis();
        StringBuilder full = new StringBuilder();
        http.postStream(url(), authHeaders(), requestBody(messages, true), line -> {
            String payload = ssePayload(line);
            if (payload == null || DONE.equals(payload)) {
                return;
            }
            try {
                JsonNode delta = http.mapper().readTree(payload)
                        .path("choices").path(0).path("delta").path("content");
                if (delta.isTextual() && !delta.asText().isEmpty()) {
                    full.append(delta.asText());
                    onDelta.accept(delta.asText());
                }
            } catch (Exception e) {
                // 单行解析失败不影响整体流：跳过该增量
                log.debug("跳过无法解析的流式分片：{}", payload);
            }
        });
        if (log.isDebugEnabled()) {
            log.debug("对话模型（流式）：model={} 消息={} 条｜耗时 {} ms｜输出 {} 字",
                    properties.getDeepseek().getModel(), messages.size(),
                    System.currentTimeMillis() - start, full.length());
        }
        return full.toString();
    }

    /** 取 SSE data 行负载；非 data 行（注释、事件名、空行）返回 null */
    private String ssePayload(String line) {
        if (line == null || !line.startsWith("data:")) {
            return null;
        }
        String payload = line.substring("data:".length()).trim();
        return payload.isEmpty() ? null : payload;
    }

    private Map<String, Object> requestBody(List<ChatMsg> messages, boolean stream) {
        List<Map<String, String>> payload = new ArrayList<>(messages.size());
        for (ChatMsg msg : messages) {
            payload.add(Map.of("role", msg.role(), "content", msg.content()));
        }
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("model", properties.getDeepseek().getModel());
        body.put("messages", payload);
        body.put("stream", stream);
        body.put("temperature", properties.getDeepseek().getTemperature());
        return body;
    }

    private Map<String, String> authHeaders() {
        return Map.of("Authorization", "Bearer " + properties.getDeepseek().getApiKey());
    }

    private String url() {
        return trimTrailingSlash(properties.getDeepseek().getBaseUrl()) + "/chat/completions";
    }

    private void requireKey() {
        if (!StringUtils.hasText(properties.getDeepseek().getApiKey())) {
            throw new BizException(ErrorCode.LLM_NOT_CONFIGURED, "llm.deepseek.api-key 为空");
        }
    }

    private String trimTrailingSlash(String url) {
        return url != null && url.endsWith("/") ? url.substring(0, url.length() - 1) : url;
    }
}
