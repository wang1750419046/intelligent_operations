package com.example.aiops.rag;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public class KnowledgeSearchCriteria {

    public static final String PUBLIC_PERMISSION = "PUBLIC";

    private String query;
    private int limit = 5;
    private String country;
    private String businessLine;
    private String systemName;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private List<String> permissionCodes = List.of(PUBLIC_PERMISSION);

    public static KnowledgeSearchCriteria simple(String query, int limit) {
        KnowledgeSearchCriteria criteria = new KnowledgeSearchCriteria();
        criteria.setQuery(query);
        criteria.setLimit(limit);
        return criteria;
    }

    public List<String> effectivePermissionCodes() {
        Set<String> codes = new LinkedHashSet<>();
        codes.add(PUBLIC_PERMISSION);
        if (permissionCodes != null) {
            for (String code : permissionCodes) {
                if (code != null && !code.isBlank()) {
                    codes.add(code.trim());
                }
            }
        }
        return new ArrayList<>(codes);
    }

    public String getQuery() {
        return query;
    }

    public void setQuery(String query) {
        this.query = query;
    }

    public int getLimit() {
        return limit;
    }

    public void setLimit(int limit) {
        if (limit > 0) {
            this.limit = Math.min(limit, 20);
        }
    }

    public String getCountry() {
        return country;
    }

    public void setCountry(String country) {
        this.country = blankToNull(country);
    }

    public String getBusinessLine() {
        return businessLine;
    }

    public void setBusinessLine(String businessLine) {
        this.businessLine = blankToNull(businessLine);
    }

    public String getSystemName() {
        return systemName;
    }

    public void setSystemName(String systemName) {
        this.systemName = blankToNull(systemName);
    }

    public LocalDateTime getStartTime() {
        return startTime;
    }

    public void setStartTime(LocalDateTime startTime) {
        this.startTime = startTime;
    }

    public LocalDateTime getEndTime() {
        return endTime;
    }

    public void setEndTime(LocalDateTime endTime) {
        this.endTime = endTime;
    }

    public List<String> getPermissionCodes() {
        return permissionCodes;
    }

    public void setPermissionCodes(List<String> permissionCodes) {
        this.permissionCodes = permissionCodes == null ? List.of(PUBLIC_PERMISSION) : permissionCodes;
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
