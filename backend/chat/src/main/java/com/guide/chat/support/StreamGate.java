package com.guide.chat.support;

/**
 * 流式分流闸门：模型输出「自然语言回复 + 结论标记 + JSON」，其中只有自然语言该进对话气泡。
 * 逐段喂入增量文本，返回可安全转发的片段——标记之前原样转发，标记之后全部截留；
 * 跨分片的半个标记（如 "---RE"）会被暂存等待后续片段，避免误转发到气泡。
 */
public class StreamGate {

    private final String marker;
    private final StringBuilder buffered = new StringBuilder();
    private boolean closed;
    private boolean emitted;

    public StreamGate(String marker) {
        this.marker = marker;
    }

    /**
     * @param delta 模型增量文本
     * @return 可转发给前端的片段（可能为空串）
     */
    public String accept(String delta) {
        if (closed || delta == null || delta.isEmpty()) {
            return "";
        }
        buffered.append(delta);
        int markerIndex = buffered.indexOf(marker);
        if (markerIndex >= 0) {
            // 标记即可见文本结尾：去掉其前面的换行/空白，避免气泡尾部空行
            String forward = buffered.substring(0, markerIndex).replaceAll("\\s+$", "");
            buffered.setLength(0);
            closed = true;
            return finish(forward);
        }
        int hold = holdBackLength();
        int emitLength = buffered.length() - hold;
        if (emitLength <= 0) {
            return "";
        }
        String forward = buffered.substring(0, emitLength);
        buffered.delete(0, emitLength);
        return finish(forward);
    }

    /** 流结束后的收尾：返回仍被暂存且可安全转发的文本（无标记时即剩余全文） */
    public String flush() {
        if (closed) {
            return "";
        }
        closed = true;
        String forward = buffered.toString();
        buffered.setLength(0);
        return finish(forward.replaceAll("\\s+$", ""));
    }

    /** 去掉开头空白，避免气泡顶部出现空行 */
    private String finish(String text) {
        if (!emitted) {
            String trimmed = text.replaceAll("^\\s+", "");
            if (!trimmed.isEmpty()) {
                emitted = true;
            }
            return trimmed;
        }
        return text;
    }

    /** 暂存长度：当前缓冲尾部是标记前缀的最长长度（跨分片的半个标记） */
    private int holdBackLength() {
        int max = Math.min(marker.length() - 1, buffered.length());
        for (int length = max; length > 0; length--) {
            if (endsWith(buffered, marker.substring(0, length))) {
                return length;
            }
        }
        return 0;
    }

    private boolean endsWith(StringBuilder source, String suffix) {
        int offset = source.length() - suffix.length();
        if (offset < 0) {
            return false;
        }
        for (int i = 0; i < suffix.length(); i++) {
            if (source.charAt(offset + i) != suffix.charAt(i)) {
                return false;
            }
        }
        return true;
    }
}
