package com.guide.auth.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.guide.auth.enums.SensitiveWordType;
import com.guide.common.entity.BaseEntity;
import lombok.Getter;
import lombok.Setter;

/**
 * 敏感词库（链路 A 入口前置校验）；管理端可维护，支持批量导入。
 * 白名单复用知识库部位词，不建表。
 */
@Getter
@Setter
@TableName("sensitive_word")
public class SensitiveWord extends BaseEntity {

    private String word;

    private SensitiveWordType type;

    private Integer hitCount;

    /** 0 启用 / 1 停用（含义按管理端约定） */
    private Integer enabled;
}
