package com.guide.async.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 入库流水线阶段（async，链路 B）：解析→切分→向量化→完成。
 */
@Getter
@RequiredArgsConstructor
public enum IngestStage {

    PARSE("parse"),
    SPLIT("split"),
    EMBED("embed"),
    DONE("done");

    /** 入库编码值（英文小写，见《数据库设计.md》§0） */
    @EnumValue
    private final String code;
}
