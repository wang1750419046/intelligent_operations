package com.example.aiops.agent;

import com.example.aiops.dto.ReferenceItem;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

public class AgentRunContext {

    private final String sessionId;
    private final String traceId;
    private final AtomicInteger stepCounter = new AtomicInteger(1);
    private final Set<String> usedTools = new LinkedHashSet<>();
    private final List<ReferenceItem> references = new ArrayList<>();

    public AgentRunContext(String sessionId, String traceId) {
        this.sessionId = sessionId;
        this.traceId = traceId;
    }

    public String getSessionId() {
        return sessionId;
    }

    public String getTraceId() {
        return traceId;
    }

    public int nextStepNo() {
        return stepCounter.getAndIncrement();
    }

    public void addUsedTool(String toolName) {
        usedTools.add(toolName);
    }

    public void addReferences(List<ReferenceItem> items) {
        references.addAll(items);
    }

    public List<String> getUsedTools() {
        return new ArrayList<>(usedTools);
    }

    public List<ReferenceItem> getReferences() {
        return new ArrayList<>(references);
    }
}
