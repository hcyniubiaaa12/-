package com.guide.kb.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 医疗术语来源（kb）：LLM 抽取新术语落库默认未启用，受"人工审核"开关控制。
 */
@Getter
@RequiredArgsConstructor
public enum TermSource {

    LLM_EXTRACT("llm_extract"),
    MANUAL("manual");

    /** 入库编码值（英文小写，见《数据库设计.md》§0） */
    @EnumValue
    private final String code;
}
