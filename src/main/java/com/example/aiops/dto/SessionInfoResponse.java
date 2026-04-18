package com.example.aiops.dto;

public class SessionInfoResponse {

    private String sessionId;
    private String title;
    private Long modelConfigId;
    private String updatedAt;

    public SessionInfoResponse() {
    }

    public SessionInfoResponse(String sessionId, String title, Long modelConfigId, String updatedAt) {
        this.sessionId = sessionId;
        this.title = title;
        this.modelConfigId = modelConfigId;
        this.updatedAt = updatedAt;
    }

    public String getSessionId() {
        return sessionId;
    }

    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public Long getModelConfigId() {
        return modelConfigId;
    }

    public void setModelConfigId(Long modelConfigId) {
        this.modelConfigId = modelConfigId;
    }

    public String getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(String updatedAt) {
        this.updatedAt = updatedAt;
    }
}
