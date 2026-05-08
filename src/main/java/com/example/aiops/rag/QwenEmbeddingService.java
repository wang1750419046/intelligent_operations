package com.example.aiops.rag;

import com.example.aiops.entity.LlmConfig;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
public class QwenEmbeddingService {

    private final RestClient.Builder restClientBuilder;

    public QwenEmbeddingService(RestClient.Builder restClientBuilder) {
        this.restClientBuilder = restClientBuilder;
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
            throw new IllegalArgumentException("embedding config missing API Key");
        }
        Map<String, Object> body = Map.of(
                "model", config.getModelName(),
                "input", texts
        );
        Map<String, Object> response = restClientBuilder
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

    private String trimTrailingSlash(String value) {
        if (value == null || value.isBlank()) {
            return "";
        }
        return value.endsWith("/") ? value.substring(0, value.length() - 1) : value;
    }
}
