package com.guide.feedback.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.guide.common.entity.BaseEntity;
import com.guide.feedback.enums.TrackStage;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * 埋点三触点（链路 C），旁路落库；只作漏斗/耗时等过程信号，
 * 非准确率事实来源（事实在 guide_record.actual_dept_id）。
 */
@Getter
@Setter
@TableName("track_event")
public class TrackEvent extends BaseEntity {

    private String recordId;

    private String userId;

    private TrackStage stage;

    private String deptId;

    private LocalDateTime occurredAt;
}
