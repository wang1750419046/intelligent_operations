package com.example.aiops.rag;

import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

class QwenRerankServiceTest {

    @Test
    void shouldParseRerankResults() {
        QwenRerankService service = new QwenRerankService(RestClient.builder(), 2000);

        List<RerankResult> results = service.parseResults(Map.of(
                "results", List.of(
                        Map.of("index", 1, "relevance_score", 0.93),
                        Map.of("index", 0, "score", 0.51)
                )));

        assertEquals(2, results.size());
        assertEquals(1, results.get(0).getIndex());
        assertEquals(0.93D, results.get(0).getScore(), 0.0001D);
    }
}
