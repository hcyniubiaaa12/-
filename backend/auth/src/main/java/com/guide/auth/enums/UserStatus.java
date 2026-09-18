package com.guide.auth.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 用户状态（auth）：登录时校验，封禁即拒绝。
 */
@Getter
@RequiredArgsConstructor
public enum UserStatus {

    NORMAL("normal"),
    BANNED("banned");

    /** 入库编码值（英文小写，见《数据库设计.md》§0） */
    @EnumValue
    private final String code;
}
