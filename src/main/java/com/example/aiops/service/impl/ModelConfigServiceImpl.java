package com.example.aiops.service.impl;

import com.example.aiops.dto.ModelConfigRequest;
import com.example.aiops.dto.ModelConfigResponse;
import com.example.aiops.dto.ModelConfigTestResponse;
import com.example.aiops.entity.LlmConfig;
import com.example.aiops.exception.BusinessException;
import com.example.aiops.mapper.LlmConfigMapper;
import com.example.aiops.service.ModelConfigService;
import com.example.aiops.util.TimeUtils;
import dev.langchain4j.model.chat.ChatLanguageModel;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class ModelConfigServiceImpl implements ModelConfigService {

    private final LlmConfigMapper llmConfigMapper;
    private final ChatModelFactory chatModelFactory;

    public ModelConfigServiceImpl(LlmConfigMapper llmConfigMapper, ChatModelFactory chatModelFactory) {
        this.llmConfigMapper = llmConfigMapper;
        this.chatModelFactory = chatModelFactory;
    }

    @Override
    public List<ModelConfigResponse> listAll() {
        return llmConfigMapper.findAll().stream().map(this::toResponse).toList();
    }

    @Override
    public ModelConfigResponse getById(Long id) {
        return toResponse(findEntity(id));
    }

    @Override
    public ModelConfigResponse create(ModelConfigRequest request) {
        LlmConfig config = new LlmConfig();
        apply(request, config);
        config.setCreatedAt(LocalDateTime.now());
        config.setUpdatedAt(LocalDateTime.now());
        if (Boolean.TRUE.equals(config.getDefaultConfig())) {
            llmConfigMapper.clearDefaultFlag();
        }
        llmConfigMapper.insert(config);
        return toResponse(config);
    }

    @Override
    public ModelConfigResponse update(Long id, ModelConfigRequest request) {
        LlmConfig config = findEntity(id);
        apply(request, config);
        config.setUpdatedAt(LocalDateTime.now());
        if (Boolean.TRUE.equals(config.getDefaultConfig())) {
            llmConfigMapper.clearDefaultFlag();
        }
        llmConfigMapper.update(config);
        return toResponse(config);
    }

    @Override
    public void delete(Long id) {
        llmConfigMapper.deleteById(id);
    }

    @Override
    public ModelConfigTestResponse test(Long id) {
        LlmConfig config = findEntity(id);
        if (config.getApiKey() == null || config.getApiKey().isBlank()) {
            return new ModelConfigTestResponse(false, "当前模型配置缺少 API Key");
        }
        try {
            ChatLanguageModel model = chatModelFactory.create(config);
            String reply = model.generate("请只回复 OK");
            return new ModelConfigTestResponse(true, "连接成功，模型返回: " + reply);
        } catch (Exception ex) {
            return new ModelConfigTestResponse(false, "连接失败: " + normalizeLlmError(ex));
        }
    }

    @Override
    public LlmConfig resolveActiveConfig(Long requestedId) {
        if (requestedId != null) {
            LlmConfig config = findEntity(requestedId);
            if (!Boolean.TRUE.equals(config.getEnabled())) {
                throw new BusinessException(40021, "selected model config is disabled");
            }
            return config;
        }
        LlmConfig defaultConfig = llmConfigMapper.findDefault();
        if (defaultConfig == null) {
            throw new BusinessException(40020, "no default model config found");
        }
        return defaultConfig;
    }

    private LlmConfig findEntity(Long id) {
        LlmConfig config = llmConfigMapper.findById(id);
        if (config == null) {
            throw new BusinessException(40402, "model config not found");
        }
        return config;
    }

    private void apply(ModelConfigRequest request, LlmConfig config) {
        config.setName(request.getName());
        config.setProvider(request.getProvider());
        config.setBaseUrl(request.getBaseUrl());
        if (request.getApiKey() != null && !request.getApiKey().isBlank()) {
            config.setApiKey(request.getApiKey().trim());
        }
        config.setModelName(request.getModelName());
        config.setTemperature(request.getTemperature() == null ? BigDecimal.valueOf(0.2D) : request.getTemperature());
        config.setMaxTokens(request.getMaxTokens());
        config.setEnabled(Boolean.TRUE.equals(request.getEnabled()));
        config.setDefaultConfig(Boolean.TRUE.equals(request.getDefaultConfig()));
    }

    private ModelConfigResponse toResponse(LlmConfig config) {
        ModelConfigResponse response = new ModelConfigResponse();
        response.setId(config.getId());
        response.setName(config.getName());
        response.setProvider(config.getProvider());
        response.setBaseUrl(config.getBaseUrl());
        response.setModelName(config.getModelName());
        response.setTemperature(config.getTemperature());
        response.setMaxTokens(config.getMaxTokens());
        response.setEnabled(config.getEnabled());
        response.setDefaultConfig(config.getDefaultConfig());
        response.setHasApiKey(config.getApiKey() != null && !config.getApiKey().isBlank());
        response.setUpdatedAt(TimeUtils.format(config.getUpdatedAt()));
        return response;
    }

    private String normalizeLlmError(Exception ex) {
        String message = ex.getMessage();
        if (message != null && message.contains("AllocationQuota.FreeTierOnly")) {
            return "Qwen 模型免费额度已用完，当前 API Key 开启了仅使用免费额度模式。请到 DashScope/阿里云百炼控制台关闭“仅使用免费额度”或开通付费额度，也可以切换到其他有额度的模型配置。";
        }
        if (message != null && message.contains("insufficient_quota")) {
            return "模型调用额度不足，请检查当前模型供应商账号余额、配额或更换有额度的 API Key。";
        }
        return message;
    }
}
