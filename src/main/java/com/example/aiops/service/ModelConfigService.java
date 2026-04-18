package com.example.aiops.service;

import com.example.aiops.dto.ModelConfigRequest;
import com.example.aiops.dto.ModelConfigResponse;
import com.example.aiops.dto.ModelConfigTestResponse;
import com.example.aiops.entity.LlmConfig;

import java.util.List;

public interface ModelConfigService {

    List<ModelConfigResponse> listAll();

    ModelConfigResponse getById(Long id);

    ModelConfigResponse create(ModelConfigRequest request);

    ModelConfigResponse update(Long id, ModelConfigRequest request);

    void delete(Long id);

    ModelConfigTestResponse test(Long id);

    LlmConfig resolveActiveConfig(Long requestedId);
}
