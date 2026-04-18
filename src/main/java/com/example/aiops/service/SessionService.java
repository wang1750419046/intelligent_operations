package com.example.aiops.service;

import com.example.aiops.dto.SessionDetailResponse;
import com.example.aiops.dto.SessionInfoResponse;
import com.example.aiops.entity.ChatMessage;
import com.example.aiops.entity.ChatSession;

import java.util.List;

public interface SessionService {

    ChatSession createSession(String title, Long modelConfigId);

    List<SessionInfoResponse> listSessions();

    SessionDetailResponse getSessionDetail(String sessionId);

    void deleteSession(String sessionId);

    void appendMessage(ChatMessage message);

    List<ChatMessage> getRecentMessages(String sessionId, int limit);

    ChatSession getSession(String sessionId);
}
