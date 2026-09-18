package com.guide.chat.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 消息角色（chat，链路 A）；QUESTION 即追问消息。
 */
@Getter
@RequiredArgsConstructor
public enum MessageRole {

    USER("user"),
    AI("ai"),
    QUESTION("question");

    /** 入库编码值（英文小写，见《数据库设计.md》§0） */
    @EnumValue
    private final String code;
}
