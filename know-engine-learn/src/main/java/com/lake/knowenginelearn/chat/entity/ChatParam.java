package com.lake.knowenginelearn.chat.entity;


import com.lake.knowenginelearn.ai.model.IntentRecognitionResult;

public record ChatParam(String userId, String conversationId, String messageId,String content,String assistantMessageId,
                        IntentRecognitionResult intentRecognitionResult) {
}

