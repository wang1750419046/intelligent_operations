package com.example.aiops.controller;

import com.example.aiops.dto.TraceDetailResponse;
import com.example.aiops.dto.UnifiedResponse;
import com.example.aiops.service.AgentTraceService;
import com.example.aiops.util.TraceIdHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/trace")
public class TraceController {

    private final AgentTraceService agentTraceService;

    public TraceController(AgentTraceService agentTraceService) {
        this.agentTraceService = agentTraceService;
    }

    @GetMapping("/{traceId}")
    public UnifiedResponse<TraceDetailResponse> getTrace(@PathVariable String traceId) {
        return UnifiedResponse.success(agentTraceService.getTrace(traceId), TraceIdHolder.getTraceId());
    }
}
