package com.guide.chat.dto;

/**
 * SSE 事件协议载荷（链路 A）：session → delta → (question | result) → done，异常走 error。
 * 形态见《总体架构与链路设计.md》链路 A 对齐点；前端按事件名分发渲染。
 */
public final class SseEvents {

    private SseEvents() {
    }

    /** 事件名常量（与前端 sse 客户端一一对应） */
    public static final String SESSION = "session";
    public static final String DELTA = "delta";
    public static final String QUESTION = "question";
    public static final String RESULT = "result";
    public static final String DONE = "done";
    public static final String ERROR = "error";

    /** 建流即发：本轮流所属会话 id（前端续聊时回传） */
    public record SessionEvent(String sessionId) {
    }

    /** 流式增量文本（逐字渲染同一气泡） */
    public record DeltaEvent(String sessionId, String text) {
    }

    /** 信息不足的追问（不产生 result） */
    public record QuestionEvent(String sessionId, String content, int askRound) {
    }

    /** 收尾（正常结束；error 时也可能收到 done 以便前端收尾） */
    public record DoneEvent(String sessionId, boolean hasResult) {
    }

    /** 兜底错误：文案说清原因与下一步，不道歉不模糊 */
    public record ErrorEvent(String sessionId, String message) {
    }
}
