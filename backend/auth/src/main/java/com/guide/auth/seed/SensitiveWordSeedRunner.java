package com.guide.auth.seed;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.guide.auth.entity.SensitiveWord;
import com.guide.auth.enums.SensitiveWordType;
import com.guide.auth.mapper.SensitiveWordMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 敏感词库基础种子（开发夹具，guide.seed.enabled=true 时生效）。
 *
 * <p>为什么需要：词库是管理端维护的，但空库时演示「拦截」无从触发——
 * 患者骂一句脏话却一路放行到模型，看起来像功能没做。
 *
 * <p>口径（见《总体架构与链路设计.md》§6）：**辱骂/过激言论拦（banned），维权表达只观察（watch）**。
 * 「投诉」「退钱」「曝光」是患者正当诉求，拦了只会激化矛盾——放行 + 留痕观察即可。
 *
 * <p>幂等：按词去重，已存在的词（含管理端手工添加的）不覆盖、不改类型。
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "guide.seed.enabled", havingValue = "true")
public class SensitiveWordSeedRunner implements ApplicationRunner {

    /** 禁止词：辱骂、过激言论、对医院的恶意指控——命中即拦截 */
    private static final List<String> BANNED_WORDS = List.of(
            "狗屎", "垃圾医院", "黑心医院", "骗子医院", "庸医", "去死", "傻逼", "妈的", "草泥马", "滚");

    /** 观察词：维权与不满表达——放行 + 留痕，用于舆情观察 */
    private static final List<String> WATCH_WORDS = List.of(
            "投诉", "退钱", "曝光", "举报", "骗人的医院");

    private final SensitiveWordMapper sensitiveWordMapper;

    @Override
    public void run(ApplicationArguments args) {
        int banned = seed(BANNED_WORDS, SensitiveWordType.BANNED);
        int watch = seed(WATCH_WORDS, SensitiveWordType.WATCH);
        if (banned + watch > 0) {
            log.info("敏感词库种子装载完成：新增禁止词 {} 条、观察词 {} 条（已存在的不重复插入），管理端可增删停用",
                    banned, watch);
        }
    }

    private int seed(List<String> words, SensitiveWordType type) {
        int inserted = 0;
        for (String word : words) {
            boolean exists = sensitiveWordMapper.selectCount(Wrappers.<SensitiveWord>lambdaQuery()
                    .eq(SensitiveWord::getWord, word)) > 0;
            if (exists) {
                continue;
            }
            SensitiveWord entity = new SensitiveWord();
            entity.setWord(word);
            entity.setType(type);
            entity.setHitCount(0);
            entity.setEnabled(1);
            sensitiveWordMapper.insert(entity);
            inserted++;
        }
        return inserted;
    }
}
