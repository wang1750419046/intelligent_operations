package com.example.aiops.rag;

import com.example.aiops.entity.KnowledgeChunk;
import com.example.aiops.entity.KnowledgeDocument;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class KnowledgeChunkerTest {

    private final KnowledgeChunker chunker = new KnowledgeChunker();

    @Test
    void shouldSplitByBusinessSectionsBeforeLength() {
        KnowledgeDocument document = new KnowledgeDocument();
        document.setDocId("kb-test");
        document.setTitle("支付故障手册");
        document.setContent("""
                一、故障现象
                支付接口 RT 升高，错误日志出现 connection timeout。

                二、排查步骤
                1. 检查连接池活跃连接数。
                2. 检查慢 SQL 和下游依赖。

                问: 如果队列满了怎么办
                答: 优先限流并扩容消费者线程池。
                """);

        List<KnowledgeChunk> chunks = chunker.split(document);

        assertFalse(chunks.isEmpty());
        assertTrue(chunks.stream().anyMatch(chunk -> chunk.getChunkContent().contains("排查步骤")));
        assertTrue(chunks.stream().allMatch(chunk -> chunk.getChunkId().startsWith("chk_")));
    }
}
