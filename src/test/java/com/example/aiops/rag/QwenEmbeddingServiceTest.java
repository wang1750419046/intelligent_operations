package com.example.aiops.rag;

import com.example.aiops.entity.LlmConfig;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.util.List;

import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class QwenEmbeddingServiceTest {

    @Test
    void shouldUseOpenAiCompatibleEndpointForTextEmbeddingModel() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        QwenEmbeddingService service = new QwenEmbeddingService(builder, 1024, 0);

        server.expect(requestTo("https://dashscope.aliyuncs.com/compatible-mode/v1/embeddings"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(content().string(containsString("\"model\":\"text-embedding-v3\"")))
                .andRespond(withSuccess("""
                        {"data":[{"embedding":[0.1,0.2]}]}
                        """, MediaType.APPLICATION_JSON));

        List<Float> vector = service.embed("hello", config("text-embedding-v3", "https://dashscope.aliyuncs.com/compatible-mode/v1"));

        assertEquals(List.of(0.1F, 0.2F), vector);
        server.verify();
    }

    @Test
    void shouldUseMultimodalEndpointForVisionEmbeddingModel() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        QwenEmbeddingService service = new QwenEmbeddingService(builder, 1024, 0);

        server.expect(requestTo("https://dashscope.aliyuncs.com/api/v1/services/embeddings/multimodal-embedding/multimodal-embedding"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(content().string(containsString("\"model\":\"tongyi-embedding-vision-plus-2026-03-06\"")))
                .andExpect(content().string(containsString("\"dimension\":1024")))
                .andRespond(withSuccess("""
                        {"output":{"embeddings":[{"index":0,"embedding":[0.3,0.4]}]}}
                        """, MediaType.APPLICATION_JSON));

        List<Float> vector = service.embed(
                "hello",
                config("tongyi-embedding-vision-plus-2026-03-06", "https://dashscope.aliyuncs.com/compatible-mode/v1"));

        assertEquals(List.of(0.3F, 0.4F), vector);
        server.verify();
    }

    private LlmConfig config(String modelName, String baseUrl) {
        LlmConfig config = new LlmConfig();
        config.setModelName(modelName);
        config.setBaseUrl(baseUrl);
        config.setApiKey("sk-test");
        return config;
    }
}
