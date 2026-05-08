package com.example.aiops.rag;

import com.example.aiops.entity.KnowledgeDocument;
import com.example.aiops.entity.LlmConfig;
import com.example.aiops.mapper.KnowledgeDocumentMapper;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@Primary
@ConditionalOnProperty(prefix = "aiops.vector", name = "enabled", havingValue = "true")
public class QdrantKnowledgeBaseService implements KnowledgeBaseService {

    private final EmbeddingConfigResolver embeddingConfigResolver;
    private final QwenEmbeddingService embeddingService;
    private final QdrantKnowledgeVectorStore vectorStore;
    private final KnowledgeDocumentMapper knowledgeDocumentMapper;
    private final InMemoryKnowledgeBaseService fallbackService;

    public QdrantKnowledgeBaseService(EmbeddingConfigResolver embeddingConfigResolver,
                                      QwenEmbeddingService embeddingService,
                                      QdrantKnowledgeVectorStore vectorStore,
                                      KnowledgeDocumentMapper knowledgeDocumentMapper,
                                      InMemoryKnowledgeBaseService fallbackService) {
        this.embeddingConfigResolver = embeddingConfigResolver;
        this.embeddingService = embeddingService;
        this.vectorStore = vectorStore;
        this.knowledgeDocumentMapper = knowledgeDocumentMapper;
        this.fallbackService = fallbackService;
    }

    @Override
    public List<KnowledgeDocument> search(String query, int limit) {
        try {
            LlmConfig embeddingConfig = embeddingConfigResolver.resolveRequired();
            List<Float> vector = embeddingService.embed(query, embeddingConfig);
            List<VectorSearchHit> hits = vectorStore.search(vector, limit);
            if (hits.isEmpty()) {
                return fallbackService.search(query, limit);
            }
            Map<String, VectorSearchHit> hitMap = hits.stream()
                    .collect(Collectors.toMap(VectorSearchHit::getDocId, Function.identity(), (left, right) -> left));
            return knowledgeDocumentMapper.findByDocIds(hits.stream().map(VectorSearchHit::getDocId).toList()).stream()
                    .peek(doc -> doc.setSimilarityScore(hitMap.get(doc.getDocId()).getScore()))
                    .sorted(Comparator.comparingDouble((KnowledgeDocument doc) -> doc.getSimilarityScore() == null ? 0D : doc.getSimilarityScore()).reversed())
                    .limit(limit)
                    .toList();
        } catch (Exception ex) {
            return fallbackService.search(query, limit);
        }
    }
}
