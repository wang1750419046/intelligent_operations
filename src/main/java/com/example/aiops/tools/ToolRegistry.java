package com.example.aiops.tools;

import com.example.aiops.dto.ToolInfoResponse;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class ToolRegistry {

    public List<ToolInfoResponse> listTools() {
        return List.of(
                new ToolInfoResponse("query_logs", "查询日志并输出异常摘要、异常模式和关键日志片段"),
                new ToolInfoResponse("query_metrics", "查询 CPU、内存、QPS、RT、错误率等指标并输出异常趋势摘要"),
                new ToolInfoResponse("search_knowledge", "检索历史故障案例和知识库条目")
        );
    }
}
