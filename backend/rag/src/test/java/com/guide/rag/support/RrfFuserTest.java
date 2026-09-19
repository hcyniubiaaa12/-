package com.guide.rag.support;

import com.guide.common.model.ChunkHit;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * RRF 融合单测：纯函数、零成本，验证排名贡献与去重。
 */
class RrfFuserTest {

    @Test
    @DisplayName("两路命中的片段分数累加，排名高于单路命中")
    void fuseAccumulatesAcrossLists() {
        ChunkHit a = hit("a", 0.9);
        ChunkHit b = hit("b", 0.8);
        ChunkHit c = hit("c", 0.7);
        ChunkHit d = hit("d", 0.6);

        // 向量路：a b c；关键词路：d a
        List<ChunkHit> fused = RrfFuser.fuse(List.of(List.of(a, b, c), List.of(d, a)), 60);

        assertThat(fused).extracting(ChunkHit::chunkId).containsExactly("a", "d", "b", "c");
        // a = 1/61 + 1/62（两路命中），其余各 1/(60+rank)
        assertThat(fused.get(0).score()).isEqualTo(1.0 / 61 + 1.0 / 62, org.assertj.core.data.Offset.offset(1e-9));
        assertThat(fused.get(1).score()).isEqualTo(1.0 / 61, org.assertj.core.data.Offset.offset(1e-9));
    }

    @Test
    @DisplayName("空输入返回空；单路召回保持原顺序")
    void fuseHandlesEmptyAndSingleList() {
        assertThat(RrfFuser.fuse(List.of(), 60)).isEmpty();
        // 某一路为空或为 null（如 ES 降级）时不影响另一路
        assertThat(RrfFuser.fuse(java.util.Arrays.asList(null, List.of()), 60)).isEmpty();

        List<ChunkHit> single = RrfFuser.fuse(List.of(List.of(hit("x", 0.1), hit("y", 0.2))), 60);
        assertThat(single).extracting(ChunkHit::chunkId).containsExactly("x", "y");
    }

    @Test
    @DisplayName("k 非法时回落到默认 60")
    void fuseFallsBackToDefaultK() {
        List<ChunkHit> fused = RrfFuser.fuse(List.of(List.of(hit("a", 0.5))), 0);
        assertThat(fused.get(0).score()).isEqualTo(1.0 / 61, org.assertj.core.data.Offset.offset(1e-9));
    }

    private ChunkHit hit(String id, double score) {
        return new ChunkHit(id, "dept-" + id, "标题" + id, "正文" + id, score);
    }
}
