package com.guide.auth.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 敏感词类型（auth，链路 A 入口前置校验）。
 */
@Getter
@RequiredArgsConstructor
public enum SensitiveWordType {

    /** 禁止词：命中即拦截 */
    BANNED("banned"),

    /** 观察词：命中只记日志（filter_log action=watched） */
    WATCH("watch");

    /** 入库编码值（英文小写，见《数据库设计.md》§0） */
    @EnumValue
    private final String code;
}
