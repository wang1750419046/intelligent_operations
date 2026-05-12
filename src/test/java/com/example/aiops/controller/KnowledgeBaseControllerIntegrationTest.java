package com.example.aiops.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
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

    @Test
    void shouldCreateChunkAndSearchWhenVectorDisabled() throws Exception {
        mockMvc.perform(post("/api/kb/documents")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "title":"支付超时排查",
                                  "content":"支付接口超时时，先检查 payment-service 连接池、慢 SQL 与下游网关状态。",
                                  "source":"runbook",
                                  "tags":"payment,timeout",
                                  "country":"CN",
                                  "businessLine":"支付",
                                  "systemName":"payment-service",
                                  "permissionCodes":["PUBLIC"]
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.docId").isNotEmpty())
                .andExpect(jsonPath("$.data.chunkCount").value(1))
                .andExpect(jsonPath("$.data.indexStatus").value("SKIPPED"));

        mockMvc.perform(post("/api/kb/search")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "query":"payment-service 支付接口超时",
                                  "limit":3,
                                  "country":"CN",
                                  "permissionCodes":["PUBLIC"]
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].title").value("支付超时排查"));
    }

    @Test
    void shouldImportTextDocumentAndDeleteIt() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "release-runbook.txt",
                MediaType.TEXT_PLAIN_VALUE,
                "发布后接口变慢，需要检查变更批次、线程池和依赖版本。".getBytes(java.nio.charset.StandardCharsets.UTF_8));

        String response = mockMvc.perform(multipart("/api/kb/documents/import")
                        .file(file)
                        .param("source", "upload")
                        .param("permissionCodes", "PUBLIC"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.docId").isNotEmpty())
                .andExpect(jsonPath("$.data.chunkCount").value(1))
                .andReturn()
                .getResponse()
                .getContentAsString();

        String docId = response.replaceAll(".*\\\"docId\\\":\\\"([^\\\"]+)\\\".*", "$1");
        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete("/api/kb/documents/{docId}", docId))
                .andExpect(status().isOk());
    }
}
