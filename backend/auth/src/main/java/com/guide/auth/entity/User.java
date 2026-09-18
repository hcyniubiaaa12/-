package com.guide.auth.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.guide.auth.enums.UserRole;
import com.guide.auth.enums.UserStatus;
import com.guide.common.entity.BaseEntity;
import lombok.Getter;
import lombok.Setter;

/**
 * 用户（链路 D）：登录注册；登录时校验 status（封禁即拒绝）；管理员由初始化脚本创建。
 */
@Getter
@Setter
@TableName("user")
public class User extends BaseEntity {

    private String username;

    /** 密码（加密存储） */
    private String password;

    private UserRole role;

    private String nickname;

    private UserStatus status;
}
