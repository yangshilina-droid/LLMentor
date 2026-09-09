package com.lake.knowenginelearn.chat.entity;


import com.lake.knowenginelearn.ai.model.IntentRecognitionResult;
import com.lake.knowenginelearn.chat.constant.ChatSource;

public record ChatParam(String userId, String conversationId, String messageId, String content, String assistantMessageId,
                        IntentRecognitionResult intentRecognitionResult, ChatSource chatSource) {
}

