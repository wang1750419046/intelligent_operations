package com.example.aiops.controller;

import com.example.aiops.dto.KnowledgeDocumentRequest;
import com.example.aiops.dto.KnowledgeDocumentResponse;
import com.example.aiops.dto.KnowledgeImportResponse;
import com.example.aiops.dto.KnowledgeReindexResponse;
import com.example.aiops.dto.KnowledgeSearchRequest;
import com.example.aiops.dto.ReferenceItem;
import com.example.aiops.dto.UnifiedResponse;
import com.example.aiops.dto.VectorStatusResponse;
import com.example.aiops.entity.KnowledgeDocument;
import com.example.aiops.rag.KnowledgeBaseService;
import com.example.aiops.rag.KnowledgeDocumentManagementService;
import com.example.aiops.rag.KnowledgeIndexService;
import com.example.aiops.rag.KnowledgeSearchCriteria;
import com.example.aiops.rag.KnowledgeTextUtils;
import com.example.aiops.util.TraceIdHolder;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/kb")
public class KnowledgeBaseController {

    private final KnowledgeBaseService knowledgeBaseService;
    private final KnowledgeIndexService knowledgeIndexService;
    private final KnowledgeDocumentManagementService documentManagementService;

    public KnowledgeBaseController(KnowledgeBaseService knowledgeBaseService,
                                   KnowledgeIndexService knowledgeIndexService,
                                   KnowledgeDocumentManagementService documentManagementService) {
        this.knowledgeBaseService = knowledgeBaseService;
        this.knowledgeIndexService = knowledgeIndexService;
        this.documentManagementService = documentManagementService;
    }

    @GetMapping("/documents")
    public UnifiedResponse<List<KnowledgeDocumentResponse>> listDocuments(@RequestParam(required = false) String keyword,
                                                                          @RequestParam(required = false) String source,
                                                                          @RequestParam(required = false) String country,
                                                                          @RequestParam(required = false) String businessLine,
                                                                          @RequestParam(required = false) String systemName,
                                                                          @RequestParam(required = false) String startTime,
                                                                          @RequestParam(required = false) String endTime,
                                                                          @RequestParam(required = false) List<String> permissionCodes) {
        return UnifiedResponse.success(
                documentManagementService.list(keyword, source, country, businessLine, systemName, startTime, endTime, permissionCodes),
                TraceIdHolder.getTraceId());
    }

    @PostMapping("/documents")
    public UnifiedResponse<KnowledgeDocumentResponse> createDocument(@Valid @RequestBody KnowledgeDocumentRequest request) {
        return UnifiedResponse.success(documentManagementService.create(request), TraceIdHolder.getTraceId());
    }

    @PostMapping("/documents/import")
    public UnifiedResponse<KnowledgeImportResponse> importDocument(@RequestPart("file") MultipartFile file,
                                                                   @RequestParam(required = false) String title,
                                                                   @RequestParam(required = false) String source,
                                                                   @RequestParam(required = false) String tags,
                                                                   @RequestParam(required = false) String country,
                                                                   @RequestParam(required = false) String businessLine,
                                                                   @RequestParam(required = false) String systemName,
                                                                   @RequestParam(required = false) String eventTime,
                                                                   @RequestParam(required = false) List<String> permissionCodes) {
        return UnifiedResponse.success(
                documentManagementService.importFile(file, title, source, tags, country, businessLine, systemName, eventTime, permissionCodes),
                TraceIdHolder.getTraceId());
    }

    @PutMapping("/documents/{docId}")
    public UnifiedResponse<KnowledgeDocumentResponse> updateDocument(@PathVariable String docId,
                                                                     @Valid @RequestBody KnowledgeDocumentRequest request) {
        return UnifiedResponse.success(documentManagementService.update(docId, request), TraceIdHolder.getTraceId());
    }

    @DeleteMapping("/documents/{docId}")
    public UnifiedResponse<Void> deleteDocument(@PathVariable String docId) {
        documentManagementService.delete(docId);
        return UnifiedResponse.success(null, TraceIdHolder.getTraceId());
    }

    @PostMapping("/search")
    public UnifiedResponse<List<ReferenceItem>> search(@Valid @RequestBody KnowledgeSearchRequest request) {
        KnowledgeSearchCriteria criteria = toCriteria(request);
        List<KnowledgeDocument> results = knowledgeBaseService.search(criteria);
        List<ReferenceItem> data = results.stream()
                .map(doc -> new ReferenceItem("kb", doc.getTitle(), doc.getContent(), doc.getSource(), doc.getSimilarityScore()))
                .toList();
        return UnifiedResponse.success(data, TraceIdHolder.getTraceId());
    }

    @PostMapping("/reindex")
    public UnifiedResponse<KnowledgeReindexResponse> reindex() {
        return UnifiedResponse.success(knowledgeIndexService.reindex(), TraceIdHolder.getTraceId());
    }

    @GetMapping("/vector/status")
    public UnifiedResponse<VectorStatusResponse> vectorStatus() {
        return UnifiedResponse.success(knowledgeIndexService.status(), TraceIdHolder.getTraceId());
    }

    private KnowledgeSearchCriteria toCriteria(KnowledgeSearchRequest request) {
        KnowledgeSearchCriteria criteria = new KnowledgeSearchCriteria();
        criteria.setQuery(request.getQuery());
        criteria.setLimit(request.getLimit() == null ? 5 : request.getLimit());
        criteria.setCountry(request.getCountry());
        criteria.setBusinessLine(request.getBusinessLine());
        criteria.setSystemName(request.getSystemName());
        criteria.setStartTime(KnowledgeTextUtils.parseDateTime(request.getStartTime()));
        criteria.setEndTime(KnowledgeTextUtils.parseDateTime(request.getEndTime()));
        criteria.setPermissionCodes(request.getPermissionCodes());
        return criteria;
    }
}
