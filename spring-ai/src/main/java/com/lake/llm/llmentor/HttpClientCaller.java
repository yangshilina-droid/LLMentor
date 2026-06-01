package com.lake.llm.llmentor;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

/**
 * @author LAKE.YANG
 * @filename HttpClientCaller
 * @date 2026-04-06 17:10
 */
public class HttpClientCaller {


    private static final String API_KEY = "sk-430e069e0b8a4e4f828344764a4f1b7a";
    private static final String API_URL = "https://dashscope.aliyuncs.com/compatible-mode/v1/chat/completions";

    public static void main(String[] args) throws IOException, InterruptedException {
        /**
         * Java 15+ 引入的 文本块
         * 用 """ 包裹多行字符串
         * 自动保留换行
         * 不需要 \n、\" 转义
         * 更适合写 JSON / SQL / XML
         */
        // stream 控制流式输出，开启后实时给用户生成文本，不是生成全部文本后展示给用户
        String requestBody = """
                {
                    "model": "qwen-max",
                    "messages": [
                        {
                            "role": "system",
                            "content": "You are a helpful assistant."
                        },
                        {
                            "role": "user",
                            "content": "你好，介绍下JAVA？"
                        }
                    ],
                    "stream": true
                }
                """;

        HttpClient client = HttpClient.newHttpClient();

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(API_URL))
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer " + API_KEY)
                .header("X-DashScope-SSE", "enable")
                .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                .build();

        HttpResponse<String> response = client.send(
                request, HttpResponse.BodyHandlers.ofString());


        System.out.println(response.body());
    }

}
