package com.guide.kb.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 症状交叉映射来源（kb）：回流 approve 写 feedback。
 */
@Getter
@RequiredArgsConstructor
public enum MappingSource {

    INIT("init"),
    MANUAL("manual"),
    FEEDBACK("feedback");

    /** 入库编码值（英文小写，见《数据库设计.md》§0） */
    @EnumValue
    private final String code;
}
