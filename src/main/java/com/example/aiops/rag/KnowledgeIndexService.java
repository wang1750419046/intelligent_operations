package com.example.aiops.rag;

import com.example.aiops.dto.KnowledgeReindexResponse;
import com.example.aiops.dto.VectorStatusResponse;
import com.example.aiops.entity.KnowledgeDocument;
import com.example.aiops.entity.LlmConfig;
import com.example.aiops.mapper.KnowledgeDocumentMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class KnowledgeIndexService {

    private final boolean vectorEnabled;
    private final int dimensions;
    private final KnowledgeDocumentMapper knowledgeDocumentMapper;
    private final EmbeddingConfigResolver embeddingConfigResolver;
    private final QwenEmbeddingService embeddingService;
    private final QdrantKnowledgeVectorStore vectorStore;

    public KnowledgeIndexService(@Value("${aiops.vector.enabled:true}") boolean vectorEnabled,
                                 @Value("${aiops.vector.embedding.dimensions:1024}") int dimensions,
                                 KnowledgeDocumentMapper knowledgeDocumentMapper,
                                 EmbeddingConfigResolver embeddingConfigResolver,
                                 QwenEmbeddingService embeddingService,
                                 QdrantKnowledgeVectorStore vectorStore) {
        this.vectorEnabled = vectorEnabled;
        this.dimensions = dimensions;
        this.knowledgeDocumentMapper = knowledgeDocumentMapper;
        this.embeddingConfigResolver = embeddingConfigResolver;
        this.embeddingService = embeddingService;
        this.vectorStore = vectorStore;
    }

    public KnowledgeReindexResponse reindex() {
        if (!vectorEnabled) {
            return new KnowledgeReindexResponse(0, 0, knowledgeDocumentMapper.findAll().size());
        }
        LlmConfig config = embeddingConfigResolver.resolveRequired();
        int success = 0;
        int failure = 0;
        for (KnowledgeDocument doc : knowledgeDocumentMapper.findAll()) {
            try {
                List<Float> vector = embeddingService.embed(toEmbeddingText(doc), config);
                if (vector.size() != dimensions) {
                    throw new IllegalStateException("embedding dimension mismatch: " + vector.size());
                }
                vectorStore.upsert(doc, vector, config.getModelName());
                success++;
            } catch (Exception ex) {
                failure++;
            }
        }
        return new KnowledgeReindexResponse(success, failure, 0);
    }

    public VectorStatusResponse status() {
        VectorStatusResponse response = new VectorStatusResponse();
        response.setEnabled(vectorEnabled);
        response.setCollection(vectorStore.getCollectionName());
        LlmConfig config = embeddingConfigResolver.resolveNullable();
        response.setEmbeddingConfigured(config != null && config.getApiKey() != null && !config.getApiKey().isBlank());
        response.setEmbeddingModel(config == null ? null : config.getModelName());
        if (!vectorEnabled) {
            response.setMessage("向量检索未启用");
            response.setIndexedCount(-1L);
            return response;
        }
        boolean collectionReady = vectorStore.collectionReady();
        response.setCollectionReady(collectionReady);
        response.setQdrantReachable(collectionReady);
        response.setIndexedCount(collectionReady ? vectorStore.indexedCount() : 0L);
        if (!response.isEmbeddingConfigured()) {
            response.setMessage("向量模型 API Key 未配置");
        } else if (!collectionReady) {
            response.setMessage("Qdrant collection 尚未初始化，请先重建索引");
        } else {
            response.setMessage("向量检索可用");
        }
        return response;
    }

    private String toEmbeddingText(KnowledgeDocument doc) {
        return doc.getTitle() + "\n" + doc.getContent() + "\n标签：" + (doc.getTags() == null ? "" : doc.getTags());
    }
}
