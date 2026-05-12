package com.example.aiops.rag;

import com.example.aiops.dto.KnowledgeReindexResponse;
import com.example.aiops.dto.VectorStatusResponse;
import com.example.aiops.entity.KnowledgeChunk;
import com.example.aiops.entity.KnowledgeDocument;
import com.example.aiops.entity.LlmConfig;
import com.example.aiops.mapper.KnowledgeChunkMapper;
import com.example.aiops.mapper.KnowledgeDocumentMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
public class KnowledgeIndexService {

    private final boolean vectorEnabled;
    private final int dimensions;
    private final int batchSize;
    private final KnowledgeDocumentMapper knowledgeDocumentMapper;
    private final KnowledgeChunkMapper knowledgeChunkMapper;
    private final KnowledgeChunker knowledgeChunker;
    private final EmbeddingConfigResolver embeddingConfigResolver;
    private final QwenEmbeddingService embeddingService;
    private final QdrantKnowledgeVectorStore vectorStore;

    public KnowledgeIndexService(@Value("${aiops.vector.enabled:true}") boolean vectorEnabled,
                                 @Value("${aiops.vector.embedding.dimensions:1024}") int dimensions,
                                 @Value("${aiops.vector.embedding.batch-size:10}") int batchSize,
                                 KnowledgeDocumentMapper knowledgeDocumentMapper,
                                 KnowledgeChunkMapper knowledgeChunkMapper,
                                 KnowledgeChunker knowledgeChunker,
                                 EmbeddingConfigResolver embeddingConfigResolver,
                                 QwenEmbeddingService embeddingService,
                                 QdrantKnowledgeVectorStore vectorStore) {
        this.vectorEnabled = vectorEnabled;
        this.dimensions = dimensions;
        this.batchSize = Math.max(1, Math.min(batchSize, 10));
        this.knowledgeDocumentMapper = knowledgeDocumentMapper;
        this.knowledgeChunkMapper = knowledgeChunkMapper;
        this.knowledgeChunker = knowledgeChunker;
        this.embeddingConfigResolver = embeddingConfigResolver;
        this.embeddingService = embeddingService;
        this.vectorStore = vectorStore;
    }

    public KnowledgeReindexResponse reindex() {
        int success = 0;
        int failure = 0;
        int skipped = 0;
        for (KnowledgeDocument doc : knowledgeDocumentMapper.findAll()) {
            try {
                IndexResult result = indexDocument(doc);
                if (result.indexedChunks() > 0) {
                    success++;
                } else {
                    skipped++;
                }
            } catch (Exception ex) {
                failure++;
            }
        }
        return new KnowledgeReindexResponse(success, failure, skipped);
    }

    public IndexResult indexDocument(KnowledgeDocument doc) {
        deleteDocumentIndex(doc.getDocId());
        List<KnowledgeChunk> chunks = knowledgeChunker.split(doc);
        for (KnowledgeChunk chunk : chunks) {
            knowledgeChunkMapper.insert(chunk);
        }
        if (chunks.isEmpty()) {
            knowledgeDocumentMapper.updateIndexStatus(doc.getDocId(), "EMPTY", "EMPTY", LocalDateTime.now());
            return new IndexResult(0, 0);
        }
        if (!vectorEnabled) {
            for (KnowledgeChunk chunk : chunks) {
                knowledgeChunkMapper.updateEmbeddingStatus(chunk.getChunkId(), "SKIPPED", null, null, LocalDateTime.now());
            }
            knowledgeDocumentMapper.updateIndexStatus(doc.getDocId(), "SKIPPED", "SKIPPED", LocalDateTime.now());
            return new IndexResult(chunks.size(), 0);
        }
        LlmConfig config = embeddingConfigResolver.resolveRequired();
        int indexed = 0;
        for (int start = 0; start < chunks.size(); start += batchSize) {
            List<KnowledgeChunk> batch = chunks.subList(start, Math.min(start + batchSize, chunks.size()));
            List<String> texts = new ArrayList<>();
            for (KnowledgeChunk chunk : batch) {
                texts.add(toEmbeddingText(doc, chunk));
            }
            List<List<Float>> vectors = embeddingService.embed(texts, config);
            if (vectors.size() != batch.size()) {
                throw new IllegalStateException("embedding batch size mismatch: " + vectors.size());
            }
            for (int i = 0; i < batch.size(); i++) {
                List<Float> vector = vectors.get(i);
                if (vector.size() != dimensions) {
                    throw new IllegalStateException("embedding dimension mismatch: " + vector.size());
                }
                KnowledgeChunk chunk = batch.get(i);
                vectorStore.upsertChunk(doc, chunk, vector, config.getModelName());
                knowledgeChunkMapper.updateEmbeddingStatus(chunk.getChunkId(), "READY", config.getModelName(), LocalDateTime.now(), LocalDateTime.now());
                indexed++;
            }
        }
        knowledgeDocumentMapper.updateIndexStatus(doc.getDocId(), "READY", "READY", LocalDateTime.now());
        return new IndexResult(chunks.size(), indexed);
    }

    public void deleteDocumentIndex(String docId) {
        List<KnowledgeChunk> existing = knowledgeChunkMapper.findByDocId(docId);
        if (vectorEnabled) {
            vectorStore.deleteChunks(existing.stream().map(KnowledgeChunk::getChunkId).toList());
        }
        knowledgeChunkMapper.deleteByDocId(docId);
    }

    public VectorStatusResponse status() {
        VectorStatusResponse response = new VectorStatusResponse();
        response.setEnabled(vectorEnabled);
        response.setCollection(vectorStore.getCollectionName());
        LlmConfig config = embeddingConfigResolver.resolveNullable();
        response.setEmbeddingConfigured(config != null && config.getApiKey() != null && !config.getApiKey().isBlank());
        response.setEmbeddingModel(config == null ? null : config.getModelName());
        if (!vectorEnabled) {
            response.setMessage("Vector search is disabled");
            response.setIndexedCount(-1L);
            return response;
        }
        boolean collectionReady = vectorStore.collectionReady();
        response.setCollectionReady(collectionReady);
        response.setQdrantReachable(collectionReady);
        response.setIndexedCount(collectionReady ? vectorStore.indexedCount() : 0L);
        if (!response.isEmbeddingConfigured()) {
            response.setMessage("Embedding API Key is not configured");
        } else if (!collectionReady) {
            response.setMessage("Qdrant collection is not initialized; rebuild the index first");
        } else {
            response.setMessage("Vector search is ready");
        }
        return response;
    }

    private String toEmbeddingText(KnowledgeDocument doc, KnowledgeChunk chunk) {
        return doc.getTitle()
                + "\n" + (chunk.getSectionPath() == null ? "" : chunk.getSectionPath())
                + "\n" + chunk.getChunkContent()
                + "\nTags: " + (doc.getTags() == null ? "" : doc.getTags())
                + "\nCountry: " + (doc.getCountry() == null ? "" : doc.getCountry())
                + "\nBusinessLine: " + (doc.getBusinessLine() == null ? "" : doc.getBusinessLine())
                + "\nSystem: " + (doc.getSystemName() == null ? "" : doc.getSystemName());
    }

    public record IndexResult(int chunkCount, int indexedChunks) {
    }
}
