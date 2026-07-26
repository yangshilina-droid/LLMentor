package com.lake.knowenginelearn.chat.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.lake.knowenginelearn.chat.constant.ChatConversationStatus;
import com.lake.knowenginelearn.document.entity.BaseEntity;
import lombok.Data;

/**
 * AI对话会话表
 */
@Data
@TableName("chat_conversation")
public class ChatConversation extends BaseEntity {

    /**
     * 会话唯一标识
     */
    private String conversationId;

    /**
     * 用户ID
     */
    private String userId;

    /**
     * 会话标题
     */
    private String title;

    /**
     * 状态
     */
    private ChatConversationStatus status;
}
