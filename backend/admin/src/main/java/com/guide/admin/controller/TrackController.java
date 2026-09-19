package com.guide.admin.controller;

import com.guide.auth.security.LoginUser;
import com.guide.auth.security.LoginUserHolder;
import com.guide.common.api.ErrorCode;
import com.guide.common.api.Result;
import com.guide.common.exception.BizException;
import com.guide.feedback.dto.TrackDTO;
import com.guide.feedback.enums.TrackStage;
import com.guide.feedback.service.TrackService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 埋点上报接口（链路 C）：患者端在推荐卡渲染完成（result_view）、挂号页选定科室（sim_register）
 * 两个触点调用；register_success 由挂号确认接口在后端补记，不由前端上报。
 * 埋点是旁路信号，非准确率事实来源。
 */
@RestController
@RequestMapping("/api/track")
@RequiredArgsConstructor
public class TrackController {

    private final TrackService trackService;

    @PostMapping
    public Result<Void> track(@Valid @RequestBody TrackDTO.TrackReq request) {
        TrackStage stage = TrackStage.fromCode(request.stage());
        if (stage == null) {
            throw new BizException(ErrorCode.PARAM_INVALID, "未知埋点阶段：" + request.stage());
        }
        String userId = LoginUserHolder.current()
                .map(LoginUser::userId)
                .orElseThrow(() -> new BizException(ErrorCode.UNAUTHORIZED));
        trackService.track(stage, request.recordId(), userId, request.deptId());
        return Result.ok();
    }
}
