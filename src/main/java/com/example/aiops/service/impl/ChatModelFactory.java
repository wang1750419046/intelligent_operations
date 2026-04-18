package com.example.aiops.service.impl;

import com.example.aiops.entity.LlmConfig;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.Duration;

@Component
public class ChatModelFactory {

    private final long timeoutSeconds;

    public ChatModelFactory(@Value("${aiops.llm.timeout-seconds:60}") long timeoutSeconds) {
        this.timeoutSeconds = timeoutSeconds;
    }

    public ChatLanguageModel create(LlmConfig config) {
        return OpenAiChatModel.builder()
                .baseUrl(config.getBaseUrl())
                .apiKey(config.getApiKey())
                .modelName(config.getModelName())
                .temperature(config.getTemperature() == null ? null : config.getTemperature().doubleValue())
                .timeout(Duration.ofSeconds(timeoutSeconds))
                .maxTokens(config.getMaxTokens())
                .logRequests(true)
                .logResponses(true)
                .build();
    }
}
