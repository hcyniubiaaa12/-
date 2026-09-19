package com.guide.rag.support;

import com.guide.common.model.ChunkHit;
import com.guide.llm.client.ChatMsg;
import com.guide.rag.dto.DeptOption;
import com.guide.rag.dto.RagContext;
import com.guide.rag.dto.RagRequest;
import com.guide.rag.dto.RagTurn;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * Prompt 拼装（链路 A 第 ④ 步末）：知识片段带注号与精排分数入上下文
 * （分数作为模型自报置信度的客观锚点），结论输出协议见 {@link AnswerParser}。
 * 判定与生成合并为一次 LLM 调用：信息不足走追问分支，足够走结论分支，零额外成本。
 */
@Component
public class PromptBuilder {

    public List<ChatMsg> build(RagRequest request, RagContext context) {
        List<ChatMsg> messages = new ArrayList<>();
        messages.add(ChatMsg.system(systemPrompt(request, context)));
        for (RagTurn turn : request.history()) {
            messages.add(new ChatMsg(turn.role(), turn.content()));
        }
        messages.add(ChatMsg.user(request.query()));
        return messages;
    }

    private String systemPrompt(RagRequest request, RagContext context) {
        StringBuilder sb = new StringBuilder();
        sb.append("""
                你是医院门诊的智能导诊助手。你的职责是分诊——根据患者的症状描述推荐应该挂号的科室。\
                你不做诊断、不判断疾病名称、不给用药建议。

                """);

        sb.append("【知识库片段】\n");
        if (context.isEmpty()) {
            sb.append("（本次未检索到相关知识片段，请依据候选科室范围谨慎判断，并把置信度调低）\n");
        } else {
            List<ChunkHit> chunks = context.chunks();
            for (int i = 0; i < chunks.size(); i++) {
                ChunkHit chunk = chunks.get(i);
                sb.append("注").append(i + 1)
                        .append("（相关度 ").append(String.format("%.2f", chunk.score())).append("）")
                        .append(chunk.title() == null ? "" : "《" + chunk.title() + "》")
                        .append("：").append(chunk.content()).append('\n');
            }
        }

        sb.append("\n【候选科室】（只能从这里选，不得编造）\n");
        if (request.deptOptions().isEmpty()) {
            sb.append("（无可用科室）\n");
        } else {
            for (DeptOption dept : request.deptOptions()) {
                sb.append("- ").append(dept.name()).append('\n');
            }
        }

        sb.append("""

                【输出要求】
                1. 信息不足（缺少部位、症状性质、持续时间、诱因、伴随症状等关键信息）时：
                   只输出一句针对性的追问——结合上方知识片段，问该症状鉴别最需要补充的信息；
                   不要输出分隔符，也不要输出 JSON。
                2. 信息足够时：先用 1—3 句自然语言向患者说明判断依据（引用片段用「注1」「注2」标注），
                   然后另起一行输出 ---RESULT---，紧跟一个 JSON 对象，例如：
                   ---RESULT---
                   {"dept":"心血管内科","confidence":0.82,"top3":[{"dept":"心血管内科","confidence":0.82},{"dept":"呼吸内科","confidence":0.11}],"note":"活动后胸闷加重、休息可缓解，优先排查心脏来源","cites":[1,2]}
                   JSON 约束：dept 必须是候选科室之一；confidence 是 0—1 的小数，top3 最多 3 个且按置信度递减；
                   cites 是引用的片段注号数组（可为空数组）；note 为一句结论说明。
                3. 置信度要如实反映把握程度：证据只沾边、症状指向多个科室时给低值（如 0.4 以下）。
                4. 若症状提示急危重症（剧烈胸痛、呼吸困难、意识障碍、大出血等），在回复中明确建议立即前往急诊。
                """);

        if (request.forceConclusion()) {
            sb.append("\n【本轮约束】追问次数已达上限，本轮必须给出结论：即使信息仍不完整也要输出 ")
                    .append(AnswerParser.MARKER).append(" 与 JSON，并把置信度调低。\n");
        }
        return sb.toString();
    }
}
