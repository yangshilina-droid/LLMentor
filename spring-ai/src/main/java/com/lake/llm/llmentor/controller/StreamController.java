package com.lake.llm.llmentor.controller;

import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;
import reactor.core.publisher.Flux;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.concurrent.Executors;

/**
 * @author LAKE.YANG
 * @filename StreamController
 * @date 2026-04-06 18:02
 */
@RestController
public class StreamController {

    private static final String API_KEY = "sk-430e069e0b8a4e4f828344764a4f1b7a";
    private static final String API_URL = "https://dashscope.aliyuncs.com/compatible-mode/v1/chat/completions";

    @GetMapping("/httpClient")
    public HttpResponse<String> httpClient() {
        String requestBody = """
                {
                    "model": "qwen-plus",
                    "messages": [
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

        HttpResponse<String> response = null;
        try {
            response = client.send(
                    request, HttpResponse.BodyHandlers.ofString());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }


        return response;
    }

    @GetMapping("/sse/emitter")
    public SseEmitter sse() {
        SseEmitter emitter = new SseEmitter(60_000L); // 设置超时时间

        Executors.newSingleThreadExecutor().submit(() -> {
            try {
                for (int i = 0; i < 10; i++) {
                    emitter.send("Message " + i);
                    Thread.sleep(1000);
                }
                emitter.complete();
            } catch (Exception ex) {
                emitter.completeWithError(ex);
            }
        });

        return emitter;
    }

   /* @GetMapping("/sse/streaming")
    public ResponseEntity<StreamingResponseBody> chat() {
        StreamingResponseBody body = outputStream -> {
            for (int i = 0; i < 10; i++) {
                String data = "data chunk " + i + "\n";
                outputStream.write(data.getBytes(StandardCharsets.UTF_8));
                //
                outputStream.flush();
                try {
                    Thread.sleep(1000); // 模拟延迟
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            }
        };

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_TYPE, MediaType.TEXT_EVENT_STREAM_VALUE)
                .body(body);
    }*/

    @GetMapping(value = "/sse/streaming", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public ResponseEntity<StreamingResponseBody> chat() {
        StreamingResponseBody body = outputStream -> {
            try {
                for (int i = 0; i < 10; i++) {
                    String data = "data: data chunk " + i + "\n\n";
                    outputStream.write(data.getBytes(StandardCharsets.UTF_8));
                    outputStream.flush(); // 立即推送到客户端

                    Thread.sleep(1000); // 模拟延迟
                }

                // 可选：告诉前端流结束
                String end = "data: [DONE]\n\n";
                outputStream.write(end.getBytes(StandardCharsets.UTF_8));
                outputStream.flush();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new RuntimeException("SSE stream interrupted", e);
            } catch (IOException e) {
                throw new RuntimeException("SSE stream write failed", e);
            }
        };

        return ResponseEntity.ok()
                .contentType(MediaType.TEXT_EVENT_STREAM)
                .header(HttpHeaders.CACHE_CONTROL, "no-cache")
                .header(HttpHeaders.CONNECTION, "keep-alive")
                .body(body);
    }

    @GetMapping(value = "/sse/flux")
    public Flux<String> fluxStream() {
        return Flux.interval(Duration.ofSeconds(1))
                .map(seq -> "Stream element - " + seq + "\n");
    }


}
