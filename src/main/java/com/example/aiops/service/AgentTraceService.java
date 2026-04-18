package com.example.aiops.service;

import com.example.aiops.dto.TraceDetailResponse;

public interface AgentTraceService {

    void record(String traceId, String sessionId, int stepNo, String thoughtSummary, String actionName, String actionParams, String observation);

    TraceDetailResponse getTrace(String traceId);

    void clearBySessionId(String sessionId);
}
