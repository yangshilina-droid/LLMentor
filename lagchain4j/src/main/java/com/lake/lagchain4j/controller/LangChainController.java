package com.lake.lagchain4j.controller;

import dev.langchain4j.model.openai.OpenAiChatModel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * @author LAKE.YANG
 * @filename LangChainController
 * @date 2026-05-13 00:21
 */
@RestController
@RequestMapping("/langchain")
public class LangChainController {

    @Autowired
    OpenAiChatModel chatModel;

    @RequestMapping("/hello")
    public String hello() {
        return chatModel.chat("你好,你是谁？");
    }
}
