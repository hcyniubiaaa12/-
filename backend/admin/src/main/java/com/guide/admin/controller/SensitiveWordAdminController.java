package com.guide.admin.controller;

import com.guide.auth.dto.UserAdminDTO;
import com.guide.auth.service.SensitiveWordAdminService;
import com.guide.common.api.Result;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 管理端敏感词库（链路 A 入口校验词库维护，仅 ROLE_ADMIN）：
 * 分页查询、单条添加、批量导入、启停用、观察词转禁止词、删除。
 */
@RestController
@RequestMapping("/api/admin/sensitive-words")
@RequiredArgsConstructor
public class SensitiveWordAdminController {

    private final SensitiveWordAdminService wordService;

    @GetMapping
    public Result<?> page(@RequestParam(defaultValue = "1") long current,
                          @RequestParam(defaultValue = "10") long size,
                          @RequestParam(required = false) String type) {
        return Result.ok(wordService.pageWords(current, size, type));
    }

    @PostMapping
    public Result<Void> add(@Valid @RequestBody UserAdminDTO.WordAdd dto) {
        wordService.add(dto);
        return Result.ok();
    }

    @PostMapping("/import")
    public Result<UserAdminDTO.ImportResult> importWords(@Valid @RequestBody UserAdminDTO.WordImport dto) {
        return Result.ok(wordService.importWords(dto));
    }

    @PostMapping("/{id}/toggle")
    public Result<Void> toggleEnabled(@PathVariable String id) {
        wordService.toggleEnabled(id);
        return Result.ok();
    }

    @PostMapping("/{id}/to-banned")
    public Result<Void> convertToBanned(@PathVariable String id) {
        wordService.convertToBanned(id);
        return Result.ok();
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable String id) {
        wordService.delete(id);
        return Result.ok();
    }
}
