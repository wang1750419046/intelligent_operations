package com.example.aiops.dto;

import jakarta.validation.constraints.NotBlank;

public class SessionCreateRequest {

    @NotBlank
    private String title;

    private Long modelConfigId;

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
}
