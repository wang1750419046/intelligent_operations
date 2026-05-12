package com.example.aiops.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class ModelConfigControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void shouldListSeededConfigsAndCreateOne() throws Exception {
        mockMvc.perform(get("/api/model-configs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].provider").exists());

        mockMvc.perform(post("/api/model-configs")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "自定义 GPT",
                                  "provider": "OPENAI",
                                  "baseUrl": "https://api.openai.com/v1",
                                  "apiKey": "sk-test",
                                  "modelName": "gpt-4.1-mini",
                                  "temperature": 0.2,
                                  "maxTokens": 2048,
                                  "enabled": true,
                                  "defaultConfig": false
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.name").value("自定义 GPT"))
                .andExpect(jsonPath("$.data.apiKey").value("******"))
                .andExpect(jsonPath("$.data.hasApiKey").value(true));
    }

    @Test
    void shouldKeepExistingApiKeyWhenUpdateOmitsApiKey() throws Exception {
        String created = mockMvc.perform(post("/api/model-configs")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "key-retention",
                                  "provider": "OPENAI",
                                  "baseUrl": "https://api.openai.com/v1",
                                  "apiKey": "sk-test",
                                  "modelName": "gpt-4.1-mini",
                                  "temperature": 0.2,
                                  "maxTokens": 2048,
                                  "enabled": true,
                                  "defaultConfig": false
                                }
                                """))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        String id = com.jayway.jsonpath.JsonPath.read(created, "$.data.id").toString();

        mockMvc.perform(put("/api/model-configs/{id}", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "key-retention-updated",
                                  "provider": "OPENAI",
                                  "baseUrl": "https://api.openai.com/v1",
                                  "modelName": "gpt-4.1-mini",
                                  "temperature": 0.2,
                                  "maxTokens": 2048,
                                  "enabled": true,
                                  "defaultConfig": false
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.name").value("key-retention-updated"))
                .andExpect(jsonPath("$.data.hasApiKey").value(true));
    }

    @Test
    void shouldFilterEmbeddingConfigsWithoutLeakingApiKey() throws Exception {
        mockMvc.perform(get("/api/model-configs").param("configType", "EMBEDDING"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].configType").value("EMBEDDING"))
                .andExpect(jsonPath("$.data[0].modelName").value("text-embedding-v3"))
                .andExpect(jsonPath("$.data[0].apiKey").value(nullValue()));
    }
}
