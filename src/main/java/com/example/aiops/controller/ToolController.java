package com.example.aiops.controller;

import com.example.aiops.dto.ToolInfoResponse;
import com.example.aiops.dto.UnifiedResponse;
import com.example.aiops.tools.ToolRegistry;
import com.example.aiops.util.TraceIdHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/tools")
public class ToolController {

    private final ToolRegistry toolRegistry;

    public ToolController(ToolRegistry toolRegistry) {
        this.toolRegistry = toolRegistry;
    }

    @GetMapping
    public UnifiedResponse<List<ToolInfoResponse>> list() {
        return UnifiedResponse.success(toolRegistry.listTools(), TraceIdHolder.getTraceId());
    }
}
