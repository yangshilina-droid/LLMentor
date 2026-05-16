package com.lake.lagchain4j.service;

import dev.langchain4j.service.MemoryId;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.spring.AiService;

/**
 * @author LAKE.YANG
 * @filename LangChainMemoryAiService
 * @date 2026-05-15 23:13
 */
@AiService
public interface LangChainMemoryAiService {

    String chatMemory(@MemoryId String memoryId, @UserMessage String userMessage);

}
