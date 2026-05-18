package com.example.aiops.controller;

import com.example.aiops.agent.AgentExecutionResult;
import com.example.aiops.agent.AgentStreamHandler;
import com.example.aiops.agent.AgentStatusUpdate;
import com.example.aiops.dto.ChatRequest;
import com.example.aiops.dto.ChatResponse;
import com.example.aiops.dto.ChatStreamEvent;
import com.example.aiops.dto.UnifiedResponse;
import com.example.aiops.exception.BusinessException;
import com.example.aiops.service.ChatService;
import com.example.aiops.util.TraceIdHolder;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.core.task.TaskExecutor;
import org.springframework.http.MediaType;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

@RestController
@RequestMapping("/api/chat")
public class ChatController {

    private final ChatService chatService;
    private final TaskExecutor taskExecutor;

    public ChatController(ChatService chatService, TaskExecutor taskExecutor) {
        this.chatService = chatService;
        this.taskExecutor = taskExecutor;
    }

    @PostMapping("/send")
    public UnifiedResponse<ChatResponse> send(@Valid @RequestBody ChatRequest request) {
        AgentExecutionResult result = chatService.send(request.getSessionId(), request.getUserInput(), request.getModelConfigId());
        return UnifiedResponse.success(result.getResponse(), TraceIdHolder.getTraceId());
    }

    @PostMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter stream(@Valid @RequestBody ChatRequest request, HttpServletResponse response) {
        response.setHeader("Cache-Control", "no-cache");
        response.setHeader("X-Accel-Buffering", "no");
        SseEmitter emitter = new SseEmitter(0L);
        String requestTraceId = TraceIdHolder.getTraceId();
        AtomicReference<String> streamTraceId = new AtomicReference<>(requestTraceId);
        AtomicBoolean completed = new AtomicBoolean(false);
        emitter.onCompletion(() -> completed.set(true));
        emitter.onTimeout(() -> completed.set(true));
        emitter.onError(error -> completed.set(true));
        taskExecutor.execute(() -> {
            TraceIdHolder.setTraceId(requestTraceId);
            try {
                chatService.stream(request.getSessionId(), request.getUserInput(), request.getModelConfigId(), new AgentStreamHandler() {
                    @Override
                    public void onStart(String traceId) {
                        if (completed.get()) {
                            return;
                        }
                        streamTraceId.set(traceId);
                        emit(emitter, "trace", ChatStreamEvent.trace(traceId));
                    }

                    @Override
                    public void onStatus(AgentStatusUpdate status) {
                        if (completed.get()) {
                            return;
                        }
                        emit(emitter, "status", ChatStreamEvent.status(
                                streamTraceId.get(),
                                status.getStage(),
                                status.getMessage(),
                                status.getElapsedMs()));
                    }

                    @Override
                    public void onToken(String token) {
                        if (completed.get()) {
                            return;
                        }
                        emit(emitter, "delta", ChatStreamEvent.delta(streamTraceId.get(), token));
                    }

                    @Override
                    public void onComplete(AgentExecutionResult result) {
                        if (completed.getAndSet(true)) {
                            return;
                        }
                        emit(emitter, "done", ChatStreamEvent.done(result.getTraceId(), result.getResponse()));
                        emitter.complete();
                    }

                    @Override
                    public void onError(Throwable error) {
                        if (completed.getAndSet(true)) {
                            return;
                        }
                        emitError(emitter, streamTraceId.get(), error);
                    }
                });
            } catch (Throwable error) {
                if (completed.getAndSet(true)) {
                    return;
                }
                emitError(emitter, streamTraceId.get(), error);
            } finally {
                TraceIdHolder.clear();
            }
        });
        return emitter;
    }

    private void emitError(SseEmitter emitter, String traceId, Throwable error) {
        int code = error instanceof BusinessException businessException ? businessException.getCode() : 50000;
        String message = error.getMessage() == null ? "stream failed" : error.getMessage();
        emit(emitter, "error", ChatStreamEvent.error(traceId, code, message));
        emitter.complete();
    }

    private void emit(SseEmitter emitter, String eventName, ChatStreamEvent event) {
        try {
            synchronized (emitter) {
                emitter.send(SseEmitter.event().name(eventName).data(event));
            }
        } catch (IOException ex) {
            emitter.completeWithError(ex);
            throw new IllegalStateException(ex);
        }
    }
}
