package com.guide.feedback.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.guide.common.entity.BaseEntity;
import com.guide.feedback.enums.ReviewStatus;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * 待审核队列（链路 C）；写知识库唯一路径是人工 approve。
 */
@Getter
@Setter
@TableName("review_task")
public class ReviewTask extends BaseEntity {

    private String bucketId;

    private ReviewStatus status;

    private String reviewedBy;

    private LocalDateTime reviewedAt;

    /** 主科室：审核给出的修正目标（唯一写知识库路径是人工 approve） */
    private String mainDeptId;
}
