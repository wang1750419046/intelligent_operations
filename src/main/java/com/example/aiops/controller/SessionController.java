package com.example.aiops.controller;

import com.example.aiops.dto.SessionCreateRequest;
import com.example.aiops.dto.SessionDetailResponse;
import com.example.aiops.dto.SessionInfoResponse;
import com.example.aiops.dto.UnifiedResponse;
import com.example.aiops.entity.ChatSession;
import com.example.aiops.service.AgentTraceService;
import com.example.aiops.service.SessionService;
import com.example.aiops.util.TimeUtils;
import com.example.aiops.util.TraceIdHolder;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/session")
public class SessionController {

    private final SessionService sessionService;
    private final AgentTraceService agentTraceService;

    public SessionController(SessionService sessionService, AgentTraceService agentTraceService) {
        this.sessionService = sessionService;
        this.agentTraceService = agentTraceService;
    }

    @PostMapping("/create")
    public UnifiedResponse<SessionInfoResponse> create(@Valid @RequestBody SessionCreateRequest request) {
        ChatSession session = sessionService.createSession(request.getTitle(), request.getModelConfigId());
        SessionInfoResponse response = new SessionInfoResponse(
                session.getSessionId(),
                session.getTitle(),
                session.getModelConfigId(),
                TimeUtils.format(session.getUpdatedAt()));
        return UnifiedResponse.success(response, TraceIdHolder.getTraceId());
    }

    @GetMapping("/list")
    public UnifiedResponse<List<SessionInfoResponse>> list() {
        return UnifiedResponse.success(sessionService.listSessions(), TraceIdHolder.getTraceId());
    }

    @GetMapping("/{id}")
    public UnifiedResponse<SessionDetailResponse> detail(@PathVariable("id") String id) {
        return UnifiedResponse.success(sessionService.getSessionDetail(id), TraceIdHolder.getTraceId());
    }

    @DeleteMapping("/{id}")
    public UnifiedResponse<Void> delete(@PathVariable("id") String id) {
        sessionService.deleteSession(id);
        agentTraceService.clearBySessionId(id);
        return UnifiedResponse.success(null, TraceIdHolder.getTraceId());
    }
}
