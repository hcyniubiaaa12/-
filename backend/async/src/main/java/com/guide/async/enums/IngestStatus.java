package com.guide.async.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 入库任务状态（async，链路 B）：任务表是权威状态，幂等与重试以 taskId 为键。
 */
@Getter
@RequiredArgsConstructor
public enum IngestStatus {

    RUNNING("running"),
    SUCCESS("success"),
    FAILED("failed");

    /** 入库编码值（英文小写，见《数据库设计.md》§0） */
    @EnumValue
    private final String code;
}
