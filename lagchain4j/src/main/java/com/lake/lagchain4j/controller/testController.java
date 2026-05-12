package com.lake.lagchain4j.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * @author LAKE.YANG
 * @filename testController
 * @date 2026-05-13 00:10
 */
@RestController
@RequestMapping("/test")
public class testController {

    @GetMapping("/hello")
    public String hello() {
        return "hello";
    }
}
