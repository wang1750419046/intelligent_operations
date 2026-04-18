package com.example.aiops.tools;

import com.example.aiops.agent.AgentRunContext;
import com.example.aiops.agent.AgentRunContextHolder;
import com.example.aiops.dto.ReferenceItem;
import com.example.aiops.service.AgentTraceService;
import org.springframework.stereotype.Component;

import java.util.List;

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
}
