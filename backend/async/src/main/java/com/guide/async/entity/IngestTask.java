package com.guide.async.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.guide.async.enums.IngestStage;
import com.guide.async.enums.IngestStatus;
import com.guide.common.entity.BaseEntity;
import lombok.Getter;
import lombok.Setter;

/**
 * 离线入库任务（链路 B 流水线）：任务表是权威状态；幂等与重试以 taskId 为键。
 */
@Getter
@Setter
@TableName("ingest_task")
public class IngestTask extends BaseEntity {

    private String docId;

    private IngestStage stage;

    private IngestStatus status;

    private Integer total;

    private Integer done;

    private String error;
}
