package com.lake.mcpclient.controller;


import com.lake.mcpclient.service.McpClientService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/mcp")
@Slf4j
@RequiredArgsConstructor
public class McpClientController {

    @Autowired
    private McpClientService mcpClientService;

    // @Autowired
    // private ManualMcpClientService manualMcpClientService;

    @GetMapping("/callTool")
    public Object callTool(@RequestParam("type") String type) {
        return mcpClientService.callTool(type);
    }


    @GetMapping("/chat")
    public String chat(@RequestParam("query") String query) {
        log.info("chat request => {}", query);

        return mcpClientService.chat(query);
    }

    // @GetMapping("/manualChat")
    // public String manualChat(@RequestParam("query") String query) {
    //     log.info("manualChat request => {}", query);
    //
    //     return manualMcpClientService.chat(query);
    // }

}
