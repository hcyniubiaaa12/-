package com.guide.chat.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.guide.auth.service.SysConfigService;
import com.guide.chat.dto.ChatDTO;
import com.guide.chat.entity.ChatSession;
import com.guide.chat.entity.GuideRecord;
import com.guide.chat.enums.SessionStatus;
import com.guide.chat.mapper.ChatSessionMapper;
import com.guide.chat.mapper.GuideRecordMapper;
import com.guide.common.api.ErrorCode;
import com.guide.common.exception.BizException;
import com.guide.common.model.ChunkHit;
import com.guide.kb.entity.Dept;
import com.guide.kb.service.DeptService;
import com.guide.rag.dto.RagAnswer;
import com.guide.rag.dto.RagContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 导诊记录服务（链路 A 第 ⑥⑦ 步 + 链路 C 前半的挂号确认）。
 * 职责：科室校验（停用仅入口生效）→ 导诊记录落库（rec_top3 / evidence 只写快照）→
 * 挂号确认时同事务写入 actual_dept 与 top1_hit / top3_hit 并置会话 closed。
 * 历史不回改：结论一旦落库即为事实，回流只前向修正知识库。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class GuideService {

    /** 证据快照中 prompt 摘要的最大长度 */
    private static final int PROMPT_SNIPPET_MAX = 2000;

    private final GuideRecordMapper guideRecordMapper;
    private final ChatSessionMapper sessionMapper;
    private final DeptService deptService;
    private final SysConfigService sysConfigService;
    private final ObjectMapper objectMapper;

    /**
     * 结论落库 + 组装 result 事件载荷。
     *
     * @param rawOutput 模型原始输出（证据快照用，审核回放）
     */
    @Transactional(rollbackFor = Exception.class)
    public Conclusion saveConclusion(ChatSession session, RagAnswer answer, RagContext context, String rawOutput) {
        List<Dept> enabledDepts = deptService.listEnabled();
        Map<String, Dept> byName = new LinkedHashMap<>();
        for (Dept dept : enabledDepts) {
            byName.put(dept.getName(), dept);
        }

        // 科室校验：Top3 混入停用/不存在科室则过滤，后续候选顶上
        List<Candidate> kept = new ArrayList<>();
        for (RagAnswer.DeptCandidate candidate : answer.top3()) {
            Dept dept = byName.get(candidate.dept());
            if (dept != null) {
                kept.add(new Candidate(dept, candidate.confidence()));
            } else {
                log.info("推荐校验：科室「{}」不可用已过滤（停用或不存在）", candidate.dept());
            }
        }
        boolean fallback = false;
        if (kept.isEmpty()) {
            // 全被拦：取检索片段的所属科室兜底，走低置信度分流（进盲区榜）
            fallback = true;
            Dept dept = fallbackDept(context, enabledDepts);
            if (dept == null) {
                throw new BizException(ErrorCode.RAG_EMPTY, "知识库暂无可用科室内容");
            }
            kept.add(new Candidate(dept, null));
            log.warn("推荐科室全被拦截，回落检索片段所属科室：{}", dept.getName());
        }

        Double confidence = answer.confidenceValid() && !fallback ? kept.get(0).confidence : null;
        double threshold = sysConfigService.getDouble(SysConfigService.KEY_LOW_CONFIDENCE, 0.5);
        boolean lowConfidence = confidence == null || confidence < threshold;

        GuideRecord record = new GuideRecord();
        record.setSessionId(session.getId());
        record.setRecDeptId(kept.get(0).dept().getId());
        record.setConfidence(confidence);
        record.setRecTop3(top3Json(kept));
        record.setLowConfidence(lowConfidence ? 1 : 0);
        record.setAggregated(0);
        record.setEvidence(evidenceJson(context, answer, rawOutput));
        guideRecordMapper.insert(record);

        session.setHasResult(1);
        sessionMapper.updateById(session);

        return new Conclusion(record, resultPayload(session.getId(), record, kept, answer, context, lowConfidence));
    }

    /**
     * 挂号确认（链路 C 触点 register_success 的事实来源）：
     * actual_dept 与 top1_hit / top3_hit 同事务写入；会话置 closed（新主诉判定双信号之一）。
     */
    @Transactional(rollbackFor = Exception.class)
    public ChatDTO.RegisterVO confirmRegister(String userId, ChatDTO.RegisterReq request) {
        GuideRecord record = guideRecordMapper.selectById(request.getRecordId());
        if (record == null) {
            throw new BizException(ErrorCode.RECORD_NOT_FOUND);
        }
        ChatSession session = sessionMapper.selectById(record.getSessionId());
        if (session == null || !userId.equals(session.getUserId())) {
            throw new BizException(ErrorCode.RECORD_NOT_FOUND);
        }
        if (record.getActualDeptId() != null) {
            throw new BizException(ErrorCode.RECORD_ALREADY_REGISTERED);
        }
        Dept dept = deptService.getById(request.getDeptId());
        if (dept == null) {
            throw new BizException(ErrorCode.DEPT_NOT_FOUND);
        }
        if (dept.getEnabled() == null || dept.getEnabled() != 1) {
            throw new BizException(ErrorCode.DEPT_DISABLED);
        }

        record.setActualDeptId(dept.getId());
        record.setTop1Hit(dept.getId().equals(record.getRecDeptId()) ? 1 : 0);
        record.setTop3Hit(top3Contains(record.getRecTop3(), dept.getId()) ? 1 : 0);
        guideRecordMapper.updateById(record);

        session.setStatus(SessionStatus.CLOSED);
        sessionMapper.updateById(session);

        return new ChatDTO.RegisterVO(session.getId(), dept.getId(), dept.getName(), dept.getLocation());
    }

    /** 检索片段所属科室兜底（取第一个启用科室；片段科室可能已停用） */
    private Dept fallbackDept(RagContext context, List<Dept> enabledDepts) {
        for (ChunkHit chunk : context.chunks()) {
            Dept dept = deptService.getById(chunk.deptId());
            if (dept != null && dept.getEnabled() != null && dept.getEnabled() == 1) {
                return dept;
            }
        }
        return enabledDepts.isEmpty() ? null : enabledDepts.get(0);
    }

    /** Top3 快照（含 deptId：看板排序精度聚合与 top3_hit 比对都依赖它） */
    private String top3Json(List<Candidate> kept) {
        ArrayNode array = objectMapper.createArrayNode();
        for (Candidate candidate : kept) {
            ObjectNode node = array.addObject();
            node.put("deptId", candidate.dept().getId());
            node.put("dept", candidate.dept().getName());
            if (candidate.confidence() == null) {
                node.putNull("confidence");
            } else {
                node.put("confidence", candidate.confidence());
            }
        }
        return array.toString();
    }

    /** 证据快照（只写）：系统当时看到了什么、怎么答的，供审核回放与根因归因 */
    private String evidenceJson(RagContext context, RagAnswer answer, String rawOutput) {
        ObjectNode evidence = objectMapper.createObjectNode();
        ArrayNode retrieved = evidence.putArray("retrieved");
        int rank = 1;
        StringBuilder snippet = new StringBuilder();
        for (ChunkHit chunk : context.chunks()) {
            ObjectNode node = retrieved.addObject();
            node.put("chunk_id", chunk.chunkId());
            node.put("title", chunk.title());
            node.put("score", chunk.score());
            node.put("rank", rank++);
            snippet.append('注').append(rank - 1).append('《').append(chunk.title()).append("》：")
                    .append(abbreviate(chunk.content(), 200)).append('\n');
        }
        evidence.put("retrieved_query", context.rewrittenQuery());
        ArrayNode cited = evidence.putArray("model_cited");
        answer.cites().forEach(cited::add);
        evidence.put("prompt_snippet", abbreviate(snippet.toString(), PROMPT_SNIPPET_MAX));
        evidence.put("model_output_raw", abbreviate(rawOutput, PROMPT_SNIPPET_MAX));
        return evidence.toString();
    }

    private ChatDTO.ResultVO resultPayload(String sessionId, GuideRecord record, List<Candidate> kept,
                                           RagAnswer answer, RagContext context, boolean lowConfidence) {
        List<ChatDTO.Top3Item> top3 = kept.stream()
                .map(candidate -> new ChatDTO.Top3Item(candidate.dept().getName(), percent(candidate.confidence())))
                .toList();
        // 判断依据脚注：注号 = 证据快照 retrieved 顺序（与 Prompt 中的「注N」一致）
        List<ChatDTO.Cite> cites = new ArrayList<>();
        List<ChunkHit> chunks = context.chunks();
        for (int i = 0; i < chunks.size(); i++) {
            cites.add(new ChatDTO.Cite(i + 1, chunks.get(i).title()));
        }
        return new ChatDTO.ResultVO(sessionId, record.getId(), record.getRecDeptId(),
                kept.get(0).dept().getName(), record.getConfidence(), top3, answer.note(),
                cites, lowConfidence);
    }

    private int percent(Double confidence) {
        return confidence == null ? 0 : (int) Math.round(confidence * 100);
    }

    private boolean top3Contains(String recTop3, String deptId) {
        if (recTop3 == null || recTop3.isBlank()) {
            return false;
        }
        try {
            JsonNode array = objectMapper.readTree(recTop3);
            for (JsonNode node : array) {
                if (deptId.equals(node.path("deptId").asText())) {
                    return true;
                }
            }
        } catch (Exception e) {
            log.warn("Top3 快照解析失败：{}", e.getMessage());
        }
        return false;
    }

    private String abbreviate(String text, int max) {
        if (text == null) {
            return "";
        }
        return text.length() > max ? text.substring(0, max) + "..." : text;
    }

    /** 结论产出：落库记录 + SSE result 载荷 */
    public record Conclusion(GuideRecord record, ChatDTO.ResultVO payload) {
    }

    /** 通过校验的候选科室 */
    private record Candidate(Dept dept, Double confidence) {
    }
}
