package com.lake.knowenginelearn.ai.service;

import com.lake.knowenginelearn.ai.model.IntentRecognitionResult;
import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;

/**
 * 意图识别服务
 *
 * @author Hollis
 */
public interface IntentRecognitionService {

    @SystemMessage(fromResource = "prompts/intent-recognition-new-prompt.txt")
    IntentRecognitionResult chat(@UserMessage String userMessage);
}
