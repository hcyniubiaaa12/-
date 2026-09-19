package com.guide.auth.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.guide.auth.dto.UserAdminDTO;
import com.guide.auth.entity.User;
import com.guide.auth.enums.UserStatus;
import com.guide.auth.mapper.UserMapper;
import com.guide.auth.security.LoginUserHolder;
import com.guide.common.api.ErrorCode;
import com.guide.common.exception.BizException;
import com.guide.common.util.PageUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * 管理端用户管理（链路 D 配套）：用户分页查询、封禁/解封。
 * 封禁按总体架构 6.4 仅改账号状态——登录时校验拦截；在线 token 留 Redis 至登出/过期，与设计一致。
 */
@Service
@RequiredArgsConstructor
public class UserAdminService {

    private final UserMapper userMapper;

    public PageUtil pageUsers(long current, long size, String keyword) {
        Page<User> page = userMapper.selectPage(PageUtil.page(current, size),
                Wrappers.<User>lambdaQuery()
                        .and(keyword != null && !keyword.isBlank(), w -> w
                                .like(User::getUsername, keyword)
                                .or()
                                .like(User::getNickname, keyword))
                        .orderByDesc(User::getCreatedAt));
        return PageUtil.of(page, this::toVO);
    }

    /** 封禁：置 status=banned；不能封禁自己（避免管理员把唯一账号锁死） */
    public void ban(String userId) {
        changeStatus(userId, UserStatus.BANNED);
    }

    public void unban(String userId) {
        changeStatus(userId, UserStatus.NORMAL);
    }

    private void changeStatus(String userId, UserStatus target) {
        if (target == UserStatus.BANNED) {
            LoginUserHolder.current().ifPresent(me -> {
                if (me.userId().equals(userId)) {
                    throw new BizException(ErrorCode.CANNOT_BAN_SELF);
                }
            });
        }
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BizException(ErrorCode.USER_NOT_FOUND);
        }
        user.setStatus(target);
        userMapper.updateById(user);
    }

    private UserAdminDTO.UserVO toVO(User user) {
        UserAdminDTO.UserVO vo = new UserAdminDTO.UserVO();
        vo.setId(user.getId());
        vo.setUsername(user.getUsername());
        vo.setNickname(user.getNickname());
        vo.setRole(user.getRole().getCode());
        vo.setStatus(user.getStatus().getCode());
        vo.setCreatedAt(user.getCreatedAt());
        return vo;
    }
}
