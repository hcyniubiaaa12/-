package com.guide.kb.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.guide.kb.entity.MedicalTerm;
import com.guide.kb.mapper.MedicalTermMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;

/**
 * 医学术语白名单（链路 A 入口防误杀 + 信息充足性规则门槛）。
 * MySQL medical_term 是**唯一生效源**：内存 Set 从这里加载，ES 聚合只是候选池；
 * 知识库更新（回流 / 重新入库 / 管理端增删）后调用 {@link #refresh()} 失效重建。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MedicalTermService {

    private final MedicalTermMapper medicalTermMapper;

    /** 惰性加载的启用术语集合（不可变快照，刷新即整体替换） */
    private final AtomicReference<Set<String>> enabledTerms = new AtomicReference<>();

    /** 启用术语集合（首次访问时加载） */
    public Set<String> enabledTerms() {
        Set<String> terms = enabledTerms.get();
        if (terms == null) {
            terms = load();
            enabledTerms.set(terms);
        }
        return terms;
    }

    /** 文本是否命中医学术语白名单（含任一词即命中） */
    public boolean matches(String text) {
        if (text == null || text.isBlank()) {
            return false;
        }
        for (String term : enabledTerms()) {
            if (text.contains(term)) {
                return true;
            }
        }
        return false;
    }

    /** 失效重建（管理端增删/停用、知识库更新后调用） */
    public void refresh() {
        enabledTerms.set(load());
        log.info("医学术语白名单已刷新，启用术语 {} 条", enabledTerms().size());
    }

    private Set<String> load() {
        Set<String> terms = new HashSet<>();
        for (MedicalTerm term : medicalTermMapper.selectList(Wrappers.<MedicalTerm>lambdaQuery()
                .eq(MedicalTerm::getEnabled, 1))) {
            if (term.getTerm() != null && !term.getTerm().isBlank()) {
                terms.add(term.getTerm().trim());
            }
        }
        return Set.copyOf(terms);
    }
}
