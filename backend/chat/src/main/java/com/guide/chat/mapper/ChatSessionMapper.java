package com.guide.chat.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.guide.chat.entity.ChatSession;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface ChatSessionMapper extends BaseMapper<ChatSession> {
}
