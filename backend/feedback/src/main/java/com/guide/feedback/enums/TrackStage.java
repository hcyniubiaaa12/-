package com.guide.feedback.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 埋点三触点（feedback，链路 C）：与前端步进流程条一一对应。
 * 旁路落库；只作漏斗/耗时等过程信号，非准确率事实来源。
 */
@Getter
@RequiredArgsConstructor
public enum TrackStage {

    /** 推荐卡渲染完成 */
    RESULT_VIEW("result_view"),

    /** 挂号页选定科室 */
    SIM_REGISTER("sim_register"),

    /** 确认挂号成功 */
    REGISTER_SUCCESS("register_success");

    /** 入库编码值（英文小写，见《数据库设计.md》§0） */
    @EnumValue
    private final String code;

    /** 按编码值匹配（前端传的是小写编码值，禁用 valueOf，见进度.md 已知坑） */
    public static TrackStage fromCode(String code) {
        for (TrackStage stage : values()) {
            if (stage.code.equals(code)) {
                return stage;
            }
        }
        return null;
    }
}
