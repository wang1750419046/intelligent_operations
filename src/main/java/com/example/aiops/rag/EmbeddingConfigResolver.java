package com.example.aiops.rag;

import com.example.aiops.entity.LlmConfig;
import com.example.aiops.exception.BusinessException;
import com.example.aiops.mapper.LlmConfigMapper;
import org.springframework.stereotype.Component;

@Component
public class EmbeddingConfigResolver {

    private final LlmConfigMapper llmConfigMapper;

    public EmbeddingConfigResolver(LlmConfigMapper llmConfigMapper) {
        this.llmConfigMapper = llmConfigMapper;
    }

    public LlmConfig resolveRequired() {
        LlmConfig config = llmConfigMapper.findActiveEmbedding();
        if (config == null) {
            throw new BusinessException(40030, "no enabled embedding config found");
        }
        if (config.getApiKey() == null || config.getApiKey().isBlank()) {
            throw new BusinessException(40031, "embedding config missing API Key");
        }
        return config;
    }

    public LlmConfig resolveNullable() {
        return llmConfigMapper.findActiveEmbedding();
    }
}
