package com.example.aiops.dto;

import com.example.aiops.entity.ChatMessage;

import java.util.List;

public class SessionDetailResponse {

    private String sessionId;
    private String title;
    private Long modelConfigId;
    private List<ChatMessage> messages;

    public SessionDetailResponse() {
    }

    public SessionDetailResponse(String sessionId, String title, Long modelConfigId, List<ChatMessage> messages) {
        this.sessionId = sessionId;
        this.title = title;
        this.modelConfigId = modelConfigId;
        this.messages = messages;
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

    public List<ChatMessage> getMessages() {
        return messages;
    }

    public void setMessages(List<ChatMessage> messages) {
        this.messages = messages;
    }
}
