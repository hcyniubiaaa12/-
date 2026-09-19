package com.guide.admin.controller;

import com.guide.auth.dto.UserAdminDTO;
import com.guide.auth.service.UserAdminService;
import com.guide.common.api.Result;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 管理端用户管理（链路 D 配套，仅 ROLE_ADMIN）：用户分页查询、封禁/解封。
 */
@RestController
@RequestMapping("/api/admin/users")
@RequiredArgsConstructor
public class UserAdminController {

    private final UserAdminService userAdminService;

    @GetMapping
    public Result<?> page(@RequestParam(defaultValue = "1") long current,
                          @RequestParam(defaultValue = "10") long size,
                          @RequestParam(required = false) String keyword) {
        return Result.ok(userAdminService.pageUsers(current, size, keyword));
    }

    @PostMapping("/{id}/ban")
    public Result<Void> ban(@PathVariable String id) {
        userAdminService.ban(id);
        return Result.ok();
    }

    @PostMapping("/{id}/unban")
    public Result<Void> unban(@PathVariable String id) {
        userAdminService.unban(id);
        return Result.ok();
    }
}
