package com.guide.chat.support;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 流式分流闸门单测：保证结论 JSON 不会漏进患者看到的对话气泡。
 */
class StreamGateTest {

    private static final String MARKER = "---RESULT---";

    @Test
    @DisplayName("标记之前的内容原样转发，标记之后全部截留")
    void forwardsBeforeMarker() {
        StreamGate gate = new StreamGate(MARKER);
        StringBuilder visible = new StringBuilder();

        visible.append(gate.accept("活动后胸闷"));
        visible.append(gate.accept("加重，建议心血管内科。\n" + MARKER + "\n{\"dept\":\"心血管内科\"}"));

        assertThat(visible.toString()).isEqualTo("活动后胸闷加重，建议心血管内科。");
        assertThat(gate.flush()).isEmpty();
    }

    @Test
    @DisplayName("跨分片的半个标记暂存，不误转发到气泡")
    void holdsPartialMarker() {
        StreamGate gate = new StreamGate(MARKER);
        StringBuilder visible = new StringBuilder();

        visible.append(gate.accept("结论如下。"));
        visible.append(gate.accept("---RE"));
        visible.append(gate.accept("SULT---"));
        visible.append(gate.accept("{\"dept\":\"骨科\"}"));

        assertThat(visible.toString()).isEqualTo("结论如下。");
    }

    @Test
    @DisplayName("无标记流：flush 交出剩余文本，内容不丢")
    void flushYieldsRemaining() {
        StreamGate gate = new StreamGate(MARKER);
        StringBuilder visible = new StringBuilder();

        visible.append(gate.accept("这个胸闷"));
        visible.append(gate.accept("持续多久了？"));
        visible.append(gate.flush());

        assertThat(visible.toString()).isEqualTo("这个胸闷持续多久了？");
    }

    @Test
    @DisplayName("同时携带正文与标记的单个分片：只转发正文")
    void splitsWithinSingleChunk() {
        StreamGate gate = new StreamGate(MARKER);

        String visible = gate.accept("提示：建议急诊。\n" + MARKER + "\n{\"dept\":\"心血管内科\"}");

        assertThat(visible).isEqualTo("提示：建议急诊。");
    }

    @Test
    @DisplayName("开头空白被吃掉，避免气泡顶部空行")
    void trimsLeadingWhitespace() {
        StreamGate gate = new StreamGate(MARKER);

        assertThat(gate.accept("\n\n")).isEmpty();
        assertThat(gate.accept("  建议就诊。")).isEqualTo("建议就诊。");
    }
}
