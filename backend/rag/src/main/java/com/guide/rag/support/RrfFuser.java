package com.guide.rag.support;

import com.guide.common.model.ChunkHit;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * RRF（倒数排名融合）：混合检索的两路召回结果融合，纯代码实现、不调模型。
 * score(d) = Σ 1/(k + rank_r(d))，k 默认 60（见《已定参数汇总》）。
 * 同一片段在多路命中则分数累加；跨路分数不可比（BM25 与余弦相似度量纲不同），只排名可比。
 */
public final class RrfFuser {

    public static final int DEFAULT_K = 60;

    private RrfFuser() {
    }

    /**
     * @param rankedLists 多路召回结果，每路已按相关度降序
     * @return 融合后按 RRF 分数降序的候选片段
     */
    public static List<ChunkHit> fuse(List<List<ChunkHit>> rankedLists, int k) {
        int rrfK = k <= 0 ? DEFAULT_K : k;
        Map<String, ChunkHit> firstSeen = new LinkedHashMap<>();
        Map<String, Double> scores = new HashMap<>();
        for (List<ChunkHit> list : rankedLists) {
            if (list == null) {
                continue;
            }
            for (int i = 0; i < list.size(); i++) {
                ChunkHit hit = list.get(i);
                if (hit == null || hit.chunkId() == null) {
                    continue;
                }
                firstSeen.putIfAbsent(hit.chunkId(), hit);
                scores.merge(hit.chunkId(), 1.0 / (rrfK + i + 1), Double::sum);
            }
        }
        List<ChunkHit> fused = new ArrayList<>(firstSeen.size());
        for (Map.Entry<String, ChunkHit> entry : firstSeen.entrySet()) {
            ChunkHit hit = entry.getValue();
            fused.add(new ChunkHit(hit.chunkId(), hit.deptId(), hit.title(), hit.content(),
                    scores.get(hit.chunkId())));
        }
        // 稳定排序：分数相同保持先出现的在前
        fused.sort(Comparator.comparingDouble(ChunkHit::score).reversed());
        return fused;
    }
}
