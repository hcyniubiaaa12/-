package com.guide.chat.event;

/**
 * 敏感词命中事件（链路 A 入口前置校验的旁路留痕）。
 * chat 只发事件、不依赖 feedback 模块（依赖方向：feedback → chat），由 feedback 侧监听落 filter_log。
 * action 用入库编码值（blocked / watched），与《数据库设计.md》§0 的枚举编码一致。
 */
public record SensitiveHitEvent(String userId, String sessionId, String wordId, String action) {

    public static final String BLOCKED = "blocked";
    public static final String WATCHED = "watched";
}
