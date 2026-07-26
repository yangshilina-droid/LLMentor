package com.lake.knowenginelearn.chat.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.lake.knowenginelearn.chat.entity.ChatConversation;
import org.apache.ibatis.annotations.Mapper;

/**
 * AI对话会话表 Mapper
 */
@Mapper
public interface ChatConversationMapper extends BaseMapper<ChatConversation> {
}

