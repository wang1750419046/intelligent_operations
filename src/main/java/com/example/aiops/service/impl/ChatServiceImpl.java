package com.example.aiops.service.impl;

import com.example.aiops.agent.AgentExecutionResult;
import com.example.aiops.agent.AgentExecutor;
import com.example.aiops.entity.ChatMessage;
import com.example.aiops.service.ChatService;
import com.example.aiops.service.SessionService;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
public class ChatServiceImpl implements ChatService {

    private final SessionService sessionService;
    private final AgentExecutor agentExecutor;

    public ChatServiceImpl(SessionService sessionService, AgentExecutor agentExecutor) {
        this.sessionService = sessionService;
        this.agentExecutor = agentExecutor;
    }

    @Override
    public AgentExecutionResult send(String sessionId, String userInput, Long modelConfigId) {
        sessionService.getSession(sessionId);
        ChatMessage userMessage = new ChatMessage();
        userMessage.setMessageId("msg_" + UUID.randomUUID().toString().replace("-", ""));
        userMessage.setSessionId(sessionId);
        userMessage.setScope(SessionServiceImpl.UI_SCOPE);
        userMessage.setRole("user");
        userMessage.setContent(userInput);
        userMessage.setCreatedAt(LocalDateTime.now());
        sessionService.appendMessage(userMessage);
        return agentExecutor.execute(sessionId, userInput, modelConfigId);
    }
}
