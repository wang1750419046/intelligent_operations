package com.example.aiops.rag;

import com.example.aiops.entity.KnowledgeDocument;
import io.qdrant.client.QdrantClient;
import io.qdrant.client.QdrantGrpcClient;
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

    public List<VectorSearchHit> search(List<Float> vector, int limit) {
        ensureCollection();
        SearchPoints request = SearchPoints.newBuilder()
                .setCollectionName(collectionName)
                .addAllVector(vector)
                .setLimit(limit)
                .build();
        try {
            List<ScoredPoint> points = client.searchAsync(request).get();
            List<VectorSearchHit> hits = new ArrayList<>();
            for (ScoredPoint point : points) {
                if (point.getPayloadMap().containsKey("docId")) {
                    hits.add(new VectorSearchHit(point.getPayloadMap().get("docId").getStringValue(), point.getScore()));
                }
            }
            return hits;
        } catch (Exception ex) {
            throw new IllegalStateException("Qdrant search failed: " + ex.getMessage(), ex);
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
}
