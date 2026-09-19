package com.guide.kb.service;

import com.guide.kb.entity.KbChunk;
import com.guide.kb.mapper.KbChunkMapper;
import com.guide.rag.spi.ChunkTextProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 切片正文读取端口的 kb 侧实现：从 MySQL kb_chunk（事实源）批量取标题与正文。
 * 逻辑删除的切片由 MyBatis-Plus 自动过滤，rag 侧自然丢弃其残留向量。
 */
@Service
@RequiredArgsConstructor
public class KbChunkTextProvider implements ChunkTextProvider {

    private final KbChunkMapper chunkMapper;

    @Override
    public Map<String, ChunkText> loadTexts(Collection<String> chunkIds) {
        if (chunkIds == null || chunkIds.isEmpty()) {
            return Map.of();
        }
        List<KbChunk> chunks = chunkMapper.selectBatchIds(chunkIds);
        Map<String, ChunkText> texts = new LinkedHashMap<>();
        for (KbChunk chunk : chunks) {
            texts.put(chunk.getId(), new ChunkText(chunk.getTitle(), chunk.getContent()));
        }
        return texts;
    }
}
