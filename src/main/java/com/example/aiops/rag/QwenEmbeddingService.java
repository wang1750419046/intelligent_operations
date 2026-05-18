package com.example.aiops.rag;

import com.example.aiops.exception.BusinessException;
import com.example.aiops.entity.LlmConfig;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class QwenEmbeddingService {

    private final RestClient.Builder restClientBuilder;
    private final int dimensions;
    private final long remoteTimeoutMs;

    public QwenEmbeddingService(RestClient.Builder restClientBuilder,
                                @Value("${aiops.vector.embedding.dimensions:1024}") int dimensions,
                                @Value("${aiops.rag.remote-timeout-ms:2000}") long remoteTimeoutMs) {
        this.restClientBuilder = restClientBuilder;
        this.dimensions = dimensions;
        this.remoteTimeoutMs = remoteTimeoutMs;
    }

    public List<Float> embed(String text, LlmConfig config) {
        List<List<Float>> embeddings = embed(List.of(text), config);
        if (embeddings.isEmpty()) {
            throw new IllegalStateException("embedding response is empty");
        }
        return embeddings.get(0);
    }

    @SuppressWarnings("unchecked")
    public List<List<Float>> embed(List<String> texts, LlmConfig config) {
        if (config.getApiKey() == null || config.getApiKey().isBlank()) {
            throw new BusinessException(40031, "embedding config missing API Key");
        }
        try {
            if (isMultimodalEmbeddingModel(config.getModelName())) {
                return embedMultimodalTexts(texts, config);
            }
            return embedOpenAiCompatible(texts, config);
        } catch (RestClientResponseException ex) {
            throw new BusinessException(40032, describeEmbeddingHttpError(ex, config));
        }
    }

    @SuppressWarnings("unchecked")
    private List<List<Float>> embedOpenAiCompatible(List<String> texts, LlmConfig config) {
        String modelName = config.getModelName();
        if (!isOpenAiCompatibleEmbeddingModel(modelName)) {
            throw new BusinessException(40032, "当前向量模型 " + modelName
                    + " 不支持 OpenAI 兼容 Embedding 接口；请使用 text-embedding-v4/text-embedding-v3，或使用通义视觉多模态向量模型。");
        }
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("model", modelName);
        body.put("input", texts);
        if (supportsOpenAiDimensions(modelName)) {
            body.put("dimensions", dimensions);
            body.put("encoding_format", "float");
        }
        Map<String, Object> response = timedBuilder()
                .baseUrl(trimTrailingSlash(config.getBaseUrl()))
                .build()
                .post()
                .uri("/embeddings")
                .header("Authorization", "Bearer " + config.getApiKey())
                .body(body)
                .retrieve()
                .body(Map.class);
        if (response == null || !(response.get("data") instanceof List<?> data)) {
            throw new IllegalStateException("invalid embedding response");
        }
        List<List<Float>> vectors = new ArrayList<>();
        for (Object item : data) {
            if (!(item instanceof Map<?, ?> itemMap) || !(itemMap.get("embedding") instanceof List<?> rawVector)) {
                continue;
            }
            List<Float> vector = new ArrayList<>(rawVector.size());
            for (Object value : rawVector) {
                if (value instanceof Number number) {
                    vector.add(number.floatValue());
                }
            }
            vectors.add(vector);
        }
        return vectors;
    }

    private List<List<Float>> embedMultimodalTexts(List<String> texts, LlmConfig config) {
        validateMultimodalDimension(config.getModelName());
        List<List<Float>> vectors = new ArrayList<>(texts.size());
        for (String text : texts) {
            vectors.add(embedOneMultimodalText(text, config));
        }
        return vectors;
    }

    @SuppressWarnings("unchecked")
    private List<Float> embedOneMultimodalText(String text, LlmConfig config) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("model", config.getModelName());
        body.put("input", Map.of("contents", List.of(Map.of("text", text == null ? "" : text))));
        if (supportsMultimodalDimensions(config.getModelName())) {
            body.put("parameters", Map.of("dimension", dimensions));
        }
        Map<String, Object> response = timedBuilder()
                .baseUrl(multimodalBaseUrl(config.getBaseUrl()))
                .build()
                .post()
                .uri("/api/v1/services/embeddings/multimodal-embedding/multimodal-embedding")
                .header("Authorization", "Bearer " + config.getApiKey())
                .body(body)
                .retrieve()
                .body(Map.class);
        if (response == null || !(response.get("output") instanceof Map<?, ?> output)
                || !(output.get("embeddings") instanceof List<?> embeddings)
                || embeddings.isEmpty()) {
            throw new IllegalStateException("invalid multimodal embedding response");
        }
        Object first = embeddings.get(0);
        if (!(first instanceof Map<?, ?> item) || !(item.get("embedding") instanceof List<?> rawVector)) {
            throw new IllegalStateException("invalid multimodal embedding response");
        }
        return toFloatVector(rawVector);
    }

    private List<Float> toFloatVector(List<?> rawVector) {
        List<Float> vector = new ArrayList<>(rawVector.size());
        for (Object value : rawVector) {
            if (value instanceof Number number) {
                vector.add(number.floatValue());
            }
        }
        return vector;
    }

    private boolean isOpenAiCompatibleEmbeddingModel(String modelName) {
        return modelName != null && modelName.startsWith("text-embedding-v");
    }

    private boolean supportsOpenAiDimensions(String modelName) {
        return modelName != null && (modelName.startsWith("text-embedding-v3") || modelName.startsWith("text-embedding-v4"));
    }

    private boolean isMultimodalEmbeddingModel(String modelName) {
        return modelName != null && (modelName.contains("embedding-vision")
                || modelName.contains("vl-embedding")
                || "multimodal-embedding-v1".equals(modelName));
    }

    private boolean supportsMultimodalDimensions(String modelName) {
        return "qwen3-vl-embedding".equals(modelName)
                || "qwen2.5-vl-embedding".equals(modelName)
                || "tongyi-embedding-vision-plus-2026-03-06".equals(modelName)
                || "tongyi-embedding-vision-flash-2026-03-06".equals(modelName);
    }

    private void validateMultimodalDimension(String modelName) {
        if ("tongyi-embedding-vision-plus".equals(modelName) && dimensions != 1152) {
            throw new BusinessException(40033, "tongyi-embedding-vision-plus 固定返回 1152 维；请将 aiops.vector.embedding.dimensions 配置为 1152，或改用支持 1024 维的模型。");
        }
        if ("tongyi-embedding-vision-flash".equals(modelName) && dimensions != 768) {
            throw new BusinessException(40033, "tongyi-embedding-vision-flash 固定返回 768 维；请将 aiops.vector.embedding.dimensions 配置为 768，或改用支持 1024 维的模型。");
        }
        if ("multimodal-embedding-v1".equals(modelName) && dimensions != 1024) {
            throw new BusinessException(40033, "multimodal-embedding-v1 固定返回 1024 维；请将 aiops.vector.embedding.dimensions 配置为 1024。");
        }
        if ("tongyi-embedding-vision-flash-2026-03-06".equals(modelName)
                && dimensions != 64 && dimensions != 128 && dimensions != 256 && dimensions != 512 && dimensions != 768) {
            throw new BusinessException(40033, "tongyi-embedding-vision-flash-2026-03-06 不支持 " + dimensions + " 维；请改为 64、128、256、512 或 768。");
        }
    }

    private String multimodalBaseUrl(String value) {
        String normalized = trimTrailingSlash(value);
        if (normalized.isBlank()) {
            return "https://dashscope.aliyuncs.com";
        }
        for (String marker : List.of("/compatible-mode", "/compatible-api", "/api/v1")) {
            int index = normalized.indexOf(marker);
            if (index > 0) {
                return normalized.substring(0, index);
            }
        }
        return normalized;
    }

    private String describeEmbeddingHttpError(RestClientResponseException ex, LlmConfig config) {
        String body = ex.getResponseBodyAsString();
        if (body.contains("model_not_supported") || body.contains("Unsupported model")) {
            return "当前向量模型 " + config.getModelName()
                    + " 不支持当前接口。OpenAI 兼容接口请使用 text-embedding-v4/text-embedding-v3；通义视觉多模态模型会自动走多模态向量接口，请确认 Base URL 使用 DashScope 域名且模型名、维度配置匹配。";
        }
        return body.isBlank() ? ex.getMessage() : body;
    }

    private String trimTrailingSlash(String value) {
        if (value == null || value.isBlank()) {
            return "";
        }
        return value.endsWith("/") ? value.substring(0, value.length() - 1) : value;
    }

    private RestClient.Builder timedBuilder() {
        if (remoteTimeoutMs <= 0) {
            return restClientBuilder.clone();
        }
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(Duration.ofMillis(remoteTimeoutMs));
        factory.setReadTimeout(Duration.ofMillis(remoteTimeoutMs));
        return restClientBuilder.clone().requestFactory(factory);
    }
}
