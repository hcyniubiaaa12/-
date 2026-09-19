package com.guide.auth;

import com.guide.auth.entity.User;
import com.guide.auth.enums.UserRole;
import com.guide.auth.enums.UserStatus;
import com.guide.auth.mapper.UserMapper;
import com.guide.auth.security.JwtAuthenticationToken;
import com.guide.auth.security.LoginUser;
import com.guide.auth.service.UserAdminService;
import com.guide.common.exception.BizException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.context.SecurityContextHolder;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 用户管理单测：封禁/解封、不能封禁自己、用户不存在。
 */
class UserAdminServiceTest {

    private UserMapper userMapper;
    private UserAdminService service;

    @BeforeEach
    void setUp() {
        userMapper = mock(UserMapper.class);
        service = new UserAdminService(userMapper);
        // 模拟当前登录管理员（userId=u001）
        LoginUser me = new LoginUser("u001", "admin", "admin", "tok001");
        SecurityContextHolder.getContext().setAuthentication(
                new JwtAuthenticationToken(me, me.getAuthorities()));
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    private User user(String id, UserStatus status) {
        User user = new User();
        user.setId(id);
        user.setUsername("user-" + id);
        user.setRole(UserRole.PATIENT);
        user.setStatus(status);
        return user;
    }

    @Test
    void banSetsStatusBanned() {
        when(userMapper.selectById("u002")).thenReturn(user("u002", UserStatus.NORMAL));
        service.ban("u002");
        verify(userMapper).updateById(any(User.class));
    }

    @Test
    void banSelfThrowsCannotBanSelf() {
        BizException e = assertThrows(BizException.class, () -> service.ban("u001"));
        assertEquals(2006, e.getCode());
    }

    @Test
    void unbanSetsStatusNormal() {
        when(userMapper.selectById("u003")).thenReturn(user("u003", UserStatus.BANNED));
        service.unban("u003");
        verify(userMapper).updateById(any(User.class));
    }

    @Test
    void banMissingUserThrowsNotFound() {
        when(userMapper.selectById("ghost")).thenReturn(null);
        BizException e = assertThrows(BizException.class, () -> service.ban("ghost"));
        assertEquals(2004, e.getCode());
    }

    @Test
    void unbanSkipsSelfCheck() {
        // 解封不受"不能封禁自己"约束（解封自己无意义但无害，规则只拦封禁方向）
        when(userMapper.selectById("u001")).thenReturn(user("u001", UserStatus.NORMAL));
        service.unban("u001");
        verify(userMapper).updateById(any(User.class));
    }
}
