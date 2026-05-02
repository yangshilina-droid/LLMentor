package com.lake.llm.llmentor.controller;

import com.alibaba.cloud.ai.dashscope.chat.DashScopeChatModel;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.ai.converter.BeanOutputConverter;
import org.springframework.ai.converter.ListOutputConverter;
import org.springframework.ai.converter.MapOutputConverter;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/ai/structure")
public class StructuredOutputController implements InitializingBean {

    @Autowired
    private DashScopeChatModel chatModel;

    private ChatClient chatClient;

    /**
     * 手搓
     * BeanOutputConverter
     */
    @GetMapping("/chat")
    public String chat(HttpServletResponse response) {
        response.setCharacterEncoding("UTF-8");

        // 1 定义converter对象
        BeanOutputConverter<Book> beanOutputConverter = new BeanOutputConverter<>(Book.class);

        PromptTemplate promptTemplate = new PromptTemplate("""
                请帮我推荐一本java相关的书
                {format}
                """);

        // 2 替换提示词 format
        String result = chatClient.prompt(promptTemplate.create(Map.of("format", beanOutputConverter.getFormat())))
                .system("你是一个专业的图书推荐人员")
                .call()
                .content();

        // 3 结果转成bean对象
        Book book = beanOutputConverter.convert(result);
        System.out.println(book);

        return result;
    }

    /**
     * bean输出
     * entity(Book.class)
     */
    @GetMapping("/bean")
    public String bean(HttpServletResponse response) {
        // 1 替换提示词 2 把输出对象转换为bean对象
        Book book = chatClient.prompt("请帮我推荐一本java相关的书")
                .system("你是一个专业的图书推荐人员")
                .call()
                .entity(Book.class);

        return book.toString();
    }

    /**
     * list+bean输出
     * entity(new ParameterizedTypeReference<List<Book>>()
     */
    @GetMapping("/listBean")
    public String listBean(HttpServletResponse response) {

        List<Book> result = chatClient.prompt("请帮我推荐几本java相关的书")
                .system("你是一个专业的图书推荐人员")
                .call()
                .entity(new ParameterizedTypeReference<List<Book>>() {
                });

        return result.toString();
    }

    /**
     * 常用
     *
     * list输出
     * entity(new ParameterizedTypeReference<List<Book>>()
     */
    @GetMapping("/list")
    public String list(HttpServletResponse response) {

        List<String> result = chatClient.prompt("请帮我推荐几本java相关的书")
                .system("你是一个专业的图书推荐人员")
                .call()
                .entity(new ListOutputConverter() {
                });

        return result.toString();
    }

    /**
     * map输出 BeanOutputConverter
     * entity(new MapOutputConverter())
     */
    @GetMapping("/map")
    public String map(HttpServletResponse response) {

        Map<String, Object> book = chatClient.prompt(
                        "请给我推荐几本心理学有关的书，书的内容包括书名、作者、价格、上市时间等信息，以书名作为key，书的信息作为value")
                .call()
                .entity(new MapOutputConverter());

        return book.toString();
    }


    @Override
    public void afterPropertiesSet() throws Exception {
        chatClient = ChatClient.builder(chatModel).build();
    }
}