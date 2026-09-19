package com.guide.rag.support;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.guide.rag.dto.RagAnswer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * 模型输出解析（链路 A 第 ⑥ 步）。
 * 协议：自然语言回复在前，结论以分隔符 {@value #MARKER} + JSON 对象收尾；
 * 只输出自然语言 = 信息不足需追问（ASK）。
 * 容错：JSON 前后多余文字按花括号配对截取；置信度非法（越界/非递减）置 null 并按低置信度分流。
 */
@Slf4j
@Component
public class AnswerParser {

    /** 结论分隔标记（Prompt 中约定） */
    public static final String MARKER = "---RESULT---";

    private static final int MAX_TOP3 = 3;

    private final ObjectMapper objectMapper;

    public AnswerParser(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public RagAnswer parse(String modelOutput) {
        if (!StringUtils.hasText(modelOutput)) {
            return RagAnswer.ask("");
        }
        int markerIndex = modelOutput.indexOf(MARKER);
        if (markerIndex < 0) {
            // 无分隔符：可能是纯追问，也可能是只给了 JSON（无自然语言）——后者按结论处理
            String json = extractJsonObject(modelOutput);
            if (json == null) {
                return RagAnswer.ask(modelOutput.trim());
            }
            return buildAnswer("", json);
        }
        String reply = modelOutput.substring(0, markerIndex).trim();
        String json = extractJsonObject(modelOutput.substring(markerIndex + MARKER.length()));
        if (json == null) {
            log.warn("模型输出了结论分隔符但没有合法 JSON，按追问处理：{}", abbreviate(modelOutput));
            return RagAnswer.ask(reply);
        }
        return buildAnswer(reply, json);
    }

    private RagAnswer buildAnswer(String reply, String json) {
        RawResult raw;
        try {
            raw = objectMapper.readValue(json, RawResult.class);
        } catch (Exception e) {
            log.warn("结论 JSON 解析失败，按追问处理：{}", abbreviate(json));
            return RagAnswer.ask(reply);
        }
        if (raw == null || !StringUtils.hasText(raw.dept())) {
            return RagAnswer.ask(reply);
        }

        List<RagAnswer.DeptCandidate> top3 = new ArrayList<>();
        Double firstConfidence = raw.confidence();
        if (raw.top3() != null) {
            for (RawDept item : raw.top3()) {
                if (item == null || !StringUtils.hasText(item.dept())) {
                    continue;
                }
                top3.add(new RagAnswer.DeptCandidate(item.dept().trim(), item.confidence()));
                if (top3.size() == MAX_TOP3) {
                    break;
                }
            }
        }
        if (top3.isEmpty()) {
            top3.add(new RagAnswer.DeptCandidate(raw.dept().trim(), firstConfidence));
        }
        // 模型可能只在顶层给 confidence：以顶层为准，保证与推荐科室一致
        if (firstConfidence != null && !top3.isEmpty()) {
            top3.set(0, new RagAnswer.DeptCandidate(top3.get(0).dept(), firstConfidence));
        }
        String note = raw.note() == null ? "" : raw.note().trim();
        return new RagAnswer(RagAnswer.Verdict.RECOMMEND, reply, top3, note,
                parseCites(raw.cites()), firstConfidence, confidenceValid(top3));
    }

    /**
     * 置信度校验（见链路 A 对齐点）：0–1 区间、Top3 单调递减（允许相等）。
     * 不合法时整体置为不可信，由 chat 层置 null 并按低置信度分流（进盲区榜）。
     */
    private boolean confidenceValid(List<RagAnswer.DeptCandidate> top3) {
        Double previous = null;
        for (RagAnswer.DeptCandidate candidate : top3) {
            Double confidence = candidate.confidence();
            if (confidence == null || confidence < 0 || confidence > 1) {
                return false;
            }
            if (previous != null && confidence > previous) {
                return false;
            }
            previous = confidence;
        }
        return true;
    }

    /** cites 宽松解析：支持 [1,2] 与 [{"no":1},{"no":2}] 两种形态 */
    private List<Integer> parseCites(JsonNode cites) {
        List<Integer> result = new ArrayList<>();
        if (cites == null || !cites.isArray()) {
            return result;
        }
        for (JsonNode node : cites) {
            if (node.isInt()) {
                result.add(node.asInt());
            } else if (node.has("no") && node.get("no").isInt()) {
                result.add(node.get("no").asInt());
            } else if (node.isTextual()) {
                try {
                    result.add(Integer.parseInt(node.asText().replaceAll("\\D", "")));
                } catch (NumberFormatException ignored) {
                    // 注号不可解析则忽略该条
                }
            }
        }
        return result;
    }

    /** 花括号配对截取第一个完整 JSON 对象（容忍前后多余文字与 ```json 围栏） */
    private String extractJsonObject(String text) {
        if (text == null) {
            return null;
        }
        int start = text.indexOf('{');
        if (start < 0) {
            return null;
        }
        int depth = 0;
        boolean inString = false;
        boolean escaped = false;
        for (int i = start; i < text.length(); i++) {
            char c = text.charAt(i);
            if (inString) {
                if (escaped) {
                    escaped = false;
                } else if (c == '\\') {
                    escaped = true;
                } else if (c == '"') {
                    inString = false;
                }
                continue;
            }
            if (c == '"') {
                inString = true;
            } else if (c == '{') {
                depth++;
            } else if (c == '}') {
                depth--;
                if (depth == 0) {
                    return text.substring(start, i + 1);
                }
            }
        }
        return null;
    }

    private String abbreviate(String text) {
        return text.length() > 200 ? text.substring(0, 200) + "..." : text;
    }

    /** 模型结论原始结构（宽松映射：多余字段忽略、缺失字段置 null） */
    @JsonIgnoreProperties(ignoreUnknown = true)
    private record RawResult(String dept, Double confidence, List<RawDept> top3, String note, JsonNode cites) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record RawDept(String dept, Double confidence) {
    }
}
