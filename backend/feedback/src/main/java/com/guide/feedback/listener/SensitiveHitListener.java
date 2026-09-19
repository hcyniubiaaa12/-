package com.guide.feedback.listener;

import com.guide.chat.event.SensitiveHitEvent;
import com.guide.feedback.entity.FilterLog;
import com.guide.feedback.enums.FilterAction;
import com.guide.feedback.mapper.FilterLogMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * 敏感词命中留痕（链路 C 配套）：监听 chat 的入口校验事件写 filter_log。
 * chat 不依赖 feedback，事件解耦保证依赖方向单向（feedback → chat）；旁路落库失败只记日志。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SensitiveHitListener {

    private final FilterLogMapper filterLogMapper;

    @EventListener
    public void onSensitiveHit(SensitiveHitEvent event) {
        FilterAction action = FilterAction.fromCode(event.action());
        if (action == null) {
            log.warn("未知的敏感词命中动作：{}", event.action());
            return;
        }
        FilterLog filterLog = new FilterLog();
        filterLog.setUserId(event.userId());
        filterLog.setSessionId(event.sessionId());
        filterLog.setWordId(event.wordId());
        filterLog.setAction(action);
        filterLog.setMatchedAt(LocalDateTime.now());
        try {
            filterLogMapper.insert(filterLog);
        } catch (Exception e) {
            log.warn("filter_log 落库失败（旁路，不影响导诊）：{}", e.getMessage());
        }
    }
}
