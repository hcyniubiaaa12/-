package com.guide.auth.properties;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * JWT 配置：密钥与过期时间只走 application.yml（占位符）+ application-local.yml（真实值）。
 */
@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "jwt")
public class JwtProperties {

    /** 签名密钥（HS256，至少 32 字符） */
    private String secret;

    /** 过期时间（小时），默认 24h */
    private long expireHours = 24;

    /** Redis 登录态 key 前缀：auth:token:{userId}:{tokenId} */
    private String redisKeyPrefix = "auth:token:";
}
