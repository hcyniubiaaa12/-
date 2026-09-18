package com.guide.chat.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 会话状态（chat，链路 A）：挂号成功（register_success）即置 CLOSED，
 * 患者此后在同聊天页的新输入 = 开新会话。
 */
@Getter
@RequiredArgsConstructor
public enum SessionStatus {

    ONGOING("ongoing"),
    CLOSED("closed");

    /** 入库编码值（英文小写，见《数据库设计.md》§0） */
    @EnumValue
    private final String code;
}
