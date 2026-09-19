package com.guide.rag.support;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.guide.rag.dto.RagAnswer;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 模型输出解析单测：覆盖正常结论、纯追问、畸形输出三类分支。
 */
class AnswerParserTest {

    private final AnswerParser parser = new AnswerParser(new ObjectMapper());

    @Test
    @DisplayName("正常结论：自然语言 + 分隔符 + JSON")
    void parsesRecommend() {
        String output = """
                活动后胸闷加重、休息可缓解，结合片段提示优先排查心脏来源。注1 支持心血管内科，
                注2 提到的呼吸系统鉴别暂不吻合。
                ---RESULT---
                {"dept":"心血管内科","confidence":0.82,"top3":[{"dept":"心血管内科","confidence":0.82},{"dept":"呼吸内科","confidence":0.11}],"note":"优先排查心脏来源","cites":[1,2]}
                """;

        RagAnswer answer = parser.parse(output);

        assertThat(answer.verdict()).isEqualTo(RagAnswer.Verdict.RECOMMEND);
        assertThat(answer.reply()).contains("活动后胸闷加重").doesNotContain(AnswerParser.MARKER);
        assertThat(answer.top3()).extracting(RagAnswer.DeptCandidate::dept).containsExactly("心血管内科", "呼吸内科");
        assertThat(answer.confidence()).isEqualTo(0.82);
        assertThat(answer.confidenceValid()).isTrue();
        assertThat(answer.cites()).containsExactly(1, 2);
        assertThat(answer.note()).isEqualTo("优先排查心脏来源");
    }

    @Test
    @DisplayName("无分隔符 = 信息不足，按追问处理")
    void treatAsAskWhenNoMarker() {
        RagAnswer answer = parser.parse("这个胸闷大概持续多久了？有没有向左肩放射？");

        assertThat(answer.verdict()).isEqualTo(RagAnswer.Verdict.ASK);
        assertThat(answer.reply()).isEqualTo("这个胸闷大概持续多久了？有没有向左肩放射？");
        assertThat(answer.top3()).isEmpty();
    }

    @Test
    @DisplayName("分隔符后 JSON 畸形：按追问兜底，不抛异常")
    void malformedJsonFallsBackToAsk() {
        RagAnswer answer = parser.parse("建议就诊。\n" + AnswerParser.MARKER + "\n{dept: 心血管内科");

        assertThat(answer.verdict()).isEqualTo(RagAnswer.Verdict.ASK);
        assertThat(answer.reply()).isEqualTo("建议就诊。");
    }

    @Test
    @DisplayName("置信度越界：标记为不可信，由上层置 null 走低置信度分流")
    void invalidConfidenceMarked() {
        RagAnswer tooHigh = parser.parse(AnswerParser.MARKER + "\n{\"dept\":\"骨科\",\"confidence\":1.5,\"top3\":[{\"dept\":\"骨科\",\"confidence\":1.5}]}");
        RagAnswer negative = parser.parse(AnswerParser.MARKER + "\n{\"dept\":\"骨科\",\"confidence\":-0.2}");
        RagAnswer missing = parser.parse(AnswerParser.MARKER + "\n{\"dept\":\"骨科\"}");

        assertThat(tooHigh.confidenceValid()).isFalse();
        assertThat(negative.confidenceValid()).isFalse();
        assertThat(missing.confidenceValid()).isFalse();
    }

    @Test
    @DisplayName("Top3 置信度非递减：整体判为不可信")
    void nonMonotonicTop3Marked() {
        RagAnswer answer = parser.parse(AnswerParser.MARKER
                + "\n{\"dept\":\"骨科\",\"confidence\":0.5,\"top3\":[{\"dept\":\"骨科\",\"confidence\":0.5},{\"dept\":\"神经内科\",\"confidence\":0.8}]}");

        assertThat(answer.confidenceValid()).isFalse();
        assertThat(answer.top3()).hasSize(2);
    }

    @Test
    @DisplayName("容错：```json 围栏、JSON 后多余文字、cites 对象形态")
    void toleratesFormatNoise() {
        RagAnswer fenced = parser.parse("说明。\n" + AnswerParser.MARKER
                + "\n```json\n{\"dept\":\"消化内科\",\"confidence\":0.7,\"cites\":[{\"no\":1}]}\n```\n以上供参考。");
        RagAnswer noisy = parser.parse(AnswerParser.MARKER
                + "\n{\"dept\":\"消化内科\",\"confidence\":0.7,\"top3\":[{\"dept\":\"消化内科\",\"confidence\":0.7}]} 请以医生面诊为准。");

        assertThat(fenced.verdict()).isEqualTo(RagAnswer.Verdict.RECOMMEND);
        assertThat(fenced.cites()).containsExactly(1);
        assertThat(noisy.verdict()).isEqualTo(RagAnswer.Verdict.RECOMMEND);
        assertThat(noisy.top3()).hasSize(1);
    }

    @Test
    @DisplayName("只给 JSON 没给自然语言：仍按结论处理，reply 为空")
    void jsonOnlyStillRecommend() {
        RagAnswer answer = parser.parse("{\"dept\":\"皮肤科\",\"confidence\":0.6}");

        assertThat(answer.verdict()).isEqualTo(RagAnswer.Verdict.RECOMMEND);
        assertThat(answer.reply()).isEmpty();
    }

    @Test
    @DisplayName("top3 超过 3 条时截断为 3 条")
    void truncatesTop3() {
        RagAnswer answer = parser.parse(AnswerParser.MARKER + """
                {"dept":"骨科","confidence":0.6,"top3":[{"dept":"骨科","confidence":0.6},{"dept":"神经内科","confidence":0.3},{"dept":"风湿免疫科","confidence":0.2},{"dept":"康复科","confidence":0.1}]}""");

        assertThat(answer.top3()).hasSize(3);
    }
}
