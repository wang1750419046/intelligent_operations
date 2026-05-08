package com.example.aiops.dto;

public class KnowledgeReindexResponse {

    private int successCount;
    private int failureCount;
    private int skippedCount;

    public KnowledgeReindexResponse() {
    }

    public KnowledgeReindexResponse(int successCount, int failureCount, int skippedCount) {
        this.successCount = successCount;
        this.failureCount = failureCount;
        this.skippedCount = skippedCount;
    }

    public int getSuccessCount() {
        return successCount;
    }

    public void setSuccessCount(int successCount) {
        this.successCount = successCount;
    }

    public int getFailureCount() {
        return failureCount;
    }

    public void setFailureCount(int failureCount) {
        this.failureCount = failureCount;
    }

    public int getSkippedCount() {
        return skippedCount;
    }

    public void setSkippedCount(int skippedCount) {
        this.skippedCount = skippedCount;
    }
}
