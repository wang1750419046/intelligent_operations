package com.example.aiops.tools;

import com.example.aiops.dto.ReferenceItem;
import com.example.aiops.entity.KnowledgeDocument;
import com.example.aiops.rag.KnowledgeBaseService;
import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

@Component
public class KnowledgeSearchTool {

    private final KnowledgeBaseService knowledgeBaseService;
    private final ToolCallbackSupport toolCallbackSupport;

    public KnowledgeSearchTool(KnowledgeBaseService knowledgeBaseService, ToolCallbackSupport toolCallbackSupport) {
        this.knowledgeBaseService = knowledgeBaseService;
        this.toolCallbackSupport = toolCallbackSupport;
    }

    @Tool("检索历史故障案例和知识库条目")
    public String searchKnowledge(@P("query") String query) {
        String safeQuery = query == null || query.isBlank() ? "接口变慢" : query;
        long startNanos = System.nanoTime();
        toolCallbackSupport.started("search_knowledge");
        String params = "{query=%s}".formatted(safeQuery);
        try {
            List<KnowledgeDocument> docs = knowledgeBaseService.search(safeQuery, 3);
            String summary;
            List<ReferenceItem> references;
            if (docs.isEmpty()) {
                summary = "未检索到高度相关的知识库条目，当前只能基于工具证据进行推测。";
                references = List.of(new ReferenceItem("kb", "知识库检索结果", summary, "mysql-kb"));
            } else {
                summary = docs.stream()
                        .map(doc -> doc.getTitle() + formatScore(doc.getSimilarityScore()) + ": " + doc.getContent())
                        .collect(Collectors.joining(" | "));
                references = docs.stream()
                        .map(doc -> new ReferenceItem("kb", doc.getTitle(), doc.getContent(), doc.getSource(), doc.getSimilarityScore()))
                        .toList();
            }
            toolCallbackSupport.success("search_knowledge", params, summary, references);
            toolCallbackSupport.finished("search_knowledge", toolCallbackSupport.elapsedMs(startNanos));
            return summary;
        } catch (Exception ex) {
            toolCallbackSupport.finished("search_knowledge", toolCallbackSupport.elapsedMs(startNanos));
            return toolCallbackSupport.failure("search_knowledge", params, ex);
        }
    }

    private String formatScore(Double score) {
        if (score == null) {
            return "";
        }
        return " (相似度 " + String.format("%.2f", score) + ")";
    }
}
