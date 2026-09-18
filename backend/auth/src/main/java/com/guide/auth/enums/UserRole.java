package com.guide.auth.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 用户角色（auth）。
 */
@Getter
@RequiredArgsConstructor
public enum UserRole {

    PATIENT("patient"),
    ADMIN("admin");

    /** 入库编码值（英文小写，见《数据库设计.md》§0） */
    @EnumValue
    private final String code;
}
