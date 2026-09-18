package com.guide.kb.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.guide.common.entity.BaseEntity;
import lombok.Getter;
import lombok.Setter;

/**
 * 科室蓝本（链路 B），模拟挂号科室范围。
 * 停用仅入口生效：挂号页不显示 + 推荐校验拦截，chunk 留库不删、历史记录引用不受影响。
 */
@Getter
@Setter
@TableName("dept")
public class Dept extends BaseEntity {

    private String name;

    /** 1 启用 / 0 停用 */
    private Integer enabled;

    private String location;

    private String intro;
}
