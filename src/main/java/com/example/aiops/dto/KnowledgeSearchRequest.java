package com.example.aiops.dto;

import jakarta.validation.constraints.NotBlank;

public class KnowledgeSearchRequest {

    @NotBlank
    private String query;

    public String getQuery() {
        return query;
    }

    public void setQuery(String query) {
        this.query = query;
    }
}
