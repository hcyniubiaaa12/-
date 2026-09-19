package com.guide.auth.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * 管理端用户/敏感词 DTO（链路 D 配套：用户管理页）。
 */
public final class UserAdminDTO {

    private UserAdminDTO() {
    }

    /** 用户行（列表） */
    @Getter
    @Setter
    public static class UserVO {
        private String id;
        private String username;
        private String nickname;
        private String role;
        private String status;
        private LocalDateTime createdAt;
    }

    /** 敏感词行（列表） */
    @Getter
    @Setter
    public static class WordVO {
        private String id;
        private String word;
        private String type;
        private Integer hitCount;
        private Integer enabled;
        private LocalDateTime createdAt;
    }

    /** 敏感词新增 */
    @Getter
    @Setter
    public static class WordAdd {
        @NotBlank(message = "敏感词不能为空")
        private String word;

        /** banned / watch，缺省 banned */
        private String type;
    }

    /** 敏感词批量导入：txt 一行一词，自动去重（总体架构 6.2） */
    @Getter
    @Setter
    public static class WordImport {
        @NotBlank(message = "导入内容不能为空")
        private String text;

        /** 本批导入词的类型，缺省 banned */
        private String type;
    }

    /** 批量导入结果 */
    @Getter
    @Setter
    public static class ImportResult {
        private int imported;
        private int skipped;
    }
}
