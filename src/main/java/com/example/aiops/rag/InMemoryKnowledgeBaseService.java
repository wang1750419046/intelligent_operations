package com.example.aiops.rag;

import com.example.aiops.entity.KnowledgeDocument;
import com.example.aiops.mapper.KnowledgeDocumentMapper;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;
import java.util.Locale;

@Service
public class InMemoryKnowledgeBaseService implements KnowledgeBaseService {

    private final KnowledgeDocumentMapper knowledgeDocumentMapper;

    public InMemoryKnowledgeBaseService(KnowledgeDocumentMapper knowledgeDocumentMapper) {
        this.knowledgeDocumentMapper = knowledgeDocumentMapper;
    }

    @Override
    public List<KnowledgeDocument> search(String query, int limit) {
        String normalized = query == null ? "" : query.toLowerCase(Locale.ROOT);
        return knowledgeDocumentMapper.findAll().stream()
                .sorted(Comparator.comparingInt((KnowledgeDocument doc) -> score(doc, normalized)).reversed())
                .filter(doc -> score(doc, normalized) > 0)
                .limit(limit)
                .toList();
    }

    private int score(KnowledgeDocument doc, String query) {
        int score = 0;
        String tags = doc.getTags() == null ? "" : doc.getTags().replace(",", " ");
        String text = (doc.getTitle() + " " + doc.getContent() + " " + tags).toLowerCase(Locale.ROOT);
        for (String token : query.split("\\s+")) {
            if (!token.isBlank() && text.contains(token)) {
                score += 2;
            }
        }
        if (text.contains("超时") && query.contains("慢")) {
            score += 1;
        }
        if (text.contains("发布") && query.contains("发布")) {
            score += 2;
        }
        return score;
    }
}
