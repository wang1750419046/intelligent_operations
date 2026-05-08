package com.example.aiops.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class KnowledgeBaseControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void shouldExposeVectorStatusWhenDisabledInTests() throws Exception {
        mockMvc.perform(get("/api/kb/vector/status"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.enabled").value(false))
                .andExpect(jsonPath("$.data.collection").value("aiops_knowledge_v1"));
    }

    @Test
    void shouldSkipReindexWhenVectorDisabledInTests() throws Exception {
        mockMvc.perform(post("/api/kb/reindex"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.successCount").value(0))
                .andExpect(jsonPath("$.data.failureCount").value(0));
    }
}
