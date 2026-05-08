package com.example.aiops.service.impl;

import com.example.aiops.dto.ModelConfigRequest;
import com.example.aiops.dto.ModelConfigResponse;
import com.example.aiops.dto.ModelConfigTestResponse;
import com.example.aiops.entity.LlmConfig;
import com.example.aiops.exception.BusinessException;
import com.example.aiops.mapper.LlmConfigMapper;
import com.example.aiops.rag.QwenEmbeddingService;
import com.example.aiops.service.ModelConfigService;
import com.example.aiops.util.TimeUtils;
import dev.ai4j.openai4j.OpenAiHttpException;
import dev.langchain4j.model.chat.ChatLanguageModel;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;

@Service
public class ModelConfigServiceImpl implements ModelConfigService {

    private final LlmConfigMapper llmConfigMapper;
    private final ChatModelFactory chatModelFactory;
    private final QwenEmbeddingService qwenEmbeddingService;

    public ModelConfigServiceImpl(LlmConfigMapper llmConfigMapper,
                                  ChatModelFactory chatModelFactory,
                                  QwenEmbeddingService qwenEmbeddingService) {
        this.llmConfigMapper = llmConfigMapper;
        this.chatModelFactory = chatModelFactory;
        this.qwenEmbeddingService = qwenEmbeddingService;
    }

    @Override
    public List<ModelConfigResponse> listAll(String configType) {
        if (configType != null && !configType.isBlank()) {
            return llmConfigMapper.findByConfigType(normalizeConfigType(configType)).stream().map(this::toResponse).toList();
        }
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
            llmConfigMapper.clearDefaultFlag(config.getConfigType());
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
            llmConfigMapper.clearDefaultFlag(config.getConfigType());
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
            if ("EMBEDDING".equalsIgnoreCase(config.getConfigType())) {
                int dimensions = qwenEmbeddingService.embed("AIOps embedding connection test", config).size();
                return new ModelConfigTestResponse(true, "连接成功，向量维度: " + dimensions);
            }
            ChatLanguageModel model = chatModelFactory.create(config);
            String reply = model.generate("请只回复 OK");
            return new ModelConfigTestResponse(true, "连接成功，模型返回: " + reply);
        } catch (Exception ex) {
            return new ModelConfigTestResponse(false, "连接失败: " + normalizeLlmError(ex, config));
        }
    }

    @Override
    public LlmConfig resolveActiveConfig(Long requestedId) {
        if (requestedId != null) {
            LlmConfig config = findEntity(requestedId);
            if (!Boolean.TRUE.equals(config.getEnabled())) {
                throw new BusinessException(40021, "selected model config is disabled");
            }
            if (!"CHAT".equalsIgnoreCase(config.getConfigType())) {
                throw new BusinessException(40022, "selected model config is not a chat model");
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
        config.setConfigType(normalizeConfigType(request.getConfigType()));
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
        response.setConfigType(config.getConfigType());
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

    private String normalizeLlmError(Exception ex, LlmConfig config) {
        Throwable root = rootCause(ex);
        String message = root.getMessage();
        if (message != null && message.contains("AllocationQuota.FreeTierOnly")) {
            return "Qwen 模型免费额度已用完，当前 API Key 开启了仅使用免费额度模式。请到 DashScope/阿里云百炼控制台关闭“仅使用免费额度”或开通付费额度，也可以切换到其他有额度的模型配置。";
        }
        if (message != null && message.contains("insufficient_quota")) {
            return "模型调用额度不足，请检查当前模型供应商账号余额、配额或更换有额度的 API Key。";
        }
        if (root instanceof OpenAiHttpException httpException) {
            return describeOpenAiHttpError(httpException, config);
        }
        return message == null || message.isBlank() ? "模型调用异常，请检查 Base URL、模型名、API Key 和供应商控制台日志。" : message;
    }

    private Throwable rootCause(Throwable ex) {
        Throwable current = ex;
        while (current.getCause() != null && current.getCause() != current) {
            current = current.getCause();
        }
        return current;
    }

    private String describeOpenAiHttpError(OpenAiHttpException ex, LlmConfig config) {
        String target = "当前配置：" + config.getProvider() + " / " + config.getModelName() + " / " + config.getBaseUrl();
        return switch (ex.code()) {
            case 400 -> "模型请求被供应商拒绝（HTTP 400）。常见原因：模型名不支持 OpenAI Chat/工具调用，或请求参数不兼容。建议切换到支持 function calling 的聊天模型，例如 Qwen 的 qwen-plus。 " + target;
            case 401 -> "模型调用认证失败（HTTP 401）。请检查 API Key 是否正确、是否过期。 " + target;
            case 403 -> "模型调用无权限（HTTP 403）。请检查账号是否开通该模型、API Key 权限、地域或付费状态。 " + target;
            case 404 -> "模型接口或模型名称不存在（HTTP 404）。请检查 Base URL 和模型名是否匹配供应商的 OpenAI 兼容接口。 " + target;
            case 429 -> "模型调用被限流或额度不足（HTTP 429）。请检查供应商额度、并发限制或稍后重试。 " + target;
            default -> {
                if (ex.code() >= 500) {
                    yield "模型供应商服务异常（HTTP " + ex.code() + "），请稍后重试或切换模型配置。 " + target;
                }
                yield "模型调用失败（HTTP " + ex.code() + "）。请检查模型配置、网络和供应商控制台错误详情。 " + target;
            }
        };
    }

    private String normalizeConfigType(String configType) {
        if (configType == null || configType.isBlank()) {
            return "CHAT";
        }
        String normalized = configType.trim().toUpperCase(Locale.ROOT);
        return "EMBEDDING".equals(normalized) ? "EMBEDDING" : "CHAT";
    }
}
