package com.guide.feedback.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * 埋点上报入参（链路 C）：stage 为编码值（result_view / sim_register / register_success）。
 */
public class TrackDTO {

    public record TrackReq(
            @NotBlank(message = "缺少埋点阶段") String stage,
            @NotBlank(message = "缺少导诊记录") String recordId,
            String deptId) {
    }
}
