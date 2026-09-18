package com.guide.feedback.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.guide.common.entity.BaseEntity;
import lombok.Getter;
import lombok.Setter;

/**
 * 根因归因（链路 C），可事后修改，看板按最新聚合；
 * causes 存根因选项 key（选项清单代码枚举硬编码，暂不建字典表）。
 * updated_at 复用 BaseEntity 公共字段（updateFill 自动刷新）。
 */
@Getter
@Setter
@TableName("root_cause")
public class RootCause extends BaseEntity {

    private String recordId;

    /** JSON 多选，根因选项 key */
    private String causes;

    private String updatedBy;
}
