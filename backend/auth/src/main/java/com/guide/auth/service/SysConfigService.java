package com.guide.auth.service;

import com.guide.auth.entity.SysConfig;
import com.guide.auth.mapper.SysConfigMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 运行时可调配置（sys_config 字典表，管理端可改）。
 * 链路参数统一从这里读，避免魔法值散落：检索 Top-K/Top-N、追问上限、低置信度阈值等。
 * 读多写少 → 进程内缓存，管理端改参数后调用 {@link #refresh()} 失效重建。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SysConfigService {

    public static final String KEY_RETRIEVE_TOP_K = "retrieve.top.k";
    public static final String KEY_RETRIEVE_TOP_N = "retrieve.top.n";
    public static final String KEY_ASK_MAX_ROUNDS = "chat.ask.max.rounds";
    public static final String KEY_LOW_CONFIDENCE = "guide.low.confidence";
    public static final String KEY_TERM_MANUAL_REVIEW = "term.manual.review";

    private final SysConfigMapper sysConfigMapper;
    private final Map<String, String> cache = new ConcurrentHashMap<>();

    public String get(String key, String defaultValue) {
        String value = cache.computeIfAbsent(key, k -> {
            SysConfig config = sysConfigMapper.selectOne(
                    com.baomidou.mybatisplus.core.toolkit.Wrappers.<SysConfig>lambdaQuery()
                            .eq(SysConfig::getConfigKey, k).last("LIMIT 1"));
            return config == null ? "" : config.getConfigValue();
        });
        return value == null || value.isBlank() ? defaultValue : value;
    }

    public int getInt(String key, int defaultValue) {
        try {
            return Integer.parseInt(get(key, String.valueOf(defaultValue)).trim());
        } catch (NumberFormatException e) {
            log.warn("配置 {} 不是合法整数，使用默认值 {}", key, defaultValue);
            return defaultValue;
        }
    }

    public double getDouble(String key, double defaultValue) {
        try {
            return Double.parseDouble(get(key, String.valueOf(defaultValue)).trim());
        } catch (NumberFormatException e) {
            log.warn("配置 {} 不是合法小数，使用默认值 {}", key, defaultValue);
            return defaultValue;
        }
    }

    public boolean getBoolean(String key, boolean defaultValue) {
        return Boolean.parseBoolean(get(key, String.valueOf(defaultValue)).trim());
    }

    /** 管理端改参数后失效重建 */
    public void refresh() {
        cache.clear();
    }
}
