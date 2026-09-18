package com.guide.chat.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.guide.chat.enums.MessageRole;
import com.guide.common.entity.BaseEntity;
import lombok.Getter;
import lombok.Setter;

/**
 * 会话消息（链路 A）；question 即追问消息。
 */
@Getter
@Setter
@TableName("chat_message")
public class ChatMessage extends BaseEntity {

    private String sessionId;

    private MessageRole role;

    private String content;
}
