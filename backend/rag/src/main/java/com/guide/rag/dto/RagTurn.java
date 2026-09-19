package com.guide.rag.dto;

/**
 * 对话轮（rag 层的入参形态）：业务侧按本类型传历史，rag 内部再转 LLM 层的消息类型——
 * 这样 chat 等业务模块只依赖 rag，不直接依赖 llm（见 CLAUDE.md 依赖规范）。
 *
 * @param role    user / assistant
 * @param content 文本
 */
public record RagTurn(String role, String content) {

    public static RagTurn user(String content) {
        return new RagTurn("user", content);
    }

    public static RagTurn assistant(String content) {
        return new RagTurn("assistant", content);
    }
}
