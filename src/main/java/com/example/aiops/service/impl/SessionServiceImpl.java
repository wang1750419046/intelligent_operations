package com.example.aiops.service.impl;

import com.example.aiops.dto.SessionDetailResponse;
import com.example.aiops.dto.SessionInfoResponse;
import com.example.aiops.entity.ChatMessage;
import com.example.aiops.entity.ChatSession;
import com.example.aiops.exception.BusinessException;
import com.example.aiops.mapper.ChatMessageMapper;
import com.example.aiops.mapper.ChatSessionMapper;
import com.example.aiops.service.SessionService;
import com.example.aiops.util.TimeUtils;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

@Service
public class SessionServiceImpl implements SessionService {

    public static final String UI_SCOPE = "ui";

    private final ChatSessionMapper chatSessionMapper;
    private final ChatMessageMapper chatMessageMapper;

    public SessionServiceImpl(ChatSessionMapper chatSessionMapper, ChatMessageMapper chatMessageMapper) {
        this.chatSessionMapper = chatSessionMapper;
        this.chatMessageMapper = chatMessageMapper;
    }

    @Override
    public ChatSession createSession(String title, Long modelConfigId) {
        ChatSession session = new ChatSession();
        session.setSessionId("sess_" + UUID.randomUUID().toString().replace("-", ""));
        session.setTitle(title);
        session.setUserId("demo-user");
        session.setModelConfigId(modelConfigId);
        session.setCreatedAt(LocalDateTime.now());
        session.setUpdatedAt(LocalDateTime.now());
        chatSessionMapper.insert(session);
        return session;
    }

    @Override
    public List<SessionInfoResponse> listSessions() {
        return chatSessionMapper.findAll().stream()
                .map(session -> new SessionInfoResponse(
                        session.getSessionId(),
                        session.getTitle(),
                        session.getModelConfigId(),
                        TimeUtils.format(session.getUpdatedAt())))
                .toList();
    }

    @Override
    public SessionDetailResponse getSessionDetail(String sessionId) {
        ChatSession session = getSession(sessionId);
        List<ChatMessage> messages = chatMessageMapper.findBySessionIdAndScope(sessionId, UI_SCOPE).stream()
                .sorted(Comparator.comparing(ChatMessage::getCreatedAt).thenComparing(ChatMessage::getId))
                .toList();
        return new SessionDetailResponse(session.getSessionId(), session.getTitle(), session.getModelConfigId(), messages);
    }

    @Override
    public void deleteSession(String sessionId) {
        chatSessionMapper.deleteBySessionId(sessionId);
        chatMessageMapper.deleteBySessionId(sessionId);
    }

    @Override
    public void appendMessage(ChatMessage message) {
        chatMessageMapper.insert(message);
        ChatSession session = getSession(message.getSessionId());
        session.setUpdatedAt(LocalDateTime.now());
        chatSessionMapper.update(session);
    }

    @Override
    public List<ChatMessage> getRecentMessages(String sessionId, int limit) {
        List<ChatMessage> messages = chatMessageMapper.findLatestBySessionIdAndScope(sessionId, UI_SCOPE, limit);
        messages.sort(Comparator.comparing(ChatMessage::getCreatedAt).thenComparing(ChatMessage::getId));
        return messages;
    }

    @Override
    public ChatSession getSession(String sessionId) {
        return chatSessionMapper.findOptional(sessionId)
                .orElseThrow(() -> new BusinessException(40401, "session not found"));
    }
}
