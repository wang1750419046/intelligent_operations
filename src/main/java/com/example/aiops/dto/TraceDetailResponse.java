package com.example.aiops.dto;

import java.util.List;

public class TraceDetailResponse {

    private String traceId;
    private String sessionId;
    private List<TraceStepResponse> steps;

    public TraceDetailResponse() {
    }

    public TraceDetailResponse(String traceId, String sessionId, List<TraceStepResponse> steps) {
        this.traceId = traceId;
        this.sessionId = sessionId;
        this.steps = steps;
    }

    public String getTraceId() {
        return traceId;
    }

    public void setTraceId(String traceId) {
        this.traceId = traceId;
    }

    public String getSessionId() {
        return sessionId;
    }

    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
    }

    public List<TraceStepResponse> getSteps() {
        return steps;
    }

    public void setSteps(List<TraceStepResponse> steps) {
        this.steps = steps;
    }
}
