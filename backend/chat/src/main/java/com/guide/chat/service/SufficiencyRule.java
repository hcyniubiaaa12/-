package com.guide.chat.service;

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

    private static final String TEMPLATE_QUESTION =
            "请补充一下：具体是哪个部位不舒服？是什么样的感觉（疼、闷、胀、痒、麻）？大概持续多久了？";

    private final MedicalTermService medicalTermService;

    /** 是否为过于笼统的首条主诉 */
    public boolean tooVague(String content) {
        if (content == null) {
            return true;
        }
        String text = content.trim();
        return text.length() < MIN_CHARS && !medicalTermService.matches(text);
    }

    public String templateQuestion() {
        return TEMPLATE_QUESTION;
    }
}
