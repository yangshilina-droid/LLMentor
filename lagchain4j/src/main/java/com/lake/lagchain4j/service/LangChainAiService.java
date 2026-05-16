package com.lake.lagchain4j.service;

import com.lake.lagchain4j.Book;
import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.spring.AiService;
import reactor.core.publisher.Flux;


/**
 * @author LAKE.YANG
 * @filename LangChainAiService
 * @date 2026-05-15 22:26
 */
@AiService
public interface LangChainAiService {

    /**
     * 对话
     */
    String chat(String userMessage);

    /**
     * 从配置文件中获取提示词
     */
    @UserMessage(fromResource = "your-prompt-template.txt")
    String chatFromFile();

    /**
     * 默认参数 + 变量{{}}
     */
    @SystemMessage("你是一个毒舌博主，擅长怼人")
    @UserMessage("针对用户的内容：{{topic}}，先复述一遍他的问题，然后再回答")
    Flux<String> chatStream(String topic);

    @UserMessage("请帮我推荐1本java相关的书")
    @SystemMessage("你是一个专业的图书推荐人员")
    Book getBooks();



}
