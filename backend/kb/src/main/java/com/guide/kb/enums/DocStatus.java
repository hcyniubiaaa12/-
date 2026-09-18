package com.guide.kb.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 文档状态（kb，链路 B）：状态机 解析中→完成/失败。
 */
@Getter
@RequiredArgsConstructor
public enum DocStatus {

    PARSING("parsing"),
    DONE("done"),
    FAILED("failed");

    /** 入库编码值（英文小写，见《数据库设计.md》§0） */
    @EnumValue
    private final String code;
}
