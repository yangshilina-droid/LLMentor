package com.lake.llm.llmentor.controller;

import jakarta.servlet.http.HttpServletResponse;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.ai.template.st.StTemplateRenderer;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;

import java.util.HashMap;
import java.util.Map;

/**
 * @author LAKE.YANG
 * @filename PromptTemplateController
 * @date 2026-05-01 17:50
 */

@RestController
@RequestMapping("/prompt/template")
public class PromptTemplateController implements InitializingBean {

    @Autowired
    private ChatModel chatModel;

    private ChatClient chatClient;

    /**
     * 提示词管理，单个占位符
     */
    @GetMapping("/promptsEngineer6")
    public Flux<String> chat6(@RequestParam(value = "message") String message, HttpServletResponse response) {
        response.setCharacterEncoding("UTF-8");

        PromptTemplate promptTemplate = new PromptTemplate("请给我推荐几个关于{topic}的开源项目");
        promptTemplate.add("topic", message);

        return chatClient.prompt(promptTemplate.create(Map.of("topic", message)))
                .system("你是一个专业的的github项目收集人员")
                .stream()
                .content();
    }

    /**
     * 提示词管理，多个占位符
     */
    @GetMapping("/promptsEngineer7")
    public Flux<String> chat7(@RequestParam(value = "message") String message, HttpServletResponse response) {
        response.setCharacterEncoding("UTF-8");

        HashMap variables = new HashMap();
        variables.put("language", "Java");
        variables.put("topic", message);
        PromptTemplate promptTemplate = PromptTemplate.builder()
                .template("请给我推荐几个关于{topic}的开源项目,要求是和编程语言{language}相关的。")
                .variables(variables)
                .build();

        return chatClient.prompt(promptTemplate.create(Map.of("topic", message)))
                .system("你是一个专业的的github项目收集人员")
                .stream()
                .content();
    }

    /**
     * 提示词管理，多个占位符
     * 修改占位符样式< > 默认 {}
     */
    @GetMapping("/promptsEngineer8")
    public Flux<String> chat8(@RequestParam(value = "message") String message, HttpServletResponse response) {
        response.setCharacterEncoding("UTF-8");

        PromptTemplate promptTemplate = PromptTemplate.builder()
                .renderer(StTemplateRenderer.builder().startDelimiterToken('<').endDelimiterToken('>').build())
                .template("""
            告诉我 5 部由 <composer> 作曲的电影名称。
            """)
                .build();

        String prompt = promptTemplate.render(Map.of("composer", message));
        return chatClient.prompt(prompt)
                .system("你是一个影视助手")
                .stream()
                .content();
    }



    @Value("classpath:pompts/open-source-system-prompt.st")
    private Resource systemText;

    /**
     * 使用配置文件管理提示词 .st
     */
    @GetMapping("/chat2")
    public Flux<String> chat2(@RequestParam(value = "message") String message, HttpServletResponse response) {
        response.setCharacterEncoding("UTF-8");

        HashMap variables = new HashMap();
        variables.put("language", "Java");
        variables.put("topic", message);
        PromptTemplate promptTemplate = PromptTemplate.builder().resource(systemText).variables(variables).build();

        return chatClient.prompt(promptTemplate.create(Map.of("topic", message))).system("你是一个专业的的github项目收集人员").stream().content();
    }



    @Override
    public void afterPropertiesSet() throws Exception {
        chatClient = ChatClient.builder(chatModel).build();
    }
}
