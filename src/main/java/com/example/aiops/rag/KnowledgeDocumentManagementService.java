package com.example.aiops.rag;

import com.example.aiops.dto.KnowledgeDocumentRequest;
import com.example.aiops.dto.KnowledgeDocumentResponse;
import com.example.aiops.dto.KnowledgeImportResponse;
import com.example.aiops.entity.KnowledgeDocument;
import com.example.aiops.exception.BusinessException;
import com.example.aiops.mapper.KnowledgeChunkMapper;
import com.example.aiops.mapper.KnowledgeDocumentMapper;
import com.example.aiops.util.TimeUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
public class KnowledgeDocumentManagementService {

    private final KnowledgeDocumentMapper knowledgeDocumentMapper;
    private final KnowledgeChunkMapper knowledgeChunkMapper;
    private final KnowledgeIndexService knowledgeIndexService;
    private final KnowledgeDocumentExtractor documentExtractor;

    public KnowledgeDocumentManagementService(KnowledgeDocumentMapper knowledgeDocumentMapper,
                                              KnowledgeChunkMapper knowledgeChunkMapper,
                                              KnowledgeIndexService knowledgeIndexService,
                                              KnowledgeDocumentExtractor documentExtractor) {
        this.knowledgeDocumentMapper = knowledgeDocumentMapper;
        this.knowledgeChunkMapper = knowledgeChunkMapper;
        this.knowledgeIndexService = knowledgeIndexService;
        this.documentExtractor = documentExtractor;
    }

    public List<KnowledgeDocumentResponse> list(String keyword,
                                                String source,
                                                String country,
                                                String businessLine,
                                                String systemName,
                                                String startTime,
                                                String endTime,
                                                List<String> permissionCodes) {
        return knowledgeDocumentMapper.searchDocuments(
                        keyword,
                        blankToNull(source),
                        blankToNull(country),
                        blankToNull(businessLine),
                        blankToNull(systemName),
                        KnowledgeTextUtils.parseDateTime(startTime),
                        KnowledgeTextUtils.parseDateTime(endTime),
                        KnowledgeTextUtils.normalizeCodes(permissionCodes))
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public KnowledgeDocumentResponse create(KnowledgeDocumentRequest request) {
        KnowledgeDocument document = fromRequest(request, "kb_" + UUID.randomUUID().toString().replace("-", ""));
        knowledgeDocumentMapper.insert(document);
        KnowledgeIndexService.IndexResult result = knowledgeIndexService.indexDocument(document);
        KnowledgeDocument saved = knowledgeDocumentMapper.findByDocId(document.getDocId());
        KnowledgeDocumentResponse response = toResponse(saved);
        response.setChunkCount(result.chunkCount());
        return response;
    }

    @Transactional
    public KnowledgeImportResponse importFile(MultipartFile file,
                                              String title,
                                              String source,
                                              String tags,
                                              String country,
                                              String businessLine,
                                              String systemName,
                                              String eventTime,
                                              List<String> permissionCodes) {
        KnowledgeDocumentExtractor.ExtractedDocument extracted = documentExtractor.extract(file);
        KnowledgeDocumentRequest request = new KnowledgeDocumentRequest();
        request.setTitle(blankToNull(title) == null ? inferTitle(file) : title.trim());
        request.setContent(extracted.text());
        request.setSource(blankToNull(source) == null ? "upload" : source.trim());
        request.setTags(tags);
        request.setCountry(country);
        request.setBusinessLine(businessLine);
        request.setSystemName(systemName);
        request.setEventTime(eventTime);
        request.setPermissionCodes(permissionCodes);
        KnowledgeDocument document = fromRequest(request, "kb_" + UUID.randomUUID().toString().replace("-", ""));
        document.setFilename(file.getOriginalFilename());
        document.setMimeType(extracted.mimeType());
        knowledgeDocumentMapper.insert(document);
        KnowledgeIndexService.IndexResult result = knowledgeIndexService.indexDocument(document);
        KnowledgeDocument saved = knowledgeDocumentMapper.findByDocId(document.getDocId());
        return new KnowledgeImportResponse(saved.getDocId(), saved.getTitle(), saved.getFilename(), extracted.text().length(), result.chunkCount(), saved.getIndexStatus());
    }

    @Transactional
    public KnowledgeDocumentResponse update(String docId, KnowledgeDocumentRequest request) {
        KnowledgeDocument existing = findRequired(docId);
        KnowledgeDocument document = fromRequest(request, docId);
        document.setId(existing.getId());
        document.setFilename(existing.getFilename());
        document.setMimeType(existing.getMimeType());
        document.setCreatedAt(existing.getCreatedAt());
        knowledgeDocumentMapper.updateByDocId(document);
        KnowledgeIndexService.IndexResult result = knowledgeIndexService.indexDocument(document);
        KnowledgeDocument saved = knowledgeDocumentMapper.findByDocId(docId);
        KnowledgeDocumentResponse response = toResponse(saved);
        response.setChunkCount(result.chunkCount());
        return response;
    }

    @Transactional
    public void delete(String docId) {
        findRequired(docId);
        knowledgeIndexService.deleteDocumentIndex(docId);
        knowledgeDocumentMapper.deleteByDocId(docId);
    }

    private KnowledgeDocument fromRequest(KnowledgeDocumentRequest request, String docId) {
        LocalDateTime now = LocalDateTime.now();
        String normalizedContent = KnowledgeTextUtils.normalizeText(request.getContent());
        if (normalizedContent.isBlank()) {
            throw new BusinessException(40044, "knowledge content is empty");
        }
        KnowledgeDocument document = new KnowledgeDocument();
        document.setDocId(docId);
        document.setTitle(request.getTitle().trim());
        document.setContent(normalizedContent);
        document.setSource(request.getSource().trim());
        document.setTags(blankToNull(request.getTags()));
        document.setCountry(blankToNull(request.getCountry()));
        document.setBusinessLine(blankToNull(request.getBusinessLine()));
        document.setSystemName(blankToNull(request.getSystemName()));
        document.setEventTime(KnowledgeTextUtils.parseDateTime(request.getEventTime()));
        document.setPermissionCodes(KnowledgeTextUtils.joinCodes(request.getPermissionCodes()));
        document.setContentHash(KnowledgeTextUtils.sha256(document.getTitle() + "\n" + normalizedContent));
        document.setIndexStatus("PENDING");
        document.setEmbeddingStatus("PENDING");
        document.setCreatedAt(now);
        document.setUpdatedAt(now);
        return document;
    }

    private KnowledgeDocument findRequired(String docId) {
        KnowledgeDocument document = knowledgeDocumentMapper.findByDocId(docId);
        if (document == null) {
            throw new BusinessException(40403, "knowledge document not found");
        }
        return document;
    }

    private KnowledgeDocumentResponse toResponse(KnowledgeDocument document) {
        KnowledgeDocumentResponse response = new KnowledgeDocumentResponse();
        response.setDocId(document.getDocId());
        response.setTitle(document.getTitle());
        response.setContent(document.getContent());
        response.setSource(document.getSource());
        response.setTags(document.getTags());
        response.setFilename(document.getFilename());
        response.setMimeType(document.getMimeType());
        response.setCountry(document.getCountry());
        response.setBusinessLine(document.getBusinessLine());
        response.setSystemName(document.getSystemName());
        response.setEventTime(TimeUtils.format(document.getEventTime()));
        response.setPermissionCodes(document.getPermissionCodes());
        response.setIndexStatus(document.getIndexStatus());
        response.setEmbeddingStatus(document.getEmbeddingStatus());
        response.setChunkCount(knowledgeChunkMapper.countByDocId(document.getDocId()));
        response.setCreatedAt(TimeUtils.format(document.getCreatedAt()));
        response.setUpdatedAt(TimeUtils.format(document.getUpdatedAt()));
        return response;
    }

    private String inferTitle(MultipartFile file) {
        String filename = file.getOriginalFilename();
        if (filename == null || filename.isBlank()) {
            return "Imported knowledge";
        }
        int dot = filename.lastIndexOf('.');
        return dot > 0 ? filename.substring(0, dot) : filename;
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
