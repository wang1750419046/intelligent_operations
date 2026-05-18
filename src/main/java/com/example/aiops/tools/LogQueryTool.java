package com.example.aiops.tools;

import com.example.aiops.dto.ReferenceItem;
import com.example.aiops.exception.BusinessException;
import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class LogQueryTool {

    private final ToolCallbackSupport toolCallbackSupport;

    public LogQueryTool(ToolCallbackSupport toolCallbackSupport) {
        this.toolCallbackSupport = toolCallbackSupport;
    }

    @Tool("查询日志并输出异常摘要、异常模式和关键日志片段")
    public String queryLogs(
            @P("serviceName") String serviceName,
            @P("timeRange") String timeRange,
            @P("keyword") String keyword
    ) {
        long startNanos = System.nanoTime();
        toolCallbackSupport.started("query_logs");
        String params = "{serviceName=%s, timeRange=%s, keyword=%s}".formatted(serviceName, timeRange, keyword);
        try {
            if ("fail".equalsIgnoreCase(keyword)) {
                throw new BusinessException(50001, "tool timeout");
            }
            String summary = "服务 " + safe(serviceName, "order-service") + " 在 " + safe(timeRange, "最近 1 小时")
                    + " 出现数据库连接超时和少量线程池排队，错误峰值集中在 02:17-02:32，异常模式以 connection timeout 为主。";
            List<ReferenceItem> references = List.of(
                    new ReferenceItem("log", "应用日志摘要", summary, "mock-log-store"),
                    new ReferenceItem("log", "关键日志片段", "[ERROR] HikariPool timeout after 30000ms", "mock-log-store")
            );
            toolCallbackSupport.success("query_logs", params, summary, references);
            toolCallbackSupport.finished("query_logs", toolCallbackSupport.elapsedMs(startNanos));
            return summary;
        } catch (Exception ex) {
            toolCallbackSupport.finished("query_logs", toolCallbackSupport.elapsedMs(startNanos));
            return toolCallbackSupport.failure("query_logs", params, ex);
        }
    }

    private String safe(String value, String defaultValue) {
        return value == null || value.isBlank() ? defaultValue : value;
    }
}
