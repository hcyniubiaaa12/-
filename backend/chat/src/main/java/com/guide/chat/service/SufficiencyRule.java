package com.guide.chat.service;

import com.guide.common.config.PromptProperties;
import com.guide.kb.service.MedicalTermService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * 信息充足性判定的第一层·规则硬门槛（链路 A 配套）：
 * 主诉连"部位 + 症状类型"都没有（如"我不舒服""在吗"）→ 直接模板追问，不浪费模型调用。
 * 第二层（判定与生成合并为一次 LLM 调用）由 PromptBuilder 承担。
 */
@Service
@RequiredArgsConstructor
public class SufficiencyRule {

    /** 过短阈值：低于此长度且不含任何医学术语，判为信息不足 */
    private static final int MIN_CHARS = 6;

    private final MedicalTermService medicalTermService;
    private final PromptProperties prompts;

    /** 是否为过于笼统的首条主诉 */
    public boolean tooVague(String content) {
        if (content == null) {
            return true;
        }
        String text = content.trim();
        return text.length() < MIN_CHARS && !medicalTermService.matches(text);
    }

    /** 模板追问话术：来自 prompts.yml 的 prompts.chat.template-question */
    public String templateQuestion() {
        return prompts.getChat().getTemplateQuestion();
    }
}
