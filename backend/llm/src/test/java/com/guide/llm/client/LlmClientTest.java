package com.guide.llm.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.guide.common.api.ErrorCode;
import com.guide.common.exception.BizException;
import com.guide.llm.config.LlmProperties;
import com.guide.llm.support.HttpJson;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * LLM 适配层客户端测试：用 JDK 内置 HttpServer 起桩，验证请求形态与响应解析，不发真实请求。
 */
class LlmClientTest {

    private HttpServer server;
    private LlmProperties properties;
    private HttpJson httpJson;
    private final List<String> capturedRequests = new ArrayList<>();

    @BeforeEach
    void setUp() throws IOException {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.start();
        properties = new LlmProperties();
        properties.getDeepseek().setApiKey("test-key");
        properties.getDeepseek().setBaseUrl(baseUrl());
        properties.getDeepseek().setModel("deepseek-chat");
        properties.getDashscope().setApiKey("test-key");
        properties.getDashscope().setBaseUrl(baseUrl());
        properties.getDashscope().setEmbeddingModel("text-embedding-v3");
        properties.getDashscope().setRerankModel("gte-rerank-v2");
        properties.getDashscope().setEmbeddingDimension(4);
        properties.getDashscope().setEmbeddingBatchSize(2);
        httpJson = new HttpJson(new ObjectMapper());
    }

    @AfterEach
    void tearDown() {
        server.stop(0);
    }

    @Test
    @DisplayName("流式对话：逐分片回调并拼接完整文本，[DONE] 不产生增量")
    void chatStreamCollectsDeltas() {
        stub("""
                data: {"choices":[{"delta":{"role":"assistant","content":""}}]}

                data: {"choices":[{"delta":{"content":"建议首诊"}}]}

                data: {"choices":[{"delta":{"content":"心血管内科"}}]}

                data: [DONE]

                """);
        List<String> deltas = new ArrayList<>();

        String full = new DeepSeekChatModel(properties, httpJson)
                .chatStream(List.of(ChatMsg.system("sys"), ChatMsg.user("胸口闷")), deltas::add);

        assertThat(deltas).containsExactly("建议首诊", "心血管内科");
        assertThat(full).isEqualTo("建议首诊心血管内科");
        assertThat(capturedRequests.get(0)).contains("\"stream\":true").contains("胸口闷");
    }

    @Test
    @DisplayName("非流式对话：取 choices[0].message.content")
    void chatReturnsMessageContent() {
        stub("""
                {"choices":[{"message":{"role":"assistant","content":"{\\"ok\\":true}"}}]}
                """);

        String content = new DeepSeekChatModel(properties, httpJson)
                .chat(List.of(ChatMsg.user("改写这句话")));

        assertThat(content).isEqualTo("{\"ok\":true}");
        assertThat(capturedRequests.get(0)).contains("\"stream\":false");
    }

    @Test
    @DisplayName("embedding：按 index 回填顺序，超出批大小自动分批")
    void embedBatchKeepsOrderAndSplitsBatches() {
        // 桩：把文本映射成第 4 维数值，并倒序返回 index，验证客户端跨批次按 index 回填
        Map<String, Integer> code = Map.of("胸闷", 1, "咳嗽", 2, "腹痛", 3);
        stubBody(body -> {
            JsonNode input = body.path("input");
            StringBuilder data = new StringBuilder();
            for (int i = input.size() - 1; i >= 0; i--) {
                if (!data.isEmpty()) {
                    data.append(',');
                }
                data.append("{\"index\":").append(i).append(",\"embedding\":[0.1,0.2,0.3,")
                        .append(code.get(input.get(i).asText())).append("]}");
            }
            return "{\"data\":[" + data + "]}";
        });

        List<float[]> vectors = new DashScopeEmbeddingModel(properties, httpJson)
                .embedBatch(List.of("胸闷", "咳嗽", "腹痛"));

        assertThat(vectors).hasSize(3);
        assertThat(vectors.get(0)[3]).isEqualTo(1f);
        assertThat(vectors.get(1)[3]).isEqualTo(2f);
        assertThat(vectors.get(2)[3]).isEqualTo(3f);
        // 批大小 2 → 3 条文本分 2 次请求
        assertThat(capturedRequests).hasSize(2);
    }

    @Test
    @DisplayName("embedding：维度与配置不一致直接报错（防止写坏 pgvector）")
    void embedRejectsWrongDimension() {
        stub("""
                {"data":[{"index":0,"embedding":[0.1,0.2]}]}
                """);

        assertThatThrownBy(() -> new DashScopeEmbeddingModel(properties, httpJson).embed("胸闷"))
                .isInstanceOf(BizException.class)
                .hasMessageContaining("维度");
    }

    @Test
    @DisplayName("rerank：解析 output.results 的下标与分数")
    void rerankParsesResults() {
        stub("""
                {"output":{"results":[{"index":1,"relevance_score":0.91},{"index":0,"relevance_score":0.42}]}}
                """);

        List<RerankModel.RerankHit> hits = new DashScopeRerankModel(properties, httpJson)
                .rerank("胸口闷", List.of("片段A", "片段B"), 2);

        assertThat(hits).hasSize(2);
        assertThat(hits.get(0).index()).isEqualTo(1);
        assertThat(hits.get(0).score()).isEqualTo(0.91);
        assertThat(capturedRequests.get(0)).contains("gte-rerank-v2");
    }

    @Test
    @DisplayName("未配置 API Key：调用即报 LLM_NOT_CONFIGURED，不发出请求")
    void missingKeyFailsFast() {
        properties.getDashscope().setApiKey("");
        properties.getDeepseek().setApiKey("");

        assertThatThrownBy(() -> new DashScopeEmbeddingModel(properties, httpJson).embed("胸闷"))
                .isInstanceOf(BizException.class)
                .extracting(e -> ((BizException) e).getCode())
                .isEqualTo(ErrorCode.LLM_NOT_CONFIGURED.getCode());
        assertThatThrownBy(() -> new DeepSeekChatModel(properties, httpJson).chat(List.of(ChatMsg.user("hi"))))
                .isInstanceOf(BizException.class);
        assertThat(capturedRequests).isEmpty();
    }

    // —— 桩服务 ——

    private String baseUrl() {
        return "http://127.0.0.1:" + server.getAddress().getPort();
    }

    private void stub(String responseBody) {
        stubBody(body -> responseBody);
    }

    /** 按请求体动态生成响应；请求体只读取一次并留档 */
    private void stubBody(Function<JsonNode, String> responder) {
        ObjectMapper mapper = new ObjectMapper();
        server.createContext("/", exchange -> {
            String raw = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
            capturedRequests.add(raw);
            JsonNode body = raw.isEmpty() ? mapper.missingNode() : mapper.readTree(raw);
            byte[] bytes = responder.apply(body).getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().add("Content-Type", "application/json; charset=utf-8");
            exchange.sendResponseHeaders(200, bytes.length);
            try (OutputStream out = exchange.getResponseBody()) {
                out.write(bytes);
            }
        });
    }
}
