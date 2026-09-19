package com.guide.auth.util;

import com.guide.auth.properties.JwtProperties;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Date;
import java.util.Optional;

/**
 * JWT 工具（链路 D）：签发含角色 claim，解析校验签名与过期。
 * 登录态本体在 Redis（key 含 tokenId，登出/过期即时失效），JWT 只是携带层。
 */
@Component
@RequiredArgsConstructor
public class JwtUtil {

    private final JwtProperties jwtProperties;

    public record TokenInfo(String token, String tokenId, Duration ttl) {
    }

    private SecretKey key() {
        return Keys.hmacShaKeyFor(jwtProperties.getSecret().getBytes(StandardCharsets.UTF_8));
    }

    /** 签发：subject=userId，claim=role/username，jti=tokenId（对应 Redis 登录态 key） */
    public TokenInfo issue(String userId, String username, String role, String tokenId) {
        Date now = new Date();
        Date expire = new Date(now.getTime() + jwtProperties.getExpireHours() * 3600_000L);
        String token = Jwts.builder()
                .subject(userId)
                .claim("username", username)
                .claim("role", role)
                .id(tokenId)
                .issuedAt(now)
                .expiration(expire)
                .signWith(key())
                .compact();
        return new TokenInfo(token, tokenId, Duration.ofHours(jwtProperties.getExpireHours()));
    }

    /** 解析校验：签名无效/格式非法返回 empty；过期返回 empty（前端统一按 401 跳登录） */
    public Optional<Claims> parse(String token) {
        try {
            return Optional.of(Jwts.parser().verifyWith(key()).build()
                    .parseSignedClaims(token).getPayload());
        } catch (JwtException | IllegalArgumentException e) {
            return Optional.empty();
        }
    }
}
