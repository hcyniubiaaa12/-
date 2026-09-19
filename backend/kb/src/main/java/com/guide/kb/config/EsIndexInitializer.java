package com.guide.kb.config;

import com.guide.common.util.EsChunkUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

/**
 * 启动时确保 chunk 索引存在（幂等）。
 * 索引创建不能只挂在种子装载上——种子默认不跑，缺索引时写入会抛异常回滚事务，
 * 而检索侧对缺索引是静默降级，排查时只会看到「BM25 一路总召回 0」。
 * ES 不可用时只记日志：写读两侧各自有降级与报错路径，不阻断应用启动。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class EsIndexInitializer implements ApplicationRunner {

    private final EsChunkUtil esChunkUtil;

    @Override
    public void run(ApplicationArguments args) {
        esChunkUtil.ensureIndex();
    }
}
