package com.example.aiops.agent;

import com.example.aiops.entity.ChatSession;
import com.example.aiops.entity.LlmConfig;
import com.example.aiops.mapper.LlmConfigMapper;
import com.example.aiops.service.ChatService;
import com.example.aiops.service.SessionService;
import com.example.aiops.service.impl.ChatModelFactory;
import com.example.aiops.util.TraceIdHolder;
import dev.langchain4j.agent.tool.ToolExecutionRequest;
import dev.langchain4j.agent.tool.ToolSpecification;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.ToolExecutionResultMessage;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.output.Response;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
class OpsAgentExecutorTest {

    @Autowired
    private ChatService chatService;

    @Autowired
    private SessionService sessionService;

    @Autowired
    private LlmConfigMapper llmConfigMapper;

    @MockBean
    private ChatModelFactory chatModelFactory;

    @BeforeEach
    void setUp() {
        Mockito.when(chatModelFactory.create(Mockito.any())).thenReturn(new FakeToolCallingModel());
        LlmConfig config = llmConfigMapper.findById(1L);
        config.setApiKey("test-key");
        llmConfigMapper.update(config);
    }

    @Test
    void shouldExecuteToolCallingAgentFlow() {
        ChatSession session = sessionService.createSession("Agent 集成测试", 1L);
        TraceIdHolder.setTraceId("trace_test_agent_001");
        AgentExecutionResult result = chatService.send(session.getSessionId(), "昨晚订单接口响应变慢，帮我分析原因", 1L);

        assertTrue(result.getResponse().getAnswer().contains("问题描述"));
        assertTrue(result.getResponse().getAnswer().contains("根因分析"));
        assertFalse(result.getResponse().getUsedTools().isEmpty());
        assertFalse(result.getResponse().getReferences().isEmpty());
        TraceIdHolder.clear();
    }

    static class FakeToolCallingModel implements ChatLanguageModel {

        @Override
        public Response<AiMessage> generate(List<ChatMessage> messages) {
            return Response.from(AiMessage.from("模型直接返回"));
        }

        @Override
        public Response<AiMessage> generate(List<ChatMessage> messages, List<ToolSpecification> toolSpecifications) {
            boolean hasToolResult = messages.stream().anyMatch(message -> message instanceof ToolExecutionResultMessage);
            if (!hasToolResult) {
                ToolExecutionRequest request = ToolExecutionRequest.builder()
                        .id("call_1")
                        .name("queryLogs")
                        .arguments("""
                                {"serviceName":"order-service","timeRange":"昨晚 2 点到 3 点","keyword":"timeout"}
                                """)
                        .build();
                return Response.from(AiMessage.from(List.of(request)));
            }

            String answer = """
                    问题描述
                    用户反馈昨晚订单接口出现响应变慢。

                    调用证据
                    - 日志显示数据库连接超时。

                    初步判断
                    问题与数据库连接等待有关。

                    根因分析
                    最可能原因是数据库连接池等待或慢 SQL 导致请求阻塞。

                    建议动作
                    1. 检查连接池配置。
                    2. 排查慢 SQL。
                    3. 对关键接口增加链路追踪。

                    风险提示
                    当前结论基于模拟工具结果，请结合真实环境复核。
                    """;
            return Response.from(AiMessage.from(answer));
        }
    }
}
