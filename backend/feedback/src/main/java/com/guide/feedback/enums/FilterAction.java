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

    /**
     * 按编码值匹配（禁止用 valueOf——它匹配常量名，入参是小写编码值必炸，见进度.md 已知坑）。
     */
    public static FilterAction fromCode(String code) {
        for (FilterAction action : values()) {
            if (action.code.equals(code)) {
                return action;
            }
        }
        return null;
    }
}
