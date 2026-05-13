package com.example.aiops.rag;

import com.example.aiops.entity.LlmConfig;
import com.example.aiops.exception.BusinessException;
import com.example.aiops.mapper.LlmConfigMapper;
import org.springframework.stereotype.Component;

@Component
public class RerankConfigResolver {

    private final LlmConfigMapper llmConfigMapper;

    public RerankConfigResolver(LlmConfigMapper llmConfigMapper) {
        this.llmConfigMapper = llmConfigMapper;
    }

    public LlmConfig resolveRequired() {
        LlmConfig config = llmConfigMapper.findActiveRerank();
        if (config == null) {
            throw new BusinessException(40032, "no enabled rerank config found");
        }
        if (config.getApiKey() == null || config.getApiKey().isBlank()) {
            throw new BusinessException(40033, "rerank config missing API Key");
        }
        return config;
    }

    public LlmConfig resolveNullable() {
        return llmConfigMapper.findActiveRerank();
    }
}
