package com.example.aiops.rag;

import com.example.aiops.entity.LlmConfig;
import com.example.aiops.service.impl.ChatModelFactory;
import com.example.aiops.service.impl.ModelConfigServiceImpl;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.langchain4j.model.chat.ChatLanguageModel;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class QueryRewriteService {

    private final ModelConfigServiceImpl modelConfigService;
    private final ChatModelFactory chatModelFactory;
    private final ObjectMapper objectMapper;

    public QueryRewriteService(ModelConfigServiceImpl modelConfigService,
                               ChatModelFactory chatModelFactory,
                               ObjectMapper objectMapper) {
        this.modelConfigService = modelConfigService;
        this.chatModelFactory = chatModelFactory;
        this.objectMapper = objectMapper;
    }

    public RewrittenQuery rewrite(String query) {
        if (query == null || query.isBlank()) {
            return RewrittenQuery.fallback("");
        }
        try {
            LlmConfig config = modelConfigService.resolveActiveConfig(null);
            if (config.getApiKey() == null || config.getApiKey().isBlank()) {
                return heuristic(query);
            }
            ChatLanguageModel model = chatModelFactory.create(config);
            String reply = model.generate(prompt(query));
            return parse(reply, query);
        } catch (Exception ex) {
            return heuristic(query);
        }
    }

    private RewrittenQuery parse(String reply, String fallbackQuery) throws Exception {
        String json = extractJson(reply);
        JsonNode root = objectMapper.readTree(json);
        String rewritten = text(root, "query");
        List<String> keywords = new ArrayList<>();
        JsonNode keywordNode = root.get("keywords");
        if (keywordNode != null && keywordNode.isArray()) {
            for (JsonNode item : keywordNode) {
                if (item.isTextual() && !item.asText().isBlank()) {
                    keywords.add(item.asText().trim());
                }
            }
        }
        if (keywords.isEmpty()) {
            keywords.addAll(KnowledgeTextUtils.tokens(fallbackQuery));
        }
        return new RewrittenQuery(
                rewritten == null || rewritten.isBlank() ? fallbackQuery : rewritten,
                keywords,
                text(root, "country"),
                text(root, "businessLine"),
                text(root, "systemName"));
    }

    private RewrittenQuery heuristic(String query) {
        return new RewrittenQuery(query, KnowledgeTextUtils.tokens(query), null, null, null);
    }

    private String extractJson(String reply) {
        if (reply == null) {
            return "{}";
        }
        int start = reply.indexOf('{');
        int end = reply.lastIndexOf('}');
        if (start >= 0 && end > start) {
            return reply.substring(start, end + 1);
        }
        return reply;
    }

    private String text(JsonNode root, String field) {
        JsonNode node = root.get(field);
        return node != null && node.isTextual() && !node.asText().isBlank() ? node.asText().trim() : null;
    }

    private String prompt(String query) {
        return """
                Rewrite the user question for AIOps knowledge-base retrieval.
                Return only compact JSON with fields:
                query: rewritten retrieval query in Chinese or mixed Chinese/English,
                keywords: array of important terms,
                country: country filter or null,
                businessLine: business-line filter or null,
                systemName: system/service/system filter or null.
                User question:
                %s
                """.formatted(query);
    }
}
