package com.lake.knowengine.chat.controller;

/**
 * @author LAKE.YANG
 * @filename ChatController
 * @date 2026-06-29 23:55
 */
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 流式对话接口
 */
@RestController
@RequestMapping("/chat")
@Slf4j
public class ChatController {

    @GetMapping("/test")
    public String test() {
        return "Hello World!";
    }


}
