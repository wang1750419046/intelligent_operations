package com.example.aiops.service;

import com.example.aiops.dto.SessionDetailResponse;
import com.example.aiops.entity.ChatSession;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@SpringBootTest
class SessionServiceTest {

    @Autowired
    private SessionService sessionService;

    @Test
    void shouldPersistSessionWithModelConfig() {
        ChatSession session = sessionService.createSession("服务异常分析", 2L);
        assertNotNull(session.getSessionId());

        SessionDetailResponse detail = sessionService.getSessionDetail(session.getSessionId());
        assertEquals("服务异常分析", detail.getTitle());
        assertEquals(2L, detail.getModelConfigId());
    }
}
