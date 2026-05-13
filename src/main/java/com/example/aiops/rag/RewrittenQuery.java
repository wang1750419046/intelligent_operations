package com.example.aiops.rag;

import java.util.List;

public class RewrittenQuery {

    private final String query;
    private final List<String> keywords;
    private final String country;
    private final String businessLine;
    private final String systemName;

    public RewrittenQuery(String query, List<String> keywords, String country, String businessLine, String systemName) {
        this.query = query;
        this.keywords = keywords == null ? List.of() : keywords;
        this.country = country;
        this.businessLine = businessLine;
        this.systemName = systemName;
    }

    public static RewrittenQuery fallback(String query) {
        return new RewrittenQuery(query, List.of(), null, null, null);
    }

    public String getQuery() {
        return query;
    }

    public List<String> getKeywords() {
        return keywords;
    }

    public String getCountry() {
        return country;
    }

    public String getBusinessLine() {
        return businessLine;
    }

    public String getSystemName() {
        return systemName;
    }
}
