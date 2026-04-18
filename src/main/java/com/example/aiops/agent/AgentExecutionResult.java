package com.example.aiops.agent;

import com.example.aiops.dto.ChatResponse;

public class AgentExecutionResult {

    private final String traceId;
    private final ChatResponse response;

    public AgentExecutionResult(String traceId, ChatResponse response) {
        this.traceId = traceId;
        this.response = response;
    }

    public String getTraceId() {
        return traceId;
    }

    public ChatResponse getResponse() {
        return response;
    }
}
