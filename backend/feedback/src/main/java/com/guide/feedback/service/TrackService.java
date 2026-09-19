package com.guide.feedback.service;

import com.guide.chat.entity.ChatSession;
import com.guide.chat.entity.GuideRecord;
import com.guide.chat.mapper.ChatSessionMapper;
import com.guide.chat.mapper.GuideRecordMapper;
import com.guide.common.api.ErrorCode;
import com.guide.common.exception.BizException;
import com.guide.feedback.entity.TrackEvent;
import com.guide.feedback.enums.TrackStage;
import com.guide.feedback.mapper.TrackEventMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

/**
 * 埋点落库（链路 C）：三触点只作漏斗/耗时等过程信号，**非准确率事实来源**
 * （事实在 guide_record.actual_dept_id，由挂号确认接口写入）。
 * 旁路性质：落库失败不阻塞患者主流程，仅记日志。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TrackService {

    private final TrackEventMapper trackEventMapper;
    private final GuideRecordMapper guideRecordMapper;
    private final ChatSessionMapper chatSessionMapper;

    public void track(TrackStage stage, String recordId, String userId, String deptId) {
        GuideRecord record = guideRecordMapper.selectById(recordId);
        if (record == null) {
            throw new BizException(ErrorCode.RECORD_NOT_FOUND);
        }
        // 归属校验：埋点只能上报本人导诊记录，防脏数据
        ChatSession session = chatSessionMapper.selectById(record.getSessionId());
        if (session == null || !userId.equals(session.getUserId())) {
            throw new BizException(ErrorCode.RECORD_NOT_FOUND);
        }
        TrackEvent event = new TrackEvent();
        event.setRecordId(recordId);
        event.setUserId(userId);
        event.setStage(stage);
        event.setDeptId(deptId);
        event.setOccurredAt(LocalDateTime.now());
        trackEventMapper.insert(event);
    }
}
