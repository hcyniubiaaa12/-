package com.guide.chat.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.ThreadPoolExecutor;

/**
 * 在线导诊线程池：SSE 流式编排（含模型流式等待）用，与离线入库线程池隔离，不共用
 * （见 CLAUDE.md 依赖规范：async 线程池与在线导诊线程隔离）。
 * 队列满时 AbortPolicy 直接拒绝：CallerRunsPolicy 会让 Tomcat 请求线程同步跑完整个模型生成，
 * 此时 emitter 还没交给 MVC，delta 只能攒在缓冲区里一次性回放，逐字流式渲染整体失效；
 * 拒绝后由 ChatService 明确回一个 error 事件，患者可重试。
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
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.AbortPolicy());
        executor.initialize();
        return executor;
    }
}
