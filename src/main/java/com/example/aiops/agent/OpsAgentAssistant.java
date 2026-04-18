package com.example.aiops.agent;

import dev.langchain4j.service.MemoryId;
import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;

public interface OpsAgentAssistant {

    @SystemMessage(fromResource = "prompts/ops-agent-system-prompt.txt")
    String analyze(@MemoryId String sessionId, @UserMessage String userMessage);
}
