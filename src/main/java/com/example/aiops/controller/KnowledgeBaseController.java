package com.example.aiops.controller;

import com.example.aiops.dto.KnowledgeSearchRequest;
import com.example.aiops.dto.ReferenceItem;
import com.example.aiops.dto.UnifiedResponse;
import com.example.aiops.entity.KnowledgeDocument;
import com.example.aiops.rag.KnowledgeBaseService;
import com.example.aiops.util.TraceIdHolder;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/kb")
public class KnowledgeBaseController {

    private final KnowledgeBaseService knowledgeBaseService;

    public KnowledgeBaseController(KnowledgeBaseService knowledgeBaseService) {
        this.knowledgeBaseService = knowledgeBaseService;
    }

    @PostMapping("/search")
    public UnifiedResponse<List<ReferenceItem>> search(@Valid @RequestBody KnowledgeSearchRequest request) {
        List<KnowledgeDocument> results = knowledgeBaseService.search(request.getQuery(), 5);
        List<ReferenceItem> data = results.stream()
                .map(doc -> new ReferenceItem("kb", doc.getTitle(), doc.getContent(), doc.getSource()))
                .toList();
        return UnifiedResponse.success(data, TraceIdHolder.getTraceId());
    }
}
