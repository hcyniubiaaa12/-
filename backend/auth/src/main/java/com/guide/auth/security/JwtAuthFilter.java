package com.guide.auth.security;

import com.guide.auth.properties.JwtProperties;
import com.guide.auth.util.JwtUtil;
import com.guide.common.util.RedisUtil;
import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * JWT 过滤器（链路 D）：解析 Bearer → 校验 Redis 登录态（登出即时失效）→ 注入 SecurityContext。
 * 无 token / 校验失败不写入上下文，由授权规则统一放行公开接口或拒绝。
 */
@Component
@RequiredArgsConstructor
public class JwtAuthFilter extends OncePerRequestFilter {

    private final JwtUtil jwtUtil;
    private final RedisUtil redisUtil;
    private final JwtProperties jwtProperties;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String header = request.getHeader("Authorization");
        if (header != null && header.startsWith("Bearer ")
                && SecurityContextHolder.getContext().getAuthentication() == null) {
            jwtUtil.parse(header.substring(7)).ifPresent(claims -> {
                if (redisTokenValid(claims)) {
                    LoginUser loginUser = new LoginUser(
                            claims.getSubject(),
                            claims.get("username", String.class),
                            claims.get("role", String.class),
                            claims.getId());
                    JwtAuthenticationToken authentication =
                            new JwtAuthenticationToken(loginUser, loginUser.getAuthorities());
                    authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                    SecurityContextHolder.getContext().setAuthentication(authentication);
                }
            });
        }
        filterChain.doFilter(request, response);
    }

    /** Redis 登录态校验：key 不存在 = 已登出/被清除，即使 JWT 本身未过期也拒绝 */
    private boolean redisTokenValid(Claims claims) {
        return redisUtil.hasKey(jwtProperties.getRedisKeyPrefix() + claims.getSubject() + ":" + claims.getId());
    }
}
