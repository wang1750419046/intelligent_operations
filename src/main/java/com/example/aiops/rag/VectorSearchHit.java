package com.example.aiops.rag;

public class VectorSearchHit {

    private final String docId;
    private final double score;

    public VectorSearchHit(String docId, double score) {
        this.docId = docId;
        this.score = score;
    }

    public String getDocId() {
        return docId;
    }

    public double getScore() {
        return score;
    }
}
