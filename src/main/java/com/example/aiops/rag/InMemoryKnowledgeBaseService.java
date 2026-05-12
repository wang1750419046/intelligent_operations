package com.example.aiops.rag;

import com.example.aiops.entity.KnowledgeChunk;
import com.example.aiops.entity.KnowledgeDocument;
import com.example.aiops.mapper.KnowledgeChunkMapper;
import com.example.aiops.mapper.KnowledgeDocumentMapper;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;
import java.util.Locale;

@Service
public class InMemoryKnowledgeBaseService implements KnowledgeBaseService {

    private final KnowledgeDocumentMapper knowledgeDocumentMapper;
    private final KnowledgeChunkMapper knowledgeChunkMapper;

    public InMemoryKnowledgeBaseService(KnowledgeDocumentMapper knowledgeDocumentMapper,
                                        KnowledgeChunkMapper knowledgeChunkMapper) {
        this.knowledgeDocumentMapper = knowledgeDocumentMapper;
        this.knowledgeChunkMapper = knowledgeChunkMapper;
    }

    @Override
    public List<KnowledgeDocument> search(String query, int limit) {
        return search(KnowledgeSearchCriteria.simple(query, limit));
    }

    @Override
    public List<KnowledgeDocument> search(KnowledgeSearchCriteria criteria) {
        String query = criteria.getQuery() == null ? "" : criteria.getQuery();
        List<KnowledgeChunk> chunks = knowledgeChunkMapper.searchKeyword(
                query,
                criteria.getCountry(),
                criteria.getBusinessLine(),
                criteria.getSystemName(),
                criteria.getStartTime(),
                criteria.getEndTime(),
                criteria.effectivePermissionCodes(),
                Math.max(criteria.getLimit() * 10, 20));
        if (!chunks.isEmpty()) {
            return chunks.stream()
                    .peek(chunk -> chunk.setKeywordScore((double) score(chunk, query)))
                    .sorted(Comparator.comparingDouble((KnowledgeChunk chunk) -> chunk.getKeywordScore() == null ? 0D : chunk.getKeywordScore()).reversed())
                    .limit(criteria.getLimit())
                    .map(this::toDocument)
                    .toList();
        }
        String normalized = query.toLowerCase(Locale.ROOT);
        return knowledgeDocumentMapper.findAll().stream()
                .sorted(Comparator.comparingInt((KnowledgeDocument doc) -> score(doc, normalized)).reversed())
                .filter(doc -> score(doc, normalized) > 0)
                .limit(criteria.getLimit())
                .toList();
    }

    private int score(KnowledgeChunk chunk, String query) {
        String text = (KnowledgeTextUtils.safe(chunk.getTitle()) + " "
                + KnowledgeTextUtils.safe(chunk.getChunkContent()) + " "
                + KnowledgeTextUtils.safe(chunk.getTags())).toLowerCase(Locale.ROOT);
        return scoreText(text, query.toLowerCase(Locale.ROOT));
    }

    private int score(KnowledgeDocument doc, String query) {
        String text = (KnowledgeTextUtils.safe(doc.getTitle()) + " "
                + KnowledgeTextUtils.safe(doc.getContent()) + " "
                + KnowledgeTextUtils.safe(doc.getTags())).toLowerCase(Locale.ROOT);
        return scoreText(text, query);
    }

    private int scoreText(String text, String query) {
        int score = 0;
        for (String token : KnowledgeTextUtils.tokens(query)) {
            if (text.contains(token)) {
                score += token.length() >= 4 ? 3 : 2;
            }
        }
        if (score == 0 && text.contains(query)) {
            score = 1;
        }
        return score;
    }

    private KnowledgeDocument toDocument(KnowledgeChunk chunk) {
        KnowledgeDocument doc = new KnowledgeDocument();
        doc.setDocId(chunk.getDocId());
        doc.setChunkId(chunk.getChunkId());
        doc.setChunkIndex(chunk.getChunkIndex());
        doc.setSectionPath(chunk.getSectionPath());
        doc.setTitle(chunk.getTitle());
        doc.setContent(chunk.getChunkContent());
        doc.setSource(chunk.getSource());
        doc.setTags(chunk.getTags());
        doc.setCountry(chunk.getCountry());
        doc.setBusinessLine(chunk.getBusinessLine());
        doc.setSystemName(chunk.getSystemName());
        doc.setPermissionCodes(chunk.getPermissionCodes());
        doc.setKeywordScore(chunk.getKeywordScore());
        doc.setSimilarityScore(chunk.getKeywordScore());
        return doc;
    }
}
