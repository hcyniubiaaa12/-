package com.guide.auth.security;

import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;

import java.util.Collection;

/**
 * JWT 认证令牌：principal 为 LoginUser，标记已认证状态。
 */
public class JwtAuthenticationToken extends AbstractAuthenticationToken {

    private final LoginUser principal;

    public JwtAuthenticationToken(LoginUser principal, Collection<? extends GrantedAuthority> authorities) {
        super(authorities);
        this.principal = principal;
        setAuthenticated(true);
    }

    @Override
    public Object getCredentials() {
        return null;
    }

    @Override
    public Object getPrincipal() {
        return principal;
    }
}
