package com.example.aiops.tools;

import com.example.aiops.dto.ReferenceItem;
import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class MetricsQueryTool {

    private final ToolCallbackSupport toolCallbackSupport;

    public MetricsQueryTool(ToolCallbackSupport toolCallbackSupport) {
        this.toolCallbackSupport = toolCallbackSupport;
    }

    @Tool("查询 CPU、内存、QPS、RT、错误率等指标并输出异常趋势摘要")
    public String queryMetrics(
            @P("serviceName") String serviceName,
            @P("timeRange") String timeRange,
            @P("metricName") String metricName
    ) {
        long startNanos = System.nanoTime();
        toolCallbackSupport.started("query_metrics");
        String params = "{serviceName=%s, timeRange=%s, metricName=%s}".formatted(serviceName, timeRange, metricName);
        try {
            String summary = "服务 " + safe(serviceName, "order-service") + " 在 " + safe(timeRange, "最近 1 小时")
                    + " 的指标显示 RT 从 180ms 升至 1.8s，错误率峰值 8.7%，CPU 中等波动，说明瓶颈更像数据库或下游等待，而不是纯计算资源不足。查询指标: "
                    + safe(metricName, "rt,errorRate,cpu,memory") + "。";
            List<ReferenceItem> references = List.of(
                    new ReferenceItem("metric", "指标趋势摘要", summary, "mock-metrics-store")
            );
            toolCallbackSupport.success("query_metrics", params, summary, references);
            toolCallbackSupport.finished("query_metrics", toolCallbackSupport.elapsedMs(startNanos));
            return summary;
        } catch (Exception ex) {
            toolCallbackSupport.finished("query_metrics", toolCallbackSupport.elapsedMs(startNanos));
            return toolCallbackSupport.failure("query_metrics", params, ex);
        }
    }

    private String safe(String value, String defaultValue) {
        return value == null || value.isBlank() ? defaultValue : value;
    }
}
