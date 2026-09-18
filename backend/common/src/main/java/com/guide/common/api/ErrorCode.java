package com.guide.common.api;

/**
 * 错误码枚举：code 统一按模块分段。
 * common 1xxx / auth 2xxx / chat 3xxx / rag 4xxx / kb 5xxx / feedback 6xxx。
 */
public enum ErrorCode {

    // common 1xxx
    INTERNAL_ERROR(1000, "系统内部错误"),
    PARAM_INVALID(1001, "参数校验失败"),

    // auth 2xxx
    UNAUTHORIZED(2000, "未登录或登录已过期"),
    FORBIDDEN(2001, "无权限访问"),
    USERNAME_EXISTS(2002, "用户名已存在"),
    USER_BANNED(2003, "账号已被封禁"),

    // chat 3xxx
    SESSION_CLOSED(3000, "会话已结束，请重新描述症状"),
    SENSITIVE_BLOCKED(3001, "输入包含不允许的内容"),

    // rag 4xxx
    RAG_EMPTY(4000, "知识库暂无相关内容"),

    // kb 5xxx
    DOC_NOT_FOUND(5000, "文档不存在"),

    // feedback 6xxx
    RECORD_NOT_FOUND(6000, "导诊记录不存在");

    private final int code;
    private final String message;

    ErrorCode(int code, String message) {
        this.code = code;
        this.message = message;
    }

    public int getCode() {
        return code;
    }

    public String getMessage() {
        return message;
    }
}
