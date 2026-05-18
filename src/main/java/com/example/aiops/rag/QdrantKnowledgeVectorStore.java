package com.example.aiops.rag;

import com.example.aiops.entity.KnowledgeDocument;
import com.example.aiops.entity.KnowledgeChunk;
import io.qdrant.client.QdrantClient;
import io.qdrant.client.QdrantGrpcClient;
import io.qdrant.client.grpc.Common.Filter;
import io.qdrant.client.grpc.Collections.Distance;
import io.qdrant.client.grpc.Collections.VectorParams;
import io.qdrant.client.grpc.Points.PointStruct;
import io.qdrant.client.grpc.Points.SearchPoints;
import io.qdrant.client.grpc.Points.ScoredPoint;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static io.qdrant.client.ConditionFactory.matchKeyword;
import static io.qdrant.client.ConditionFactory.matchKeywords;
import static io.qdrant.client.PointIdFactory.id;
import static io.qdrant.client.ValueFactory.value;
import static io.qdrant.client.VectorsFactory.vectors;

@Component
public class QdrantKnowledgeVectorStore {

    private final QdrantClient client;
    private final String collectionName;
    private final int dimensions;

    public QdrantKnowledgeVectorStore(@Value("${aiops.vector.qdrant.host:8.146.230.188}") String host,
                                      @Value("${aiops.vector.qdrant.port:6334}") int port,
                                      @Value("${aiops.vector.qdrant.collection:aiops_knowledge_v1}") String collectionName,
                                      @Value("${aiops.vector.embedding.dimensions:1024}") int dimensions) {
        this.client = new QdrantClient(QdrantGrpcClient.newBuilder(host, port, false, false).build());
        this.collectionName = collectionName;
        this.dimensions = dimensions;
    }

    public String getCollectionName() {
        return collectionName;
    }

    public void ensureCollection() {
        try {
            if (Boolean.TRUE.equals(client.collectionExistsAsync(collectionName).get())) {
                return;
            }
            VectorParams params = VectorParams.newBuilder()
                    .setSize(dimensions)
                    .setDistance(Distance.Cosine)
                    .build();
            client.createCollectionAsync(collectionName, params).get();
        } catch (Exception ex) {
            throw new IllegalStateException("Qdrant collection init failed: " + ex.getMessage(), ex);
        }
    }

    public void upsert(KnowledgeDocument doc, List<Float> vector, String embeddingModel) {
        ensureCollection();
        PointStruct point = PointStruct.newBuilder()
                .setId(id(stableUuid(doc.getDocId())))
                .setVectors(vectors(vector))
                .putPayload("docId", value(doc.getDocId()))
                .putPayload("title", value(doc.getTitle()))
                .putPayload("source", value(doc.getSource()))
                .putPayload("tags", value(doc.getTags() == null ? "" : doc.getTags()))
                .putPayload("embeddingModel", value(embeddingModel))
                .putPayload("embeddingDimensions", value(dimensions))
                .putPayload("indexedAt", value(Instant.now().toString()))
                .build();
        try {
            client.upsertAsync(collectionName, List.of(point)).get();
        } catch (Exception ex) {
            throw new IllegalStateException("Qdrant upsert failed: " + ex.getMessage(), ex);
        }
    }

    public void upsertChunk(KnowledgeDocument doc, KnowledgeChunk chunk, List<Float> vector, String embeddingModel) {
        ensureCollection();
        PointStruct point = PointStruct.newBuilder()
                .setId(id(stableUuid(chunk.getChunkId())))
                .setVectors(vectors(vector))
                .putPayload("indexType", value("chunk"))
                .putPayload("chunkId", value(chunk.getChunkId()))
                .putPayload("docId", value(doc.getDocId()))
                .putPayload("title", value(doc.getTitle()))
                .putPayload("source", value(doc.getSource()))
                .putPayload("sectionPath", value(chunk.getSectionPath() == null ? "" : chunk.getSectionPath()))
                .putPayload("tags", value(doc.getTags() == null ? "" : doc.getTags()))
                .putPayload("country", value(doc.getCountry() == null ? "" : doc.getCountry()))
                .putPayload("businessLine", value(doc.getBusinessLine() == null ? "" : doc.getBusinessLine()))
                .putPayload("systemName", value(doc.getSystemName() == null ? "" : doc.getSystemName()))
                .putPayload("eventTime", value(doc.getEventTime() == null ? "" : doc.getEventTime().toString()))
                .putPayload("permissionCodes", value(toPayloadList(doc.getPermissionCodes())))
                .putPayload("embeddingModel", value(embeddingModel))
                .putPayload("embeddingDimensions", value(dimensions))
                .putPayload("indexedAt", value(Instant.now().toString()))
                .build();
        try {
            client.upsertAsync(collectionName, List.of(point)).get();
        } catch (Exception ex) {
            throw new IllegalStateException("Qdrant chunk upsert failed: " + ex.getMessage(), ex);
        }
    }

    public List<VectorSearchHit> search(List<Float> vector, int limit) {
        return search(vector, limit, null);
    }

    public List<VectorSearchHit> search(List<Float> vector, int limit, KnowledgeSearchCriteria criteria) {
        return search(vector, limit, criteria, 0L);
    }

    public List<VectorSearchHit> search(List<Float> vector, int limit, KnowledgeSearchCriteria criteria, long timeoutMs) {
        if (timeoutMs > 0) {
            ensureCollection(timeoutMs);
        } else {
            ensureCollection();
        }
        SearchPoints.Builder builder = SearchPoints.newBuilder()
                .setCollectionName(collectionName)
                .addAllVector(vector)
                .setLimit(limit);
        Filter filter = buildFilter(criteria);
        if (filter != null) {
            builder.setFilter(filter);
        }
        SearchPoints request = builder.build();
        try {
            List<ScoredPoint> points = timeoutMs > 0
                    ? client.searchAsync(request).get(timeoutMs, TimeUnit.MILLISECONDS)
                    : client.searchAsync(request).get();
            List<VectorSearchHit> hits = new ArrayList<>();
            for (ScoredPoint point : points) {
                if (point.getPayloadMap().containsKey("docId")) {
                    String docId = point.getPayloadMap().get("docId").getStringValue();
                    String chunkId = point.getPayloadMap().containsKey("chunkId")
                            ? point.getPayloadMap().get("chunkId").getStringValue()
                            : null;
                    hits.add(new VectorSearchHit(chunkId, docId, point.getScore()));
                }
            }
            return hits;
        } catch (Exception ex) {
            throw new IllegalStateException("Qdrant search failed: " + ex.getMessage(), ex);
        }
    }

    private void ensureCollection(long timeoutMs) {
        try {
            if (Boolean.TRUE.equals(client.collectionExistsAsync(collectionName).get(timeoutMs, TimeUnit.MILLISECONDS))) {
                return;
            }
            VectorParams params = VectorParams.newBuilder()
                    .setSize(dimensions)
                    .setDistance(Distance.Cosine)
                    .build();
            client.createCollectionAsync(collectionName, params).get(timeoutMs, TimeUnit.MILLISECONDS);
        } catch (Exception ex) {
            throw new IllegalStateException("Qdrant collection init failed: " + ex.getMessage(), ex);
        }
    }

    public void deleteChunks(List<String> chunkIds) {
        if (chunkIds == null || chunkIds.isEmpty() || !collectionReady()) {
            return;
        }
        try {
            client.deleteAsync(collectionName, chunkIds.stream()
                    .map(this::stableUuid)
                    .map(uuid -> id(uuid))
                    .toList()).get();
        } catch (Exception ex) {
            throw new IllegalStateException("Qdrant delete chunks failed: " + ex.getMessage(), ex);
        }
    }

    public boolean collectionReady() {
        try {
            return Boolean.TRUE.equals(client.collectionExistsAsync(collectionName).get());
        } catch (Exception ex) {
            return false;
        }
    }

    public long indexedCount() {
        try {
            if (!Boolean.TRUE.equals(client.collectionExistsAsync(collectionName).get())) {
                return 0L;
            }
            return client.countAsync(collectionName).get();
        } catch (Exception ex) {
            return -1L;
        }
    }

    private UUID stableUuid(String docId) {
        return UUID.nameUUIDFromBytes(docId.getBytes(StandardCharsets.UTF_8));
    }

    private List<io.qdrant.client.grpc.JsonWithInt.Value> toPayloadList(String commaSeparated) {
        List<io.qdrant.client.grpc.JsonWithInt.Value> values = new ArrayList<>();
        if (commaSeparated != null) {
            for (String code : commaSeparated.split(",")) {
                if (!code.isBlank()) {
                    values.add(value(code.trim()));
                }
            }
        }
        if (values.isEmpty()) {
            values.add(value(KnowledgeSearchCriteria.PUBLIC_PERMISSION));
        }
        return values;
    }

    private Filter buildFilter(KnowledgeSearchCriteria criteria) {
        Filter.Builder filter = Filter.newBuilder();
        filter.addMust(matchKeyword("indexType", "chunk"));
        if (criteria != null) {
            if (criteria.getCountry() != null) {
                filter.addMust(matchKeyword("country", criteria.getCountry()));
            }
            if (criteria.getBusinessLine() != null) {
                filter.addMust(matchKeyword("businessLine", criteria.getBusinessLine()));
            }
            if (criteria.getSystemName() != null) {
                filter.addMust(matchKeyword("systemName", criteria.getSystemName()));
            }
            filter.addMust(matchKeywords("permissionCodes", criteria.effectivePermissionCodes()));
        }
        return filter.build();
    }
}
