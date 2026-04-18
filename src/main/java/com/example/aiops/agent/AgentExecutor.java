package com.example.aiops.agent;

public interface AgentExecutor {

    AgentExecutionResult execute(String sessionId, String userInput, Long modelConfigId);
}
