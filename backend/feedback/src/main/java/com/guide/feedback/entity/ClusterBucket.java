package com.guide.feedback.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.guide.common.entity.BaseEntity;
import com.guide.feedback.enums.BucketStatus;
import lombok.Getter;
import lombok.Setter;

/**
 * 错误模式聚合桶（链路 C，MySQL 存事实）；锚点固定不漂移；方向预筛。
 * 锚点向量不存本表，在 pgvector cluster_bucket_vec（见数据库设计 4.1）。
 */
@Getter
@Setter
@TableName("cluster_bucket")
public class ClusterBucket extends BaseEntity {

    /** 错误方向：rec_dept_id → actual_dept_id */
    private String recDeptId;

    private String actualDeptId;

    private String anchorText;

    /** 精确归桶键 */
    private String exactKey;

    private Integer count;

    private BucketStatus status;
}
