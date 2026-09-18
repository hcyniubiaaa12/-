package com.guide.common.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * PostgreSQL + pgvector 向量库连接配置（管语义，独立于 MySQL 主数据源）。
 * 真实密码放 application-local.yml。写入方：kb（RAG 语料）、feedback（聚类锚点）。
 */
@Data
@Component
@ConfigurationProperties(prefix = "pgvector")
public class PgVectorProperties {

    private String url;

    private String username;

    private String password;

    /** embedding 维度，与建表 VECTOR(n) 一致；换模型需同步修改并重建向量 */
    private Integer dimension;
}
