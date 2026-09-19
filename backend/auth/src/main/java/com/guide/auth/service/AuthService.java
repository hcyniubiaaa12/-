package com.guide.auth.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.guide.auth.dto.AuthDTO;
import com.guide.auth.entity.User;
import com.guide.auth.enums.UserRole;
import com.guide.auth.enums.UserStatus;
import com.guide.auth.mapper.UserMapper;
import com.guide.auth.properties.JwtProperties;
import com.guide.auth.util.JwtUtil;
import com.guide.common.api.ErrorCode;
import com.guide.common.exception.BizException;
import com.guide.common.util.RedisUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.UUID;

/**
 * 鉴权服务（链路 D）：登录（封禁校验 + BCrypt + 签发 + Redis 登录态）、注册（默认患者角色）、登出。
 * 允许同一账号多端登录（多 token 并存，登出按 tokenId 删除各自登录态）。
 */
@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final JwtProperties jwtProperties;
    private final RedisUtil redisUtil;

    public AuthDTO.LoginVO login(AuthDTO.Login dto) {
        User user = userMapper.selectOne(Wrappers.<User>lambdaQuery()
                .eq(User::getUsername, dto.getUsername()));
        if (user == null || !passwordEncoder.matches(dto.getPassword(), user.getPassword())) {
            throw new BizException(ErrorCode.UNAUTHORIZED.getCode(), "用户名或密码错误");
        }
        if (user.getStatus() == UserStatus.BANNED) {
            throw new BizException(ErrorCode.USER_BANNED);
        }

        String tokenId = UUID.randomUUID().toString().replace("-", "");
        JwtUtil.TokenInfo tokenInfo = jwtUtil.issue(user.getId(), user.getUsername(),
                user.getRole().getCode(), tokenId);
        redisUtil.set(redisKey(user.getId(), tokenId), "1", tokenInfo.ttl());

        return toLoginVO(user, tokenInfo.token());
    }

    public AuthDTO.LoginVO register(AuthDTO.Register dto) {
        // 用户名唯一：唯一键兜底并发注册，预查给出友好提示
        if (userMapper.selectCount(Wrappers.<User>lambdaQuery()
                .eq(User::getUsername, dto.getUsername())) > 0) {
            throw new BizException(ErrorCode.USERNAME_EXISTS);
        }
        User user = new User();
        user.setUsername(dto.getUsername());
        user.setPassword(passwordEncoder.encode(dto.getPassword()));
        user.setRole(UserRole.PATIENT);
        user.setNickname(dto.getNickname() != null ? dto.getNickname() : dto.getUsername());
        user.setStatus(UserStatus.NORMAL);
        try {
            userMapper.insert(user);
        } catch (DataIntegrityViolationException e) {
            throw new BizException(ErrorCode.USERNAME_EXISTS);
        }
        // 注册成功即登录：直接签发 token
        String tokenId = UUID.randomUUID().toString().replace("-", "");
        JwtUtil.TokenInfo tokenInfo = jwtUtil.issue(user.getId(), user.getUsername(),
                user.getRole().getCode(), tokenId);
        redisUtil.set(redisKey(user.getId(), tokenId), "1", tokenInfo.ttl());
        return toLoginVO(user, tokenInfo.token());
    }

    /** 登出：删 Redis 登录态，登出即时失效 */
    public void logout(String userId, String tokenId) {
        redisUtil.delete(redisKey(userId, tokenId));
    }

    private String redisKey(String userId, String tokenId) {
        return jwtProperties.getRedisKeyPrefix() + userId + ":" + tokenId;
    }

    private AuthDTO.LoginVO toLoginVO(User user, String token) {
        AuthDTO.LoginVO vo = new AuthDTO.LoginVO();
        vo.setToken(token);
        vo.setUserId(user.getId());
        vo.setUsername(user.getUsername());
        vo.setNickname(user.getNickname());
        vo.setRole(user.getRole().getCode());
        return vo;
    }
}
