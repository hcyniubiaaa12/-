package com.guide.common.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableLogic;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * 公共字段基类：雪花 id / 逻辑删除 / 创建更新时间（见数据库设计.md 通用约定）。
 */
@Getter
@Setter
public abstract class BaseEntity {

    /** 主键，Java 侧雪花算法生成（String） */
    @com.baomidou.mybatisplus.annotation.TableId(type = com.baomidou.mybatisplus.annotation.IdType.ASSIGN_ID)
    private String id;

    /** 逻辑删除，0 正常 / 1 删除 */
    @TableLogic
    private Integer deleted;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
