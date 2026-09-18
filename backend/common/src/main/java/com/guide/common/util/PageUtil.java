package com.guide.common.util;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.Getter;

import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 分页工具类：IPage → 统一分页返回体，支持 entity → dto 泛型转换。
 */
@Getter
public class PageUtil {

    private final long total;
    private final long pages;
    private final long current;
    private final long size;
    private final List<?> records;

    private PageUtil(long total, long pages, long current, long size, List<?> records) {
        this.total = total;
        this.pages = pages;
        this.current = current;
        this.size = size;
        this.records = records;
    }

    /** 直接包装 IPage（记录不转换） */
    public static <T> PageUtil of(IPage<T> page) {
        return new PageUtil(page.getTotal(), page.getPages(), page.getCurrent(), page.getSize(), page.getRecords());
    }

    /** 包装 IPage 并转换记录类型（entity → dto） */
    public static <E, D> PageUtil of(IPage<E> page, Function<E, D> converter) {
        List<D> records = page.getRecords().stream().map(converter).collect(Collectors.toList());
        return new PageUtil(page.getTotal(), page.getPages(), page.getCurrent(), page.getSize(), records);
    }

    /** 构造 MyBatis-Plus 分页参数 */
    public static <T> Page<T> page(long current, long size) {
        return new Page<>(current, size);
    }
}
