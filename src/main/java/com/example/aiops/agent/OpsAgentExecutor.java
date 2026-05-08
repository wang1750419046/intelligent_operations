package com.example.aiops.agent;

import com.example.aiops.dto.ChatResponse;
import com.example.aiops.entity.ChatMessage;
import com.example.aiops.entity.ChatSession;
import com.example.aiops.entity.LlmConfig;
import com.example.aiops.exception.BusinessException;
import com.example.aiops.memory.DbChatMemoryStore;
import com.example.aiops.service.AgentTraceService;
import com.example.aiops.service.ModelConfigService;
import com.example.aiops.service.SessionService;
import com.example.aiops.service.impl.ChatModelFactory;
import com.example.aiops.service.impl.SessionServiceImpl;
import com.example.aiops.tools.KnowledgeSearchTool;
import com.example.aiops.tools.LogQueryTool;
import com.example.aiops.tools.MetricsQueryTool;
import com.example.aiops.util.TraceIdHolder;
import dev.ai4j.openai4j.OpenAiHttpException;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.service.AiServices;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.UUID;

@Component
public class OpsAgentExecutor implements AgentExecutor {

    private final SessionService sessionService;
    private final AgentTraceService agentTraceService;
    private final ModelConfigService modelConfigService;
    private final ChatModelFactory chatModelFactory;
    private final DbChatMemoryStore dbChatMemoryStore;
    private final LogQueryTool logQueryTool;
    private final MetricsQueryTool metricsQueryTool;
    private final KnowledgeSearchTool knowledgeSearchTool;
    private final int maxHistoryMessages;

    public OpsAgentExecutor(SessionService sessionService,
                            AgentTraceService agentTraceService,
                            ModelConfigService modelConfigService,
                            ChatModelFactory chatModelFactory,
                            DbChatMemoryStore dbChatMemoryStore,
                            LogQueryTool logQueryTool,
                            MetricsQueryTool metricsQueryTool,
                            KnowledgeSearchTool knowledgeSearchTool,
                            @Value("${aiops.memory.max-history-messages:10}") int maxHistoryMessages) {
        this.sessionService = sessionService;
        this.agentTraceService = agentTraceService;
        this.modelConfigService = modelConfigService;
        this.chatModelFactory = chatModelFactory;
        this.dbChatMemoryStore = dbChatMemoryStore;
        this.logQueryTool = logQueryTool;
        this.metricsQueryTool = metricsQueryTool;
        this.knowledgeSearchTool = knowledgeSearchTool;
        this.maxHistoryMessages = maxHistoryMessages;
    }

    @Override
    public AgentExecutionResult execute(String sessionId, String userInput, Long modelConfigId) {
        ChatSession session = sessionService.getSession(sessionId);
        LlmConfig llmConfig = modelConfigService.resolveActiveConfig(modelConfigId != null ? modelConfigId : session.getModelConfigId());
        if (llmConfig.getApiKey() == null || llmConfig.getApiKey().isBlank()) {
            throw new BusinessException(40022, "selected model config has no api key");
        }

        String traceId = TraceIdHolder.getTraceId();
        AgentRunContext context = new AgentRunContext(sessionId, traceId);
        AgentRunContextHolder.set(context);
        try {
            ChatLanguageModel model = chatModelFactory.create(llmConfig);
            OpsAgentAssistant assistant = AiServices.builder(OpsAgentAssistant.class)
                    .chatLanguageModel(model)
                    .chatMemoryProvider(memoryId -> MessageWindowChatMemory.builder()
                            .id(memoryId)
                            .maxMessages(maxHistoryMessages)
                            .chatMemoryStore(dbChatMemoryStore)
                            .build())
                    .tools(logQueryTool, metricsQueryTool, knowledgeSearchTool)
                    .build();

            String answer;
            try {
                answer = assistant.analyze(sessionId, userInput);
            } catch (RuntimeException ex) {
                throw normalizeLlmException(ex, llmConfig);
            }
            if (answer == null || answer.isBlank()) {
                throw new BusinessException(50002, "llm returned empty result");
            }

            agentTraceService.record(traceId, sessionId, context.nextStepNo(), "LLM produced final answer", "final_answer", "{}", answer);

            ChatMessage assistantMessage = new ChatMessage();
            assistantMessage.setMessageId("msg_" + UUID.randomUUID().toString().replace("-", ""));
            assistantMessage.setSessionId(sessionId);
            assistantMessage.setScope(SessionServiceImpl.UI_SCOPE);
            assistantMessage.setRole("assistant");
            assistantMessage.setContent(answer);
            assistantMessage.setCreatedAt(LocalDateTime.now());
            sessionService.appendMessage(assistantMessage);

            ChatResponse response = new ChatResponse(answer, context.getUsedTools(), context.getReferences());
            return new AgentExecutionResult(traceId, response);
        } finally {
            AgentRunContextHolder.clear();
        }
    }

    private RuntimeException normalizeLlmException(RuntimeException ex, LlmConfig config) {
        Throwable root = rootCause(ex);
        String message = root.getMessage();
        if (message != null && message.contains("AllocationQuota.FreeTierOnly")) {
            return new BusinessException(40023, "Qwen 模型免费额度已用完，当前 API Key 开启了仅使用免费额度模式。请到 DashScope/阿里云百炼控制台关闭“仅使用免费额度”或开通付费额度，也可以切换到其他有额度的模型配置。");
        }
        if (message != null && message.contains("insufficient_quota")) {
            return new BusinessException(40024, "模型调用额度不足，请检查当前模型供应商账号余额、配额或更换有额度的 API Key。");
        }
        if (root instanceof OpenAiHttpException httpException) {
            return new BusinessException(40025, describeOpenAiHttpError(httpException, config));
        }
        return ex;
    }

    private Throwable rootCause(Throwable ex) {
        Throwable current = ex;
        while (current.getCause() != null && current.getCause() != current) {
            current = current.getCause();
        }
        return current;
    }

    private String describeOpenAiHttpError(OpenAiHttpException ex, LlmConfig config) {
        String target = "当前配置：" + config.getProvider() + " / " + config.getModelName() + " / " + config.getBaseUrl();
        return switch (ex.code()) {
            case 400 -> "模型请求被供应商拒绝（HTTP 400）。常见原因：模型名不支持 OpenAI Chat/工具调用，或请求参数不兼容。建议切换到支持 function calling 的聊天模型，例如 Qwen 的 qwen-plus。"
                    + targetSuffix(target);
            case 401 -> "模型调用认证失败（HTTP 401）。请检查当前模型配置的 API Key 是否正确、是否过期。"
                    + targetSuffix(target);
            case 403 -> "模型调用无权限（HTTP 403）。请检查账号是否开通该模型、API Key 权限、地域或付费状态。"
                    + targetSuffix(target);
            case 404 -> "模型接口或模型名称不存在（HTTP 404）。请检查 Base URL 和模型名是否匹配供应商的 OpenAI 兼容接口。"
                    + targetSuffix(target);
            case 429 -> "模型调用被限流或额度不足（HTTP 429）。请检查供应商额度、并发限制或稍后重试。"
                    + targetSuffix(target);
            default -> {
                if (ex.code() >= 500) {
                    yield "模型供应商服务异常（HTTP " + ex.code() + "），请稍后重试或切换模型配置。"
                            + targetSuffix(target);
                }
                yield "模型调用失败（HTTP " + ex.code() + "）。请检查模型配置、网络和供应商控制台错误详情。"
                        + targetSuffix(target);
            }
        };
    }

    private String targetSuffix(String target) {
        return " " + target;
    }
}
