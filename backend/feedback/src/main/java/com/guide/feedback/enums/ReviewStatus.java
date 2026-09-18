package com.guide.feedback.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 待审核任务状态（feedback，链路 C）：写知识库唯一路径是人工 approve。
 */
@Getter
@RequiredArgsConstructor
public enum ReviewStatus {

    PENDING("pending"),
    DONE("done");

    /** 入库编码值（英文小写，见《数据库设计.md》§0） */
    @EnumValue
    private final String code;
}
