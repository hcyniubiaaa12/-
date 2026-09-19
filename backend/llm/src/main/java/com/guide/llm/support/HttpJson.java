package com.guide.llm.support;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.guide.common.api.ErrorCode;
import com.guide.common.exception.BizException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Map;
import java.util.function.Consumer;

/**
 * LLM 适配层内部 HTTP 工具：基于 JDK HttpClient（不引入额外 HTTP 客户端依赖）。
 * 提供「一次性 JSON 请求」与「SSE 流式逐行回调」两种形态。
 */
@Slf4j
@Component
public class HttpJson {

    private static final Duration CONNECT_TIMEOUT = Duration.ofSeconds(10);
    private static final Duration REQUEST_TIMEOUT = Duration.ofSeconds(60);

    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(CONNECT_TIMEOUT)
            .build();

    private final ObjectMapper objectMapper;

    public HttpJson(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    /** 普通 JSON 请求：非 2xx 抛 LLM_CALL_FAILED（含服务端返回片段，便于排查 Key/配额问题） */
    public JsonNode postJson(String url, Map<String, String> headers, Object body) {
        HttpRequest request = baseBuilder(url, headers)
                .timeout(REQUEST_TIMEOUT)
                .POST(HttpRequest.BodyPublishers.ofString(write(body), StandardCharsets.UTF_8))
                .build();
        try {
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            if (response.statusCode() / 100 != 2) {
                throw new BizException(ErrorCode.LLM_CALL_FAILED,
                        "HTTP " + response.statusCode() + " " + abbreviate(response.body()));
            }
            return objectMapper.readTree(response.body());
        } catch (IOException e) {
            throw new BizException(ErrorCode.LLM_CALL_FAILED, "网络异常 " + e.getMessage());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new BizException(ErrorCode.LLM_CALL_FAILED, "请求被中断");
        }
    }

    /**
     * SSE 流式请求：逐行回调响应体文本行（含空行），由调用方按 SSE 协议解析。
     * 不设整体超时——流式生成时长由模型侧决定；连接超时仍生效。
     */
    public void postStream(String url, Map<String, String> headers, Object body, Consumer<String> lineConsumer) {
        HttpRequest request = baseBuilder(url, headers)
                .POST(HttpRequest.BodyPublishers.ofString(write(body), StandardCharsets.UTF_8))
                .build();
        try {
            HttpResponse<InputStream> response = httpClient.send(request, HttpResponse.BodyHandlers.ofInputStream());
            try (InputStream in = response.body();
                 BufferedReader reader = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8))) {
                if (response.statusCode() / 100 != 2) {
                    String errorBody = reader.lines().reduce("", (a, b) -> a + b);
                    throw new BizException(ErrorCode.LLM_CALL_FAILED,
                            "HTTP " + response.statusCode() + " " + abbreviate(errorBody));
                }
                String line;
                while ((line = reader.readLine()) != null) {
                    lineConsumer.accept(line);
                }
            }
        } catch (IOException e) {
            throw new BizException(ErrorCode.LLM_CALL_FAILED, "流式网络异常 " + e.getMessage());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new BizException(ErrorCode.LLM_CALL_FAILED, "流式请求被中断");
        }
    }

    public ObjectMapper mapper() {
        return objectMapper;
    }

    private HttpRequest.Builder baseBuilder(String url, Map<String, String> headers) {
        HttpRequest.Builder builder = HttpRequest.newBuilder(URI.create(url))
                .header("Content-Type", "application/json");
        headers.forEach(builder::header);
        return builder;
    }

    private String write(Object body) {
        try {
            return objectMapper.writeValueAsString(body);
        } catch (IOException e) {
            throw new BizException(ErrorCode.INTERNAL_ERROR, "请求体序列化失败");
        }
    }

    /** 错误信息截断，避免日志/异常消息过长 */
    private String abbreviate(String text) {
        if (text == null) {
            return "";
        }
        return text.length() > 300 ? text.substring(0, 300) + "..." : text;
    }
}
