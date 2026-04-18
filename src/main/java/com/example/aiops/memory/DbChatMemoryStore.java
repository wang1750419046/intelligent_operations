package com.example.aiops.memory;

import com.example.aiops.entity.ChatMessage;
import com.example.aiops.mapper.ChatMessageMapper;
import dev.langchain4j.data.message.ChatMessageDeserializer;
import dev.langchain4j.data.message.ChatMessageSerializer;
import dev.langchain4j.store.memory.chat.ChatMemoryStore;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

@Component
public class DbChatMemoryStore implements ChatMemoryStore {

    private static final String MEMORY_SCOPE = "memory";

    private final ChatMessageMapper chatMessageMapper;

    public DbChatMemoryStore(ChatMessageMapper chatMessageMapper) {
        this.chatMessageMapper = chatMessageMapper;
    }

    @Override
    public List<dev.langchain4j.data.message.ChatMessage> getMessages(Object memoryId) {
        return chatMessageMapper.findBySessionIdAndScope(String.valueOf(memoryId), MEMORY_SCOPE).stream()
                .sorted(Comparator.comparing(ChatMessage::getCreatedAt).thenComparing(ChatMessage::getId))
                .map(row -> ChatMessageDeserializer.messageFromJson(row.getMessageJson()))
                .toList();
    }

    @Override
    public void updateMessages(Object memoryId, List<dev.langchain4j.data.message.ChatMessage> messages) {
        String sessionId = String.valueOf(memoryId);
        chatMessageMapper.deleteBySessionIdAndScope(sessionId, MEMORY_SCOPE);
        for (dev.langchain4j.data.message.ChatMessage message : messages) {
            ChatMessage row = new ChatMessage();
            row.setMessageId("mem_" + UUID.randomUUID().toString().replace("-", ""));
            row.setSessionId(sessionId);
            row.setScope(MEMORY_SCOPE);
            row.setRole(message.type().name().toLowerCase());
            row.setContent(message.text() == null ? "" : message.text());
            row.setMessageJson(ChatMessageSerializer.messageToJson(message));
            row.setCreatedAt(LocalDateTime.now());
            chatMessageMapper.insert(row);
        }
    }

    @Override
    public void deleteMessages(Object memoryId) {
        chatMessageMapper.deleteBySessionIdAndScope(String.valueOf(memoryId), MEMORY_SCOPE);
    }
}
