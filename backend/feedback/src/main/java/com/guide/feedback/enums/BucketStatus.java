package com.guide.feedback.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 聚合桶状态（feedback，链路 C）：monitoring 累计中，达到升级阈值转 pending 进待审队列。
 */
@Getter
@RequiredArgsConstructor
public enum BucketStatus {

    /** 监控中，未达升级阈值 */
    MONITORING("monitoring"),

    /** 已升级，待人工审核（已生成 review_task） */
    PENDING("pending"),

    /** 审核通过，已回流知识库 */
    APPROVED("approved"),

    /** 审核驳回 */
    REJECTED("rejected"),

    /** 人工忽略 */
    DISMISSED("dismissed");

    /** 入库编码值（英文小写，见《数据库设计.md》§0） */
    @EnumValue
    private final String code;
}
