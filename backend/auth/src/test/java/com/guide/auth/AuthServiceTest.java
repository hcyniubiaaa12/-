package com.guide.auth;

import com.guide.auth.dto.AuthDTO;
import com.guide.auth.entity.User;
import com.guide.auth.enums.UserRole;
import com.guide.auth.enums.UserStatus;
import com.guide.auth.mapper.UserMapper;
import com.guide.auth.properties.JwtProperties;
import com.guide.auth.service.AuthService;
import com.guide.auth.util.JwtUtil;
import com.guide.common.exception.BizException;
import com.guide.common.util.RedisUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * 鉴权服务单测：封禁校验、用户名唯一、BCrypt 校验、Redis 登录态写入。
 */
class AuthServiceTest {

    private UserMapper userMapper;
    private RedisUtil redisUtil;
    private AuthService authService;
    private final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    @BeforeEach
    void setUp() {
        userMapper = mock(UserMapper.class);
        redisUtil = mock(RedisUtil.class);
        JwtProperties props = new JwtProperties();
        props.setSecret("unit-test-secret-key-at-least-32-chars!!");
        props.setExpireHours(24);
        authService = new AuthService(userMapper, passwordEncoder, new JwtUtil(props), props, redisUtil);
    }

    private User user(String username, String rawPassword, UserStatus status) {
        User user = new User();
        user.setId("u001");
        user.setUsername(username);
        user.setPassword(passwordEncoder.encode(rawPassword));
        user.setRole(UserRole.ADMIN);
        user.setNickname("管理员");
        user.setStatus(status);
        return user;
    }

    @Test
    void loginSuccessWritesRedisLoginState() {
        when(userMapper.selectOne(any())).thenReturn(user("admin", "123456", UserStatus.NORMAL));

        AuthDTO.Login dto = new AuthDTO.Login();
        dto.setUsername("admin");
        dto.setPassword("123456");
        AuthDTO.LoginVO vo = authService.login(dto);

        assertEquals("u001", vo.getUserId());
        assertEquals("admin", vo.getRole());
        assertNotNull(vo.getToken());
        verify(redisUtil).set(startsWith("auth:token:u001:"), eq("1"), eq(Duration.ofHours(24)));
    }

    @Test
    void loginWrongPasswordThrowsUnauthorized() {
        when(userMapper.selectOne(any())).thenReturn(user("admin", "123456", UserStatus.NORMAL));

        AuthDTO.Login dto = new AuthDTO.Login();
        dto.setUsername("admin");
        dto.setPassword("wrong-pass");
        BizException e = assertThrows(BizException.class, () -> authService.login(dto));
        assertEquals(2000, e.getCode());
    }

    @Test
    void loginBannedUserThrowsBanned() {
        when(userMapper.selectOne(any())).thenReturn(user("admin", "123456", UserStatus.BANNED));

        AuthDTO.Login dto = new AuthDTO.Login();
        dto.setUsername("admin");
        dto.setPassword("123456");
        BizException e = assertThrows(BizException.class, () -> authService.login(dto));
        assertEquals(2003, e.getCode());
    }

    @Test
    void registerDefaultsPatientRoleAndWritesLoginState() {
        when(userMapper.selectCount(any())).thenReturn(0L);
        when(userMapper.insert(any(User.class))).thenReturn(1);

        AuthDTO.Register dto = new AuthDTO.Register();
        dto.setUsername("newpatient");
        dto.setPassword("123456");
        AuthDTO.LoginVO vo = authService.register(dto);

        assertEquals("patient", vo.getRole());
        assertEquals("newpatient", vo.getNickname());
        verify(redisUtil).set(startsWith("auth:token:"), eq("1"), any(Duration.class));
    }

    @Test
    void registerDuplicateUsernameThrowsExists() {
        when(userMapper.selectCount(any())).thenReturn(1L);

        AuthDTO.Register dto = new AuthDTO.Register();
        dto.setUsername("admin");
        dto.setPassword("123456");
        BizException e = assertThrows(BizException.class, () -> authService.register(dto));
        assertEquals(2002, e.getCode());
    }

    @Test
    void logoutDeletesRedisKey() {
        authService.logout("u001", "tok001");
        verify(redisUtil).delete("auth:token:u001:tok001");
    }
}
