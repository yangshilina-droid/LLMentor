package com.lake.knowenginelearn.chat.controller;

import com.lake.knowenginelearn.auth.service.AuthService;
import com.lake.knowenginelearn.chat.constant.ChatSource;
import com.lake.knowenginelearn.chat.entity.ChatConversation;
import com.lake.knowenginelearn.chat.entity.ChatMessage;
import com.lake.knowenginelearn.chat.service.ChatApplicationService;
import com.lake.knowenginelearn.chat.service.ChatConversationService;
import com.lake.knowenginelearn.chat.service.ChatMessageService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;

import java.util.List;

/**
 * 流式对话接口
 */
@RestController
@RequestMapping("/chat")
@Slf4j
public class ChatController {

    @Autowired
    private ChatConversationService chatConversationService;

    @Autowired
    private ChatMessageService chatMessageService;

    @Autowired
    private ChatApplicationService chatApplicationService;

    @Autowired
    private AuthService authService;


    /**
     * 流式对话接口
     * <p>
     * 入参：userId、content（用户问题）、conversationId（可选）
     * 返回：SSE 流，每个 token 逐字推送；流结束前推送一条 [DONE] 事件携带 conversationId
     * <p>
     * 进度通知格式：{@code [PROGRESS]:xxx...}，用于在前端展示当前处理阶段，减少等待焦虑。
     * 推送环节包括：意图识别、问题改写、问题路由、排序筛选、生成回答等。
     *
     * @param userId         用户ID
     * @param content        用户问题
     * @param conversationId 会话ID（可选，不传则自动创建新会话）
     */
    @PostMapping(value = "/send", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<String> send(
            @RequestParam String userId,
            @RequestParam String content,
            @RequestParam(required = false) String conversationId) {
        return chatApplicationService.chat(userId, content, conversationId, ChatSource.USER_WEB);
    }


    /**
     * 查询当前登录用户的对话列表，按更新时间倒序排序
     * <p>
     * userId 从 sa-token session 中获取。
     */
    @GetMapping("/list")
    public List<ChatConversation> listConversations() {
        String userId = authService.getCurrentUserId();
        return chatConversationService.getConversationsByUserId(userId);
    }

    /**
     * 查询指定对话的消息列表，按创建时间正序排序
     *
     * @param conversationId 会话ID
     */
    @GetMapping("/messages")
    public List<ChatMessage> listMessages(@RequestParam String conversationId) {
        return chatMessageService.getMessagesByConversationId(conversationId);
    }

    /**
     * 删除对话（同时删除该对话下所有消息）
     *
     * @param conversationId 会话ID
     */
    @DeleteMapping("/{conversationId}")
    public boolean deleteConversation(@PathVariable String conversationId) {
        chatMessageService.deleteMessagesByConversationId(conversationId);
        return chatConversationService.deleteConversation(conversationId);
    }
}

