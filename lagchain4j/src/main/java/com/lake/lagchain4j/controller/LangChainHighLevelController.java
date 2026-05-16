package com.lake.lagchain4j.controller;

import com.lake.lagchain4j.Book;
import com.lake.lagchain4j.chatmemory.RedisChatMemoryStore;
import com.lake.lagchain4j.service.LangChainAiService;
import com.lake.lagchain4j.service.LangChainMemoryAiService;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.service.AiServices;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;


/**
 * @author LAKE.YANG
 * @filename LangChainHighLevelController
 * @date 2026-05-15 22:25
 */
@RestController
@RequestMapping("/langChain/high")
public class LangChainHighLevelController implements InitializingBean {

    @Autowired
    private LangChainAiService aiService;

    /**
     * helloWorld
     */
    @RequestMapping("/chat")
    public String chat(HttpServletResponse response) {
        response.setCharacterEncoding("UTF-8");
        return aiService.chat("日本都有哪些美食？");
    }

    /**
     * 从配置文件中获取提示词
     */
    @RequestMapping("/chatFromFile")
    public String chatFromFile(HttpServletResponse response) {
        response.setCharacterEncoding("UTF-8");
        return aiService.chatFromFile();
    }

    /**
     * 流式输出
     */
    @RequestMapping("/chatStream")
    public Flux<String> chatStream(HttpServletResponse response) {
        response.setCharacterEncoding("UTF-8");
        return aiService.chatStream("日本都有哪些美食？");
    }

    /**
     * 结构化输出
     */
    @RequestMapping("/structure1")
    public String structure1(HttpServletResponse response) {
        response.setCharacterEncoding("UTF-8");
        Book books = aiService.getBooks();
        return books.toString();
    }


    /**
     * 对话记忆
     */
    @Autowired
    OpenAiChatModel chatModel;

    private LangChainMemoryAiService langChainMemoryAiService;

    @RequestMapping("/memoryChat")
    public String memoryChat(HttpServletResponse response, String msg, String memoryId) {
        response.setCharacterEncoding("UTF-8");
        return langChainMemoryAiService.chatMemory(memoryId, msg);
    }

    @Autowired
    private RedisChatMemoryStore redisChatMemoryStore;

    @Override
    public void afterPropertiesSet() throws Exception {
        langChainMemoryAiService = AiServices.builder(LangChainMemoryAiService.class).chatModel(chatModel)
                // .chatMemoryProvider(memoryId -> MessageWindowChatMemory.withMaxMessages(10))
                .chatMemoryProvider(memoryId -> MessageWindowChatMemory.builder()
                        .id(memoryId)
                        .maxMessages(10)
                        .chatMemoryStore(redisChatMemoryStore)
                        .build()).build();
    }

    /**
     * 工具调用
     */
    @RequestMapping("/toolCalling")
    public String toolCalling(HttpServletResponse response, String msg) {
        response.setCharacterEncoding("UTF-8");

        LangChainAiService langChainAiService1 =
                AiServices.builder(LangChainAiService.class).tools(new TemperatureTools()).chatModel(chatModel).build();

        return langChainAiService1.chat(msg);
    }



}
