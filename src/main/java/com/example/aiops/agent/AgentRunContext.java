package com.example.aiops.agent;

import com.example.aiops.dto.ReferenceItem;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

public class AgentRunContext {

    private final String sessionId;
    private final String traceId;
    private final long startedAtNanos = System.nanoTime();
    private final AtomicInteger stepCounter = new AtomicInteger(1);
    private final AtomicReference<Long> firstTokenMs = new AtomicReference<>();
    private final Set<String> usedTools = new LinkedHashSet<>();
    private final List<ReferenceItem> references = new ArrayList<>();
    private Consumer<AgentStatusUpdate> statusConsumer;

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

    public void setStatusConsumer(Consumer<AgentStatusUpdate> statusConsumer) {
        this.statusConsumer = statusConsumer;
    }

    public void publishStatus(String stage, String message) {
        publishStatus(stage, message, null);
    }

    public void publishStatus(String stage, String message, Long elapsedMs) {
        if (statusConsumer != null) {
            statusConsumer.accept(new AgentStatusUpdate(stage, message, elapsedMs));
        }
    }

    public long elapsedMs() {
        return (System.nanoTime() - startedAtNanos) / 1_000_000L;
    }

    public long markFirstToken() {
        long elapsed = elapsedMs();
        firstTokenMs.compareAndSet(null, elapsed);
        return firstTokenMs.get();
    }

    public Long getFirstTokenMs() {
        return firstTokenMs.get();
    }

    public List<String> getUsedTools() {
        return new ArrayList<>(usedTools);
    }

    public List<ReferenceItem> getReferences() {
        return new ArrayList<>(references);
    }
}
