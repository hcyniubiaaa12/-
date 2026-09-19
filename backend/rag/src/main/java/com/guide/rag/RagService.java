package com.guide.rag;

import com.guide.common.exception.BizException;
import com.guide.common.model.ChunkHit;
import com.guide.common.util.EsChunkUtil;
import com.guide.common.util.PgVectorUtil;
import com.guide.llm.client.ChatModel;
import com.guide.llm.client.ChatMsg;
import com.guide.llm.client.EmbeddingModel;
import com.guide.llm.client.RerankModel;
import com.guide.rag.dto.RagContext;
import com.guide.rag.dto.RagRequest;
import com.guide.rag.dto.RagTurn;
import com.guide.rag.spi.ChunkTextProvider;
import com.guide.rag.support.PromptBuilder;
import com.guide.rag.support.RrfFuser;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Collectors;

/**
 * RAG 检索层（技术核心）：查询改写 → 双路召回 → RRF 融合 → 精排 → Prompt 拼装。
 * 不感知业务状态（会话、用户）——科室范围与轮次约束均由入参给定。
 * 降级策略：ES 不可用退化为向量单路；精排失败退化为 RRF 顺序；改写失败退回原查询。
 * 唯一不可降级的是 embedding——没有查询向量就没有召回，直接抛出由上层转 SSE error。
 */
@Slf4j
@Service
public class RagService {

    /** 最近历史参与改写的条数上限（控制 token 与延迟） */
    private static final int REWRITE_HISTORY_LIMIT = 6;
    private static final int REWRITE_MAX_CHARS = 80;

    private final EmbeddingModel embeddingModel;
    private final RerankModel rerankModel;
    private final ChatModel chatModel;
    private final PgVectorUtil pgVectorUtil;
    private final EsChunkUtil esChunkUtil;
    private final ChunkTextProvider chunkTextProvider;
    private final PromptBuilder promptBuilder;

    public RagService(EmbeddingModel embeddingModel, RerankModel rerankModel, ChatModel chatModel,
                      PgVectorUtil pgVectorUtil, EsChunkUtil esChunkUtil,
                      ChunkTextProvider chunkTextProvider, PromptBuilder promptBuilder) {
        this.embeddingModel = embeddingModel;
        this.rerankModel = rerankModel;
        this.chatModel = chatModel;
        this.pgVectorUtil = pgVectorUtil;
        this.esChunkUtil = esChunkUtil;
        this.chunkTextProvider = chunkTextProvider;
        this.promptBuilder = promptBuilder;
    }

    /** 检索：产出送入 Prompt 的精排片段与证据素材 */
    public RagContext retrieve(RagRequest request) {
        String query = rewriteQuery(request);
        float[] queryVector = embeddingModel.embed(query);

        List<ChunkHit> vectorHits = pgVectorUtil.searchChunks(queryVector, request.topK());
        List<ChunkHit> esHits = esChunkUtil.searchChunks(query, request.topK());
        List<ChunkHit> fused = fillTexts(RrfFuser.fuse(List.of(vectorHits, esHits), RrfFuser.DEFAULT_K));
        if (fused.isEmpty()) {
            log.info("检索无命中，query={}", query);
            return new RagContext(request.query(), query, List.of(), vectorHits.size(), esHits.size());
        }
        List<ChunkHit> chunks = rerank(query, fused, request.topN());
        return new RagContext(request.query(), query, chunks, vectorHits.size(), esHits.size());
    }

    /**
     * 回填切片正文（MySQL 事实源）。向量路只回 id，正文必须现取；
     * 顺带丢弃「向量在库、元数据已删」的脏命中，保证 Prompt 里的片段都可溯源。
     */
    private List<ChunkHit> fillTexts(List<ChunkHit> fused) {
        if (fused.isEmpty()) {
            return fused;
        }
        Set<String> chunkIds = fused.stream().map(ChunkHit::chunkId).collect(Collectors.toSet());
        Map<String, ChunkTextProvider.ChunkText> texts = chunkTextProvider.loadTexts(chunkIds);
        List<ChunkHit> filled = new ArrayList<>(fused.size());
        for (ChunkHit hit : fused) {
            ChunkTextProvider.ChunkText text = texts.get(hit.chunkId());
            if (text != null) {
                filled.add(new ChunkHit(hit.chunkId(), hit.deptId(), text.title(), text.content(), hit.score()));
            } else {
                log.warn("命中片段在 MySQL 已不存在，已丢弃：chunkId={}", hit.chunkId());
            }
        }
        return filled;
    }

    /**
     * 流式生成：拼 Prompt（知识片段 + 历史 + 本轮输入）后调模型逐段回调。
     * 这是 chat 侧唯一的生成入口——chat 不直连 LLM，只经 rag → llm（见 CLAUDE.md 依赖规范）。
     *
     * @return 拼接后的完整输出（含结论 JSON 部分，由 {@link AnswerParser} 解析）
     */
    public String streamAnswer(RagRequest request, RagContext context, Consumer<String> onDelta) {
        List<ChatMsg> messages = promptBuilder.build(request, context);
        return chatModel.chatStream(messages, onDelta);
    }

    /**
     * 查询改写：多轮对话时把「它/还有/那个」等指代还原为独立可检索的查询。
     * 失败（模型不可用）不阻断主流程，退回患者原话。
     */
    private String rewriteQuery(RagRequest request) {
        if (!request.multiTurn()) {
            return request.query();
        }
        try {
            StringBuilder dialogue = new StringBuilder();
            List<RagTurn> history = request.history();
            int from = Math.max(0, history.size() - REWRITE_HISTORY_LIMIT);
            for (RagTurn msg : history.subList(from, history.size())) {
                dialogue.append("assistant".equals(msg.role()) ? "助手：" : "患者：")
                        .append(msg.content()).append('\n');
            }
            dialogue.append("患者：").append(request.query());

            String rewritten = chatModel.chat(List.of(
                    ChatMsg.system("你是检索查询改写助手。把多轮问诊对话中患者最后一句补充信息，改写成一句"
                            + "独立、完整、可用于知识库检索的症状描述：补全指代（它/那个/还有），保留部位与症状关键词，"
                            + "不要添加患者没说过的症状，不要解释，只输出改写后的这一句话。"),
                    ChatMsg.user(dialogue.toString())));
            String cleaned = cleanup(rewritten);
            if (!StringUtils.hasText(cleaned)) {
                return request.query();
            }
            log.debug("查询改写：{} → {}", request.query(), cleaned);
            return cleaned;
        } catch (BizException e) {
            log.warn("查询改写失败，使用患者原话检索：{}", e.getMessage());
            return request.query();
        }
    }

    /** 清理改写结果：去引号/换行/前缀说明，超长截断 */
    private String cleanup(String rewritten) {
        if (!StringUtils.hasText(rewritten)) {
            return "";
        }
        String cleaned = rewritten.trim()
                .replaceAll("^[\"'「『]|[\"'」』]$", "")
                .replaceAll("^(改写后|改写结果|查询)[:：]", "")
                .replaceAll("\\s+", " ")
                .trim();
        return cleaned.length() > REWRITE_MAX_CHARS ? cleaned.substring(0, REWRITE_MAX_CHARS) : cleaned;
    }

    /** 精排 Top-N；失败退化为 RRF 顺序，保证导诊不中断 */
    private List<ChunkHit> rerank(String query, List<ChunkHit> fused, int topN) {
        List<String> documents = fused.stream()
                .map(hit -> (hit.title() == null ? "" : hit.title() + "。") + hit.content())
                .toList();
        try {
            List<RerankModel.RerankHit> hits = rerankModel.rerank(query, documents, topN);
            List<ChunkHit> ranked = hits.stream()
                    .filter(hit -> hit.index() >= 0 && hit.index() < fused.size())
                    .map(hit -> withScore(fused.get(hit.index()), hit.score()))
                    .toList();
            return ranked.isEmpty() ? fused.stream().limit(topN).toList() : ranked;
        } catch (BizException e) {
            log.warn("精排失败，退化为 RRF 融合顺序：{}", e.getMessage());
            return fused.stream().limit(topN).toList();
        }
    }

    private ChunkHit withScore(ChunkHit hit, double score) {
        return new ChunkHit(hit.chunkId(), hit.deptId(), hit.title(), hit.content(), score);
    }
}
