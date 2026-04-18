package com.example.aiops.service.impl;

import com.example.aiops.dto.TraceDetailResponse;
import com.example.aiops.dto.TraceStepResponse;
import com.example.aiops.entity.AgentTraceRecord;
import com.example.aiops.mapper.AgentTraceMapper;
import com.example.aiops.service.AgentTraceService;
import com.example.aiops.util.TimeUtils;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class AgentTraceServiceImpl implements AgentTraceService {

    private final AgentTraceMapper mapper;

    public AgentTraceServiceImpl(AgentTraceMapper mapper) {
        this.mapper = mapper;
    }

    @Override
    public void record(String traceId, String sessionId, int stepNo, String thoughtSummary, String actionName, String actionParams, String observation) {
        AgentTraceRecord record = new AgentTraceRecord();
        record.setTraceId(traceId);
        record.setSessionId(sessionId);
        record.setStepNo(stepNo);
        record.setThoughtSummary(thoughtSummary);
        record.setActionName(actionName);
        record.setActionParams(actionParams);
        record.setObservation(observation);
        record.setCreatedAt(LocalDateTime.now());
        mapper.insert(record);
    }

    @Override
    public TraceDetailResponse getTrace(String traceId) {
        List<AgentTraceRecord> records = mapper.findByTraceId(traceId);
        String sessionId = records.isEmpty() ? null : records.get(0).getSessionId();
        List<TraceStepResponse> steps = records.stream()
                .map(record -> new TraceStepResponse(
                        record.getStepNo(),
                        record.getThoughtSummary(),
                        record.getActionName(),
                        record.getActionParams(),
                        record.getObservation(),
                        TimeUtils.format(record.getCreatedAt())))
                .toList();
        return new TraceDetailResponse(traceId, sessionId, steps);
    }

    @Override
    public void clearBySessionId(String sessionId) {
        mapper.deleteBySessionId(sessionId);
    }
}
