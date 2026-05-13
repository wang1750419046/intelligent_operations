package com.example.aiops.rag;

public class RerankResult {

    private final int index;
    private final double score;

    public RerankResult(int index, double score) {
        this.index = index;
        this.score = score;
    }

    public int getIndex() {
        return index;
    }

    public double getScore() {
        return score;
    }
}
