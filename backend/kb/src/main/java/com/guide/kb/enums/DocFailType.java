package com.guide.kb.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 文档失败类型（kb）：RETRYABLE 可重试 / FATAL 不可重试。
 */
@Getter
@RequiredArgsConstructor
public enum DocFailType {

    RETRYABLE("retryable"),
    FATAL("fatal");

    /** 入库编码值（英文小写，见《数据库设计.md》§0） */
    @EnumValue
    private final String code;
}
