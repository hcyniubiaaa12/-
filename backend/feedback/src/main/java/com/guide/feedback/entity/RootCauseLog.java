package com.guide.feedback.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.guide.common.entity.BaseEntity;
import lombok.Getter;
import lombok.Setter;

/**
 * 归因修改历史（链路 C），每次修改追加一条不覆盖；
 * 纯审计留痕，不参与看板聚合。
 */
@Getter
@Setter
@TableName("root_cause_log")
public class RootCauseLog extends BaseEntity {

    private String recordId;

    /** JSON */
    private String causesBefore;

    /** JSON */
    private String causesAfter;

    private String updatedBy;
}
