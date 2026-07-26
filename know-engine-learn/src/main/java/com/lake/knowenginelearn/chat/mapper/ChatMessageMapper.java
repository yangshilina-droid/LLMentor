package com.lake.knowenginelearn.chat.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.lake.knowenginelearn.chat.entity.ChatMessage;
import org.apache.ibatis.annotations.Mapper;

/**
 * AI对话消息表 Mapper
 */
@Mapper
public interface ChatMessageMapper extends BaseMapper<ChatMessage> {
}
