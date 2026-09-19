package com.guide.kb.dto;

import java.util.List;

/**
 * 待入库的知识片段（入库流水线切分产物 / 回流合成 chunk / 种子语料共用）。
 *
 * @param title   切片标题（溯源引用）
 * @param content 切片正文
 * @param seq     切片序号
 * @param terms   医学术语（随 chunk 入 ES，供 terms 聚合产出白名单候选池）
 */
public record ChunkInput(String title, String content, int seq, List<String> terms) {

    public ChunkInput {
        terms = terms == null ? List.of() : List.copyOf(terms);
    }
}
