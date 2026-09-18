package com.guide.kb.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.guide.common.entity.BaseEntity;
import com.guide.kb.enums.DocFailType;
import com.guide.kb.enums.DocStatus;
import lombok.Getter;
import lombok.Setter;

/**
 * 知识库文档（链路 B）：文档状态机 解析中→完成/失败。
 */
@Getter
@Setter
@TableName("kb_doc")
public class KbDoc extends BaseEntity {

    private String deptId;

    private String title;

    /** MinIO 链接 */
    private String fileUrl;

    private DocStatus status;

    private String failReason;

    private DocFailType failType;

    private Integer chunkTotal;

    private Integer chunkDone;
}
