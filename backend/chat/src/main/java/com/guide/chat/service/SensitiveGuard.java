package com.guide.chat.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.guide.auth.entity.SensitiveWord;
import com.guide.auth.enums.SensitiveWordType;
import com.guide.auth.mapper.SensitiveWordMapper;
import com.guide.chat.event.SensitiveHitEvent;
import com.guide.common.config.PromptProperties;
import com.guide.kb.service.MedicalTermService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 敏感词入口前置校验（链路 A 的 6.1 校验顺序）。
 *
 * <p>医院导诊的敏感词与通用内容审核不同：部位/症状名词（胸、腹、下体等）是分诊核心信息，不能拦；
 * 真正要拦的是过激言论与辱骂。「疼死了」「要死了」是真实痛苦表达，不拦。
 *
 * <p>判定口径（对齐 6.1 的"白名单优先、宁可少拦"）：命中的禁止词**本身**是医学术语白名单里的词 →
 * 放行（防误杀）；否则拦截。这样既不会因主诉里出现"胸"而漏掉脏话，也不会因词库里混入部位词而误杀主诉。
 *
 * <p>词库实时查表（表小且带索引），不缓存不落 Redis——管理端增删停用即时生效。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SensitiveGuard {

    private final SensitiveWordMapper sensitiveWordMapper;
    private final MedicalTermService medicalTermService;
    private final ApplicationEventPublisher eventPublisher;
    private final PromptProperties prompts;

    public GuardResult check(String userId, String sessionId, String content) {
        if (content == null || content.isBlank()) {
            return GuardResult.pass();
        }
        List<SensitiveWord> words = sensitiveWordMapper.selectList(Wrappers.<SensitiveWord>lambdaQuery()
                .eq(SensitiveWord::getEnabled, 1));
        if (words.isEmpty()) {
            log.debug("入口校验：词库无启用词，直接放行");
            return GuardResult.pass();
        }
        log.debug("入口校验：加载启用词 {} 条（禁止 {} / 观察 {}）", words.size(),
                words.stream().filter(w -> w.getType() == SensitiveWordType.BANNED).count(),
                words.stream().filter(w -> w.getType() == SensitiveWordType.WATCH).count());
        GuardResult watched = null;
        for (SensitiveWord word : words) {
            if (word.getWord() == null || !content.contains(word.getWord())) {
                continue;
            }
            if (word.getType() == SensitiveWordType.WATCH) {
                if (watched == null) {
                    watched = GuardResult.watched(word.getWord());
                    recordHit(userId, sessionId, word, SensitiveHitEvent.WATCHED);
                }
                continue;
            }
            if (medicalTermService.isTerm(word.getWord())) {
                // 白名单优先：该词本身是医学术语（部位/症状），防误杀放行。
                // 精确匹配，不能用子串——白名单含「心/头/手」等单字，子串判定会让「黑心医院」漏拦
                continue;
            }
            recordHit(userId, sessionId, word, SensitiveHitEvent.BLOCKED);
            // 拦截话术来自 prompts.yml 的 prompts.chat.blocked-reply
            return GuardResult.blocked(word.getWord(), prompts.getChat().getBlockedReply());
        }
        return watched != null ? watched : GuardResult.pass();
    }

    /** 命中次数累加 + 旁路日志事件；任何失败都不影响主流程 */
    private void recordHit(String userId, String sessionId, SensitiveWord word, String action) {
        try {
            sensitiveWordMapper.update(null, Wrappers.<SensitiveWord>lambdaUpdate()
                    .setSql("hit_count = hit_count + 1")
                    .eq(SensitiveWord::getId, word.getId()));
            eventPublisher.publishEvent(new SensitiveHitEvent(userId, sessionId, word.getId(), action));
        } catch (Exception e) {
            log.warn("敏感词命中留痕失败（不影响导诊）：{}", e.getMessage());
        }
    }

    /** 入口校验结果 */
    public record GuardResult(Action action, String word, String reply) {

        public enum Action {
            /** 放行 */
            PASS,
            /** 命中观察词：放行 + 留痕 */
            WATCHED,
            /** 命中禁止词：拦截 */
            BLOCKED
        }

        static GuardResult pass() {
            return new GuardResult(Action.PASS, null, null);
        }

        static GuardResult watched(String word) {
            return new GuardResult(Action.WATCHED, word, null);
        }

        static GuardResult blocked(String word, String reply) {
            return new GuardResult(Action.BLOCKED, word, reply);
        }
    }
}
