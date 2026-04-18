package com.example.aiops.service;

import com.example.aiops.agent.AgentExecutionResult;

public interface ChatService {

    AgentExecutionResult send(String sessionId, String userInput, Long modelConfigId);
}
