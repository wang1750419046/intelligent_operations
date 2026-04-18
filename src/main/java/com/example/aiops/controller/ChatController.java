package com.example.aiops.controller;

import com.example.aiops.agent.AgentExecutionResult;
import com.example.aiops.dto.ChatRequest;
import com.example.aiops.dto.ChatResponse;
import com.example.aiops.dto.UnifiedResponse;
import com.example.aiops.service.ChatService;
import com.example.aiops.util.TraceIdHolder;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/chat")
public class ChatController {

    private final ChatService chatService;

    public ChatController(ChatService chatService) {
        this.chatService = chatService;
    }

    @PostMapping("/send")
    public UnifiedResponse<ChatResponse> send(@Valid @RequestBody ChatRequest request) {
        AgentExecutionResult result = chatService.send(request.getSessionId(), request.getUserInput(), request.getModelConfigId());
        return UnifiedResponse.success(result.getResponse(), TraceIdHolder.getTraceId());
    }
}
