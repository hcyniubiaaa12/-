package com.guide.async.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.guide.async.entity.IngestTask;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface IngestTaskMapper extends BaseMapper<IngestTask> {
}
