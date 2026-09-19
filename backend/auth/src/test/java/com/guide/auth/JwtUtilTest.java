package com.guide.auth;

import com.guide.auth.properties.JwtProperties;
import com.guide.auth.util.JwtUtil;
import io.jsonwebtoken.Claims;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

/**
 * JWT 工具单测：签发含角色 claim、解析校验、非法 token 返回 empty。
 */
class JwtUtilTest {

    private JwtUtil jwtUtil;

    @BeforeEach
    void setUp() {
        JwtProperties props = new JwtProperties();
        props.setSecret("unit-test-secret-key-at-least-32-chars!!");
        props.setExpireHours(24);
        jwtUtil = new JwtUtil(props);
    }

    @Test
    void issueAndParseRoundTrip() {
        JwtUtil.TokenInfo info = jwtUtil.issue("u001", "admin", "admin", "tok001");
        assertNotNull(info.token());
        assertEquals("tok001", info.tokenId());

        Optional<Claims> claims = jwtUtil.parse(info.token());
        assertTrue(claims.isPresent());
        assertEquals("u001", claims.get().getSubject());
        assertEquals("admin", claims.get().get("role", String.class));
        assertEquals("admin", claims.get().get("username", String.class));
        assertEquals("tok001", claims.get().getId());
    }

    @Test
    void parseInvalidTokenReturnsEmpty() {
        assertTrue(jwtUtil.parse("not.a.jwt").isEmpty());
        assertTrue(jwtUtil.parse(null).isEmpty());
        assertTrue(jwtUtil.parse("").isEmpty());
    }

    @Test
    void parseTokenSignedWithOtherSecretReturnsEmpty() {
        JwtProperties otherProps = new JwtProperties();
        otherProps.setSecret("another-secret-key-also-32-chars-long!!");
        JwtUtil other = new JwtUtil(otherProps);
        String foreign = other.issue("u001", "admin", "admin", "t").token();
        assertTrue(jwtUtil.parse(foreign).isEmpty());
    }
}
