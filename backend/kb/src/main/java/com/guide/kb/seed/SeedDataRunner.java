package com.guide.kb.seed;

import com.guide.common.util.EsChunkUtil;
import com.guide.kb.dto.ChunkInput;
import com.guide.kb.entity.Dept;
import com.guide.kb.entity.KbDoc;
import com.guide.kb.entity.MedicalTerm;
import com.guide.kb.enums.DocStatus;
import com.guide.kb.enums.TermSource;
import com.guide.kb.enums.TermType;
import com.guide.kb.mapper.DeptMapper;
import com.guide.kb.mapper.KbDocMapper;
import com.guide.kb.mapper.MedicalTermMapper;
import com.guide.kb.service.ChunkIndexService;
import com.guide.kb.service.MedicalTermService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * 种子数据装载（开发/演示夹具，需显式开启 {@code guide.seed.enabled=true}）。
 * 幂等：科室表非空即跳过。走 kb 唯一写入口，直写 MySQL 事实 + pgvector 向量 + ES 全文，
 * 与链路 B 上传流水线共用同一套写入代码（差的是解析/切分与异步编排）。
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "guide.seed.enabled", havingValue = "true")
public class SeedDataRunner implements ApplicationRunner {

    private final DeptMapper deptMapper;
    private final KbDocMapper kbDocMapper;
    private final MedicalTermMapper medicalTermMapper;
    private final ChunkIndexService chunkIndexService;
    private final MedicalTermService medicalTermService;
    private final EsChunkUtil esChunkUtil;

    @Override
    public void run(ApplicationArguments args) {
        if (deptMapper.selectCount(null) > 0) {
            log.info("科室蓝本已存在，跳过种子装载");
            return;
        }
        long start = System.currentTimeMillis();
        esChunkUtil.ensureIndex();

        for (SeedCorpus.SeedDept seedDept : SeedCorpus.DEPTS) {
            Dept dept = new Dept();
            dept.setName(seedDept.name());
            dept.setEnabled(1);
            dept.setLocation(seedDept.location());
            dept.setIntro(seedDept.intro());
            deptMapper.insert(dept);

            KbDoc doc = new KbDoc();
            doc.setDeptId(dept.getId());
            doc.setTitle(seedDept.name() + " · 科室知识蓝本");
            doc.setFileUrl("seed://" + dept.getName());
            doc.setStatus(DocStatus.DONE);
            doc.setChunkTotal(seedDept.chunks().size());
            doc.setChunkDone(seedDept.chunks().size());
            kbDocMapper.insert(doc);

            List<ChunkInput> inputs = new ArrayList<>();
            for (int i = 0; i < seedDept.chunks().size(); i++) {
                SeedCorpus.SeedChunk chunk = seedDept.chunks().get(i);
                inputs.add(new ChunkInput(chunk.title(), chunk.content(), i + 1, chunk.terms()));
            }
            chunkIndexService.indexChunks(doc.getId(), dept.getId(), inputs);
            log.info("科室语料入库：{}（{} 条切片）", dept.getName(), inputs.size());
        }

        seedMedicalTerms();
        medicalTermService.refresh();
        log.info("种子数据装载完成，耗时 {} ms", System.currentTimeMillis() - start);
    }

    /** 术语白名单种子（管理端人工维护语义：source=manual，直接 enabled=1） */
    private void seedMedicalTerms() {
        if (medicalTermMapper.selectCount(null) > 0) {
            return;
        }
        for (String term : SeedCorpus.PART_TERMS) {
            insertTerm(term, TermType.PART);
        }
        for (String term : SeedCorpus.SYMPTOM_TERMS) {
            insertTerm(term, TermType.SYMPTOM);
        }
    }

    private void insertTerm(String term, TermType type) {
        MedicalTerm entity = new MedicalTerm();
        entity.setTerm(term);
        entity.setType(type);
        entity.setSource(TermSource.MANUAL);
        entity.setEnabled(1);
        medicalTermMapper.insert(entity);
    }
}
