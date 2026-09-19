package com.guide.llm.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * LLM 适配层配置：DeepSeek 对话 / 阿里 DashScope embedding + rerank。
 * 模型名与 base-url 可配（application.yml），API Key 只放 application-local.yml（密钥不进库、不进仓库）。
 */
@Data
@Component
@ConfigurationProperties(prefix = "llm")
public class LlmProperties {

    private Deepseek deepseek = new Deepseek();

    private Dashscope dashscope = new Dashscope();

    @Data
    public static class Deepseek {
        private String apiKey;
        private String baseUrl;
        private String model;
        /** 生成温度：导诊要稳定可复现，默认偏低 */
        private Double temperature = 0.3;
    }

    @Data
    public static class Dashscope {
        private String apiKey;
        private String baseUrl;
        private String embeddingModel;
        private String rerankModel;
        /** embedding 维度，需与 pgvector 建表维度一致 */
        private Integer embeddingDimension = 1024;
        /** 单次 embedding 请求最大文本数（DashScope 批量上限内保守取值） */
        private Integer embeddingBatchSize = 10;
        /** 单条文本截断长度（字符），防止超模型 token 上限 */
        private Integer embeddingMaxChars = 2000;
    }
}
