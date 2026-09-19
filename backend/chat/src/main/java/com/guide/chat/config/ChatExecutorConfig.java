package com.guide.chat.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.ThreadPoolExecutor;

/**
 * 在线导诊线程池：SSE 流式编排（含模型流式等待）用，与离线入库线程池隔离，不共用
 * （见 CLAUDE.md 依赖规范：async 线程池与在线导诊线程隔离）。
 * 队列满时用 CallerRunsPolicy 退化为调用线程执行——宁可拖慢一次请求，也不丢患者消息。
 */
@Configuration
public class ChatExecutorConfig {

    public static final String CHAT_SSE_EXECUTOR = "chatSseExecutor";

    @Bean(name = CHAT_SSE_EXECUTOR)
    public ThreadPoolTaskExecutor chatSseExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(4);
        executor.setMaxPoolSize(16);
        executor.setQueueCapacity(50);
        executor.setThreadNamePrefix("chat-sse-");
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        executor.initialize();
        return executor;
    }
}
