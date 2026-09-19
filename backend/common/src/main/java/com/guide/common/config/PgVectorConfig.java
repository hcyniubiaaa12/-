package com.guide.common.config;

import com.zaxxer.hikari.HikariDataSource;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * pgvector 数据源配置：独立于 MySQL 主数据源（管语义），仅供向量读写使用。
 *
 * <p><b>刻意不注册 DataSource 类型的 Bean</b>：Spring Boot 的 MySQL 数据源自动配置带
 * {@code @ConditionalOnMissingBean(DataSource.class)}，一旦容器里出现别的 DataSource，
 * 主数据源自动配置会整体退让，MyBatis 会误连向量库（表现为 relation "dept" does not exist）。
 * 因此这里只暴露 JdbcTemplate，连接池由它内部持有并在销毁时关闭。
 *
 * <p>连接池懒加载（无参构造 + setter）：向量库不可用不影响应用启动与 MySQL 侧业务。
 */
@Configuration
public class PgVectorConfig {

    public static final String PG_VECTOR_JDBC_TEMPLATE = "pgVectorJdbcTemplate";

    @Bean(name = PG_VECTOR_JDBC_TEMPLATE)
    public PgVectorJdbcTemplate pgVectorJdbcTemplate(PgVectorProperties properties) {
        return new PgVectorJdbcTemplate(properties);
    }

    /** pgvector 专用 JdbcTemplate：内部持有连接池，随应用关闭释放 */
    public static class PgVectorJdbcTemplate extends JdbcTemplate implements DisposableBean {

        private final transient HikariDataSource dataSource;

        PgVectorJdbcTemplate(PgVectorProperties properties) {
            this.dataSource = buildDataSource(properties);
            setDataSource(dataSource);
            setQueryTimeout(30);
        }

        private static HikariDataSource buildDataSource(PgVectorProperties properties) {
            HikariDataSource dataSource = new HikariDataSource();
            dataSource.setJdbcUrl(properties.getUrl());
            dataSource.setUsername(properties.getUsername());
            dataSource.setPassword(properties.getPassword());
            dataSource.setDriverClassName("org.postgresql.Driver");
            dataSource.setPoolName("pgvector-pool");
            dataSource.setMaximumPoolSize(4);
            dataSource.setMinimumIdle(0);
            return dataSource;
        }

        @Override
        public void destroy() {
            dataSource.close();
        }
    }
}
