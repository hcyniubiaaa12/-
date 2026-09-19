package com.guide.llm.client;

import java.util.List;
import java.util.function.Consumer;

/**
 * 对话模型接口（LLM 适配层唯一出口之一）：DeepSeek。
 * 业务代码只依赖本接口，不直连第三方 API。
 */
public interface ChatModel {

    /** 一次性返回完整回复（查询改写、合成 chunk 文本生成等离线/短任务用） */
    String chat(List<ChatMsg> messages);

    /**
     * 流式对话：逐段回调增量文本，返回拼接后的完整文本。
     * 用于链路 A 的 SSE delta 逐字渲染。
     *
     * @param onDelta 增量回调（在调用线程上同步触发）
     */
    String chatStream(List<ChatMsg> messages, Consumer<String> onDelta);
}
