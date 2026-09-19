package com.guide.llm.client;

/**
 * 对话消息（role = system / user / assistant）。
 */
public record ChatMsg(String role, String content) {

    public static ChatMsg system(String content) {
        return new ChatMsg("system", content);
    }

    public static ChatMsg user(String content) {
        return new ChatMsg("user", content);
    }

    public static ChatMsg assistant(String content) {
        return new ChatMsg("assistant", content);
    }
}
