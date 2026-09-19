package com.guide.rag.dto;

import java.util.List;

/**
 * 模型输出的结构化解析结果（rag 层产出，业务校验由 chat 层做）。
 *
 * @param verdict        模型判定：RECOMMEND 出结论 / ASK 信息不足需追问
 * @param reply          给患者的自然语言回复（流式内容）
 * @param top3           候选科室与置信度（降序；ask 时为空）
 * @param note           一句话结论说明
 * @param cites          引用注号（对应 RagContext.chunks 下标 + 1）
 * @param confidence     模型自报 top1 置信度；非法或缺失为 null
 * @param confidenceValid 置信度是否通过校验（0–1 区间、Top3 单调递减）
 */
public record RagAnswer(
        Verdict verdict,
        String reply,
        List<DeptCandidate> top3,
        String note,
        List<Integer> cites,
        Double confidence,
        boolean confidenceValid) {

    public enum Verdict {
        RECOMMEND,
        ASK
    }

    public record DeptCandidate(String dept, Double confidence) {
    }

    public static RagAnswer ask(String reply) {
        return new RagAnswer(Verdict.ASK, reply, List.of(), null, List.of(), null, false);
    }
}
