package com.guide.auth.security;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Optional;

/**
 * 当前登录用户取用工具：chat/feedback 等链路由此取 userId，避免散落取 SecurityContext。
 */
public final class LoginUserHolder {

    private LoginUserHolder() {
    }

    public static Optional<LoginUser> current() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof LoginUser loginUser) {
            return Optional.of(loginUser);
        }
        return Optional.empty();
    }
}
