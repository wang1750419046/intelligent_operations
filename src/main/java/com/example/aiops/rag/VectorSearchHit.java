package com.example.aiops.rag;

public class VectorSearchHit {

    private final String chunkId;
    private final String docId;
    private final double score;

    public VectorSearchHit(String docId, double score) {
        this(null, docId, score);
    }

    public VectorSearchHit(String chunkId, String docId, double score) {
        this.chunkId = chunkId;
        this.docId = docId;
        this.score = score;
    }

    public String getChunkId() {
        return chunkId;
    }

    public String getDocId() {
        return docId;
    }

    public double getScore() {
        return score;
    }
}
