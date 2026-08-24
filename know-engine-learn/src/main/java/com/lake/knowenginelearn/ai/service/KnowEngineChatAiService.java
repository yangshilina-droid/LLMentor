package com.lake.knowenginelearn.ai.service;

import dev.langchain4j.service.MemoryId;
import dev.langchain4j.service.UserMessage;
import reactor.core.publisher.Flux;

/**
 * @author LAKE.YANG
 * @filename KnowEngineChatAiService
 * @date 2026-08-24 23:20
 */
public interface KnowEngineChatAiService {

    public Flux<String> streamChat(@MemoryId String conversationId, @UserMessage String message);

    public String chat(@MemoryId String conversationId, @UserMessage String message);

}
