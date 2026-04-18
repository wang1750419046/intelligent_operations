package com.example.aiops.tools;

import com.example.aiops.entity.KnowledgeDocument;
import com.example.aiops.rag.KnowledgeBaseService;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;

class KnowledgeSearchToolTest {

    @Test
    void shouldReturnFallbackWhenNoKnowledgeFound() {
        KnowledgeBaseService knowledgeBaseService = Mockito.mock(KnowledgeBaseService.class);
        Mockito.when(knowledgeBaseService.search("空结果", 3)).thenReturn(List.of());
        ToolCallbackSupport callbackSupport = new ToolCallbackSupport(Mockito.mock(com.example.aiops.service.AgentTraceService.class));
        KnowledgeSearchTool tool = new KnowledgeSearchTool(knowledgeBaseService, callbackSupport);
        String result = tool.searchKnowledge("空结果");
        assertTrue(result.contains("未检索到高度相关"));
    }

    @Test
    void shouldReturnKnowledgeSummaryWhenMatchesFound() {
        KnowledgeDocument doc = new KnowledgeDocument();
        doc.setTitle("数据库连接池耗尽排查");
        doc.setContent("优先检查连接池配置和慢 SQL。");
        doc.setSource("wiki");
        KnowledgeBaseService knowledgeBaseService = Mockito.mock(KnowledgeBaseService.class);
        Mockito.when(knowledgeBaseService.search("连接池", 3)).thenReturn(List.of(doc));
        ToolCallbackSupport callbackSupport = new ToolCallbackSupport(Mockito.mock(com.example.aiops.service.AgentTraceService.class));
        KnowledgeSearchTool tool = new KnowledgeSearchTool(knowledgeBaseService, callbackSupport);
        String result = tool.searchKnowledge("连接池");
        assertTrue(result.contains("数据库连接池耗尽排查"));
    }
}
