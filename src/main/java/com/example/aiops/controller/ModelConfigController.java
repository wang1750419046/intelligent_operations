package com.example.aiops.controller;

import com.example.aiops.dto.ModelConfigRequest;
import com.example.aiops.dto.ModelConfigResponse;
import com.example.aiops.dto.ModelConfigTestResponse;
import com.example.aiops.dto.UnifiedResponse;
import com.example.aiops.service.ModelConfigService;
import com.example.aiops.util.TraceIdHolder;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/model-configs")
public class ModelConfigController {

    private final ModelConfigService modelConfigService;

    public ModelConfigController(ModelConfigService modelConfigService) {
        this.modelConfigService = modelConfigService;
    }

    @GetMapping
    public UnifiedResponse<List<ModelConfigResponse>> list(@RequestParam(required = false) String configType) {
        return UnifiedResponse.success(modelConfigService.listAll(configType), TraceIdHolder.getTraceId());
    }

    @GetMapping("/{id}")
    public UnifiedResponse<ModelConfigResponse> get(@PathVariable Long id) {
        return UnifiedResponse.success(modelConfigService.getById(id), TraceIdHolder.getTraceId());
    }

    @PostMapping
    public UnifiedResponse<ModelConfigResponse> create(@Valid @RequestBody ModelConfigRequest request) {
        return UnifiedResponse.success(modelConfigService.create(request), TraceIdHolder.getTraceId());
    }

    @PutMapping("/{id}")
    public UnifiedResponse<ModelConfigResponse> update(@PathVariable Long id, @Valid @RequestBody ModelConfigRequest request) {
        return UnifiedResponse.success(modelConfigService.update(id, request), TraceIdHolder.getTraceId());
    }

    @DeleteMapping("/{id}")
    public UnifiedResponse<Void> delete(@PathVariable Long id) {
        modelConfigService.delete(id);
        return UnifiedResponse.success(null, TraceIdHolder.getTraceId());
    }

    @PostMapping("/{id}/test")
    public UnifiedResponse<ModelConfigTestResponse> test(@PathVariable Long id) {
        return UnifiedResponse.success(modelConfigService.test(id), TraceIdHolder.getTraceId());
    }
}
