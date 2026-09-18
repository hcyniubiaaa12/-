package com.guide.feedback.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 敏感词命中动作（feedback，链路 C）：与 sensitive_word.type 对应
 * （banned → blocked / watch → watched）。
 */
@Getter
@RequiredArgsConstructor
public enum FilterAction {

    BLOCKED("blocked"),
    WATCHED("watched");

    /** 入库编码值（英文小写，见《数据库设计.md》§0） */
    @EnumValue
    private final String code;
}
