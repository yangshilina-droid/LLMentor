package com.lake.llm.llmentor.controller;

import jakarta.servlet.http.HttpServletResponse;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;

/**
 * @author LAKE.YANG
 * @filename OllamaChatController
 * @date 2026-05-11 00:34
 */
@RestController
@RequestMapping("/ai/ollama")
public class OllamaChatController {

    @Autowired
    @Qualifier("ollamaChatModel")
    private ChatModel ollamaChatModel;

    @GetMapping("/stream/chat")
    public Flux<String> streamChat(HttpServletResponse response) {
        response.setCharacterEncoding("UTF-8");
        Flux<ChatResponse> stream = ollamaChatModel.stream(new Prompt("你是谁？"));
        return stream.map(resp -> resp.getResult().getOutput().getText());
    }
}