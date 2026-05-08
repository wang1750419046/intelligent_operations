package com.example.aiops.dto;

public class VectorStatusResponse {

    private boolean enabled;
    private boolean embeddingConfigured;
    private boolean qdrantReachable;
    private boolean collectionReady;
    private long indexedCount;
    private String collection;
    private String embeddingModel;
    private String message;

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public boolean isEmbeddingConfigured() {
        return embeddingConfigured;
    }

    public void setEmbeddingConfigured(boolean embeddingConfigured) {
        this.embeddingConfigured = embeddingConfigured;
    }

    public boolean isQdrantReachable() {
        return qdrantReachable;
    }

    public void setQdrantReachable(boolean qdrantReachable) {
        this.qdrantReachable = qdrantReachable;
    }

    public boolean isCollectionReady() {
        return collectionReady;
    }

    public void setCollectionReady(boolean collectionReady) {
        this.collectionReady = collectionReady;
    }

    public long getIndexedCount() {
        return indexedCount;
    }

    public void setIndexedCount(long indexedCount) {
        this.indexedCount = indexedCount;
    }

    public String getCollection() {
        return collection;
    }

    public void setCollection(String collection) {
        this.collection = collection;
    }

    public String getEmbeddingModel() {
        return embeddingModel;
    }

    public void setEmbeddingModel(String embeddingModel) {
        this.embeddingModel = embeddingModel;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}
