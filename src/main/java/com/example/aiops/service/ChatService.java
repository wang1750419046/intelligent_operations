package com.example.aiops.service;

import com.example.aiops.agent.AgentExecutionResult;
import com.example.aiops.agent.AgentStreamHandler;

public interface ChatService {

    AgentExecutionResult send(String sessionId, String userInput, Long modelConfigId);

    void stream(String sessionId, String userInput, Long modelConfigId, AgentStreamHandler handler);
}
