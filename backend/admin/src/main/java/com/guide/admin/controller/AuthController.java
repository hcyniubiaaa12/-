package com.guide.admin.controller;

import com.guide.auth.dto.AuthDTO;
import com.guide.auth.security.LoginUser;
import com.guide.auth.security.LoginUserHolder;
import com.guide.auth.service.AuthService;
import com.guide.common.api.ErrorCode;
import com.guide.common.api.Result;
import com.guide.common.exception.BizException;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 鉴权接口（链路 D）：登录 / 注册（默认患者角色）/ 登出。
 */
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/login")
    public Result<AuthDTO.LoginVO> login(@Valid @RequestBody AuthDTO.Login dto) {
        return Result.ok(authService.login(dto));
    }

    @PostMapping("/register")
    public Result<AuthDTO.LoginVO> register(@Valid @RequestBody AuthDTO.Register dto) {
        return Result.ok(authService.register(dto));
    }

    @PostMapping("/logout")
    public Result<Void> logout() {
        LoginUser loginUser = LoginUserHolder.current()
                .orElseThrow(() -> new BizException(ErrorCode.UNAUTHORIZED.getCode(), ErrorCode.UNAUTHORIZED.getMessage()));
        authService.logout(loginUser.userId(), loginUser.tokenId());
        return Result.ok();
    }
}
