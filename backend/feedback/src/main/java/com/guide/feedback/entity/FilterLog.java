package com.guide.feedback.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.guide.common.entity.BaseEntity;
import com.guide.feedback.enums.FilterAction;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * 敏感词过滤日志（链路 C），旁路落库不阻塞主流程。
 */
@Getter
@Setter
@TableName("filter_log")
public class FilterLog extends BaseEntity {

    private String userId;

    private String sessionId;

    private String wordId;

    private FilterAction action;

    private LocalDateTime matchedAt;
}
