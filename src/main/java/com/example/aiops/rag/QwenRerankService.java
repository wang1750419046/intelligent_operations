package com.example.aiops.rag;

import com.example.aiops.entity.LlmConfig;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class QwenRerankService {

    private final RestClient.Builder restClientBuilder;

    public QwenRerankService(RestClient.Builder restClientBuilder) {
        this.restClientBuilder = restClientBuilder;
    }

    @SuppressWarnings("unchecked")
    public List<RerankResult> rerank(String query, List<String> documents, int topN, LlmConfig config) {
        if (documents == null || documents.isEmpty()) {
            return List.of();
        }
        if (config.getApiKey() == null || config.getApiKey().isBlank()) {
            throw new IllegalArgumentException("rerank config missing API Key");
        }
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("model", config.getModelName());
        body.put("query", query);
        body.put("documents", documents);
        body.put("top_n", Math.min(topN, documents.size()));
        body.put("instruct", "Given an operations incident search query, retrieve relevant runbook or case passages that answer the query.");
        Map<String, Object> response = restClientBuilder
                .baseUrl(trimTrailingSlash(config.getBaseUrl()))
                .build()
                .post()
                .uri("/reranks")
                .header("Authorization", "Bearer " + config.getApiKey())
                .body(body)
                .retrieve()
                .body(Map.class);
        return parseResults(response);
    }

    @SuppressWarnings("unchecked")
    public List<RerankResult> parseResults(Map<String, Object> response) {
        if (response == null || !(response.get("results") instanceof List<?> rawResults)) {
            return List.of();
        }
        List<RerankResult> results = new ArrayList<>();
        for (Object item : rawResults) {
            if (!(item instanceof Map<?, ?> map)) {
                continue;
            }
            Object rawIndex = map.get("index");
            Object rawScore = map.containsKey("relevance_score") ? map.get("relevance_score") : map.get("score");
            if (rawIndex instanceof Number indexNumber && rawScore instanceof Number scoreNumber) {
                results.add(new RerankResult(indexNumber.intValue(), scoreNumber.doubleValue()));
                continue;
            }
            Object document = map.get("document");
            if (document instanceof Map<?, ?> docMap && docMap.get("index") instanceof Number nestedIndex && rawScore instanceof Number nestedScore) {
                results.add(new RerankResult(nestedIndex.intValue(), nestedScore.doubleValue()));
            }
        }
        return results;
    }

    private String trimTrailingSlash(String value) {
        if (value == null || value.isBlank()) {
            return "";
        }
        return value.endsWith("/") ? value.substring(0, value.length() - 1) : value;
    }
}
