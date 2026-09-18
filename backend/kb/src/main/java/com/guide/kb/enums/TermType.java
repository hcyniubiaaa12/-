package com.guide.kb.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 医疗术语类型（kb，白名单）。
 */
@Getter
@RequiredArgsConstructor
public enum TermType {

    /** 部位词 */
    PART("part"),

    /** 症状词 */
    SYMPTOM("symptom");

    /** 入库编码值（英文小写，见《数据库设计.md》§0） */
    @EnumValue
    private final String code;
}
