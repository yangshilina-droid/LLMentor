package com.lake.llm.llmentor.controller;

import com.alibaba.cloud.ai.dashscope.chat.DashScopeChatModel;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.prompt.ChatOptions;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;

/**
 * @author LAKE.YANG
 * @filename CallController
 * @date 2026-04-28 22:56
 */
@RestController
@RequestMapping("/call")
public class ChatModelController {

    @Autowired
    private DashScopeChatModel dashScopeChatModel;

    @RequestMapping("/string")
    public String callString(String message) {
        return dashScopeChatModel.call(message);
    }

    @RequestMapping("/messages")
    public String callMessages(String message) {
        SystemMessage systemMessage = new SystemMessage("你是一个翻译工具，请把用户的消息翻译成英文");
        UserMessage userMessage = new UserMessage(message);
        return dashScopeChatModel.call(systemMessage, userMessage);
    }

    @RequestMapping("/prompt")
    public String callPrompt(String message) {
        SystemMessage systemMessage = new SystemMessage("请如实回答我的问题");
        UserMessage userMessage = new UserMessage(message);

        ChatOptions chatOptions = ChatOptions.builder().model("deepseek-v3").build();
        Prompt prompt = new Prompt.Builder().messages(systemMessage, userMessage).chatOptions(chatOptions).build();
        return dashScopeChatModel.call(prompt).getResult().getOutput().getText();
    }

    @RequestMapping("/stream")
    public Flux<String> stream(String message, HttpServletResponse response) {
        response.setCharacterEncoding("UTF-8");
        return dashScopeChatModel.stream(message);
    }



}
