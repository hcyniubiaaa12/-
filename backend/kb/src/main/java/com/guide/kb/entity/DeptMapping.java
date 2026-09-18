package com.guide.kb.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.guide.common.entity.BaseEntity;
import com.guide.kb.enums.MappingSource;
import lombok.Getter;
import lombok.Setter;

/**
 * 症状交叉映射台账（链路 B）；运行时不被 RAG 消费（闭环生效靠合成 chunk），
 * 仅管理端维护与审核事实记录；回流 approve 写 source=feedback。
 */
@Getter
@Setter
@TableName("dept_mapping")
public class DeptMapping extends BaseEntity {

    private String symptom;

    private String mainDeptId;

    /** JSON */
    private String crossDeptIds;

    private MappingSource source;
}
