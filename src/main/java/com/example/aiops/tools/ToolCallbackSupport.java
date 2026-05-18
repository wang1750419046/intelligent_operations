package com.example.aiops.tools;

import com.example.aiops.agent.AgentRunContext;
import com.example.aiops.agent.AgentRunContextHolder;
import com.example.aiops.dto.ReferenceItem;
import com.example.aiops.service.AgentTraceService;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Locale;

@Component
public class ToolCallbackSupport {

    private final AgentTraceService agentTraceService;

    public ToolCallbackSupport(AgentTraceService agentTraceService) {
        this.agentTraceService = agentTraceService;
    }

    public void success(String toolName, String actionParams, String summary, List<ReferenceItem> references) {
        AgentRunContext context = AgentRunContextHolder.get();
        if (context == null) {
            return;
        }
        context.addUsedTool(toolName);
        context.addReferences(references);
        agentTraceService.record(
                context.getTraceId(),
                context.getSessionId(),
                context.nextStepNo(),
                "LLM requested tool execution",
                toolName,
                actionParams,
                summary
        );
    }

    public void started(String toolName) {
        AgentRunContext context = AgentRunContextHolder.get();
        if (context != null) {
            context.publishStatus(stage(toolName), startMessage(toolName));
        }
    }

    public void finished(String toolName, long elapsedMs) {
        AgentRunContext context = AgentRunContextHolder.get();
        if (context != null) {
            context.publishStatus(stage(toolName), finishMessage(toolName, elapsedMs), elapsedMs);
            agentTraceService.record(
                    context.getTraceId(),
                    context.getSessionId(),
                    context.nextStepNo(),
                    "Tool timing",
                    stage(toolName),
                    "{}",
                    "elapsedMs=" + elapsedMs
            );
        }
    }

    public long elapsedMs(long startNanos) {
        return (System.nanoTime() - startNanos) / 1_000_000L;
    }

    public String failure(String toolName, String actionParams, Exception ex) {
        AgentRunContext context = AgentRunContextHolder.get();
        String fallback = "工具调用失败，已降级处理: " + ex.getMessage();
        if (context != null) {
            agentTraceService.record(
                    context.getTraceId(),
                    context.getSessionId(),
                    context.nextStepNo(),
                    "LLM requested tool execution",
                    toolName,
                    actionParams,
                    fallback
            );
        }
        return fallback;
    }

    private String stage(String toolName) {
        return switch (normalize(toolName)) {
            case "search_knowledge" -> "knowledge_search";
            case "query_logs" -> "log_search";
            case "query_metrics" -> "metrics_query";
            default -> normalize(toolName);
        };
    }

    private String startMessage(String toolName) {
        return switch (normalize(toolName)) {
            case "search_knowledge" -> "正在检索知识库";
            case "query_logs" -> "正在查询日志";
            case "query_metrics" -> "正在查询指标";
            default -> "正在调用工具";
        };
    }

    private String finishMessage(String toolName, long elapsedMs) {
        return switch (normalize(toolName)) {
            case "search_knowledge" -> "知识库检索完成 " + elapsedMs + "ms";
            case "query_logs" -> "日志查询完成 " + elapsedMs + "ms";
            case "query_metrics" -> "指标查询完成 " + elapsedMs + "ms";
            default -> "工具调用完成 " + elapsedMs + "ms";
        };
    }

    private String normalize(String toolName) {
        return toolName == null ? "" : toolName.toLowerCase(Locale.ROOT);
    }
}
