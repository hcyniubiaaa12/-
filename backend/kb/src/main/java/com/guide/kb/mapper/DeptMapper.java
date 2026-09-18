package com.guide.kb.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.guide.kb.entity.Dept;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface DeptMapper extends BaseMapper<Dept> {
}
