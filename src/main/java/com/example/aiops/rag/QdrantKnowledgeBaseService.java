package com.example.aiops.rag;

import com.example.aiops.agent.AgentRunContext;
import com.example.aiops.agent.AgentRunContextHolder;
import com.example.aiops.entity.KnowledgeChunk;
import com.example.aiops.entity.KnowledgeDocument;
import com.example.aiops.entity.LlmConfig;
import com.example.aiops.mapper.KnowledgeChunkMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@Primary
@ConditionalOnProperty(prefix = "aiops.vector", name = "enabled", havingValue = "true")
public class QdrantKnowledgeBaseService implements KnowledgeBaseService {

    private static final Logger log = LoggerFactory.getLogger(QdrantKnowledgeBaseService.class);
    private static final int RECALL_LIMIT = 50;

    private final EmbeddingConfigResolver embeddingConfigResolver;
    private final RerankConfigResolver rerankConfigResolver;
    private final QwenEmbeddingService embeddingService;
    private final QwenRerankService rerankService;
    private final QdrantKnowledgeVectorStore vectorStore;
    private final KnowledgeChunkMapper knowledgeChunkMapper;
    private final InMemoryKnowledgeBaseService fallbackService;
    private final QueryRewriteService queryRewriteService;
    private final boolean rerankEnabled;
    private final long remoteTimeoutMs;

    public QdrantKnowledgeBaseService(EmbeddingConfigResolver embeddingConfigResolver,
                                      RerankConfigResolver rerankConfigResolver,
                                      QwenEmbeddingService embeddingService,
                                      QwenRerankService rerankService,
                                      QdrantKnowledgeVectorStore vectorStore,
                                      KnowledgeChunkMapper knowledgeChunkMapper,
                                      InMemoryKnowledgeBaseService fallbackService,
                                      QueryRewriteService queryRewriteService,
                                      @Value("${aiops.rag.rerank.enabled:false}") boolean rerankEnabled,
                                      @Value("${aiops.rag.remote-timeout-ms:2000}") long remoteTimeoutMs) {
        this.embeddingConfigResolver = embeddingConfigResolver;
        this.rerankConfigResolver = rerankConfigResolver;
        this.embeddingService = embeddingService;
        this.rerankService = rerankService;
        this.vectorStore = vectorStore;
        this.knowledgeChunkMapper = knowledgeChunkMapper;
        this.fallbackService = fallbackService;
        this.queryRewriteService = queryRewriteService;
        this.rerankEnabled = rerankEnabled;
        this.remoteTimeoutMs = remoteTimeoutMs;
    }

    @Override
    public List<KnowledgeDocument> search(String query, int limit) {
        return search(KnowledgeSearchCriteria.simple(query, limit));
    }

    @Override
    public List<KnowledgeDocument> search(KnowledgeSearchCriteria criteria) {
        long startNanos = System.nanoTime();
        try {
            long rewriteStart = System.nanoTime();
            RewrittenQuery rewritten = queryRewriteService.rewrite(criteria.getQuery());
            publishSegment("knowledge_query_rewrite", "知识库查询改写完成", rewriteStart);
            KnowledgeSearchCriteria effective = mergeCriteria(criteria, rewritten);
            List<KnowledgeChunk> candidates = recallCandidates(effective, rewritten);
            if (candidates.isEmpty()) {
                log.info("Knowledge search falling back to local documents, query={}, elapsedMs={}", criteria.getQuery(), elapsedMs(startNanos));
                return fallbackService.search(criteria);
            }
            List<KnowledgeChunk> ranked = rerankOrLocal(effective, candidates);
            log.info("Knowledge search completed, query={}, candidates={}, elapsedMs={}",
                    criteria.getQuery(), candidates.size(), elapsedMs(startNanos));
            return ranked.stream()
                    .limit(effective.getLimit())
                    .map(this::toDocument)
                    .toList();
        } catch (Exception ex) {
            log.warn("Knowledge search failed, falling back to local documents, query={}, elapsedMs={}, error={}",
                    criteria.getQuery(), elapsedMs(startNanos), ex.getMessage());
            return fallbackService.search(criteria);
        }
    }

    private List<KnowledgeChunk> recallCandidates(KnowledgeSearchCriteria criteria, RewrittenQuery rewritten) {
        Map<String, KnowledgeChunk> merged = new LinkedHashMap<>();
        try {
            long embeddingStart = System.nanoTime();
            LlmConfig embeddingConfig = embeddingConfigResolver.resolveRequired();
            List<Float> vector = embeddingService.embed(rewritten.getQuery(), embeddingConfig);
            publishSegment("knowledge_embedding", "向量生成完成", embeddingStart);
            long qdrantStart = System.nanoTime();
            List<VectorSearchHit> hits = vectorStore.search(vector, RECALL_LIMIT, criteria, remoteTimeoutMs);
            publishSegment("knowledge_qdrant", "向量召回完成", qdrantStart);
            List<String> chunkIds = hits.stream()
                    .map(VectorSearchHit::getChunkId)
                    .filter(id -> id != null && !id.isBlank())
                    .toList();
            if (!chunkIds.isEmpty()) {
                Map<String, VectorSearchHit> hitMap = hits.stream()
                        .filter(hit -> hit.getChunkId() != null)
                        .collect(Collectors.toMap(VectorSearchHit::getChunkId, Function.identity(), (left, right) -> left));
                for (KnowledgeChunk chunk : knowledgeChunkMapper.findByChunkIds(chunkIds)) {
                    if (matchesTime(chunk, criteria)) {
                        VectorSearchHit hit = hitMap.get(chunk.getChunkId());
                        chunk.setVectorScore(hit == null ? 0D : hit.getScore());
                        merged.put(chunk.getChunkId(), chunk);
                    }
                }
            }
        } catch (Exception ex) {
            log.warn("Vector recall degraded to keyword recall, query={}, error={}", rewritten.getQuery(), ex.getMessage());
            // Keyword recall keeps the search usable when vector services are not ready.
        }
        long keywordStart = System.nanoTime();
        List<KnowledgeChunk> keywordHits = knowledgeChunkMapper.searchKeyword(
                rewritten.getQuery(),
                criteria.getCountry(),
                criteria.getBusinessLine(),
                criteria.getSystemName(),
                criteria.getStartTime(),
                criteria.getEndTime(),
                criteria.effectivePermissionCodes(),
                RECALL_LIMIT);
        publishSegment("knowledge_keyword", "关键词召回完成", keywordStart);
        for (KnowledgeChunk chunk : keywordHits) {
            KnowledgeChunk existing = merged.getOrDefault(chunk.getChunkId(), chunk);
            existing.setKeywordScore(keywordScore(chunk, rewritten));
            merged.put(existing.getChunkId(), existing);
        }
        List<KnowledgeChunk> candidates = new ArrayList<>(merged.values());
        for (KnowledgeChunk candidate : candidates) {
            candidate.setSimilarityScore(localScore(candidate));
        }
        candidates.sort(Comparator.comparingDouble((KnowledgeChunk chunk) -> chunk.getSimilarityScore() == null ? 0D : chunk.getSimilarityScore()).reversed());
        return candidates.stream().limit(RECALL_LIMIT).toList();
    }

    private List<KnowledgeChunk> rerankOrLocal(KnowledgeSearchCriteria criteria, List<KnowledgeChunk> candidates) {
        if (!rerankEnabled) {
            return localRank(candidates);
        }
        long rerankStart = System.nanoTime();
        try {
            LlmConfig config = rerankConfigResolver.resolveRequired();
            List<String> docs = candidates.stream().map(this::rerankText).toList();
            List<RerankResult> results = rerankService.rerank(criteria.getQuery(), docs, criteria.getLimit(), config);
            publishSegment("knowledge_rerank", "重排完成", rerankStart);
            if (!results.isEmpty()) {
                List<KnowledgeChunk> ranked = new ArrayList<>();
                for (RerankResult result : results) {
                    if (result.getIndex() >= 0 && result.getIndex() < candidates.size()) {
                        KnowledgeChunk chunk = candidates.get(result.getIndex());
                        chunk.setRerankScore(result.getScore());
                        chunk.setSimilarityScore(result.getScore());
                        ranked.add(chunk);
                    }
                }
                return ranked;
            }
        } catch (Exception ex) {
            log.warn("Rerank degraded to local rank, query={}, elapsedMs={}, error={}",
                    criteria.getQuery(), elapsedMs(rerankStart), ex.getMessage());
            // Fall back to local fusion when rerank is not configured or temporarily unavailable.
        }
        return localRank(candidates);
    }

    private List<KnowledgeChunk> localRank(List<KnowledgeChunk> candidates) {
        return candidates.stream()
                .sorted(Comparator.comparingDouble((KnowledgeChunk chunk) -> chunk.getSimilarityScore() == null ? 0D : chunk.getSimilarityScore()).reversed())
                .toList();
    }

    private KnowledgeSearchCriteria mergeCriteria(KnowledgeSearchCriteria criteria, RewrittenQuery rewritten) {
        KnowledgeSearchCriteria effective = new KnowledgeSearchCriteria();
        effective.setQuery(rewritten.getQuery() == null ? criteria.getQuery() : rewritten.getQuery());
        effective.setLimit(criteria.getLimit());
        effective.setCountry(criteria.getCountry() == null ? rewritten.getCountry() : criteria.getCountry());
        effective.setBusinessLine(criteria.getBusinessLine() == null ? rewritten.getBusinessLine() : criteria.getBusinessLine());
        effective.setSystemName(criteria.getSystemName() == null ? rewritten.getSystemName() : criteria.getSystemName());
        effective.setStartTime(criteria.getStartTime());
        effective.setEndTime(criteria.getEndTime());
        effective.setPermissionCodes(criteria.effectivePermissionCodes());
        return effective;
    }

    private double keywordScore(KnowledgeChunk chunk, RewrittenQuery rewritten) {
        String text = (KnowledgeTextUtils.safe(chunk.getTitle()) + " "
                + KnowledgeTextUtils.safe(chunk.getChunkContent()) + " "
                + KnowledgeTextUtils.safe(chunk.getTags())).toLowerCase();
        double score = 0D;
        List<String> tokens = rewritten.getKeywords().isEmpty()
                ? KnowledgeTextUtils.tokens(rewritten.getQuery())
                : rewritten.getKeywords();
        for (String token : tokens) {
            if (token != null && !token.isBlank() && text.contains(token.toLowerCase())) {
                score += token.length() >= 4 ? 0.2D : 0.12D;
            }
        }
        return Math.min(score, 1D);
    }

    private double localScore(KnowledgeChunk chunk) {
        double vector = chunk.getVectorScore() == null ? 0D : chunk.getVectorScore();
        double keyword = chunk.getKeywordScore() == null ? 0D : chunk.getKeywordScore();
        return vector * 0.70D + keyword * 0.30D;
    }

    private boolean matchesTime(KnowledgeChunk chunk, KnowledgeSearchCriteria criteria) {
        if (chunk.getEventTime() == null) {
            return true;
        }
        if (criteria.getStartTime() != null && chunk.getEventTime().isBefore(criteria.getStartTime())) {
            return false;
        }
        return criteria.getEndTime() == null || !chunk.getEventTime().isAfter(criteria.getEndTime());
    }

    private String rerankText(KnowledgeChunk chunk) {
        return KnowledgeTextUtils.safe(chunk.getTitle())
                + "\n" + KnowledgeTextUtils.safe(chunk.getSectionPath())
                + "\n" + KnowledgeTextUtils.safe(chunk.getChunkContent());
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
        doc.setVectorScore(chunk.getVectorScore());
        doc.setKeywordScore(chunk.getKeywordScore());
        doc.setRerankScore(chunk.getRerankScore());
        doc.setSimilarityScore(chunk.getSimilarityScore());
        return doc;
    }

    private void publishSegment(String stage, String message, long startNanos) {
        long elapsedMs = elapsedMs(startNanos);
        AgentRunContext context = AgentRunContextHolder.get();
        if (context != null) {
            context.publishStatus(stage, message + " " + elapsedMs + "ms", elapsedMs);
        }
        log.info("{}, stage={}, elapsedMs={}", message, stage, elapsedMs);
    }

    private long elapsedMs(long startNanos) {
        return (System.nanoTime() - startNanos) / 1_000_000L;
    }
}
