package com.guide.chat.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.guide.chat.enums.SessionStatus;
import com.guide.common.entity.BaseEntity;
import lombok.Getter;
import lombok.Setter;

/**
 * 会话（链路 A）：一条主诉一个会话；挂号成功（register_success）即置 closed；
 * 患者此后在同聊天页的新输入 = 开新会话（判定信号是会话状态，非模型语义判断）；
 * 一个聊天页可先后承载多个会话。
 */
@Getter
@Setter
@TableName("chat_session")
public class ChatSession extends BaseEntity {

    private String userId;

    private SessionStatus status;

    /** 追问轮次 0–3 */
    private Integer askRound;
}
