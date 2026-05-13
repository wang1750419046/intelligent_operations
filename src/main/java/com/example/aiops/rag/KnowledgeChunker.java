package com.example.aiops.rag;

import com.example.aiops.entity.KnowledgeChunk;
import com.example.aiops.entity.KnowledgeDocument;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.regex.Pattern;

@Component
public class KnowledgeChunker {

    private static final int TARGET_MIN = 600;
    private static final int TARGET_MAX = 900;
    private static final int HARD_MAX = 1200;
    private static final int OVERLAP = 100;
    private static final Pattern HEADING = Pattern.compile("^(#{1,6}\\s+.+|[一二三四五六七八九十]+[、.．].+|\\d{1,2}[、.．)]\\s*.+|第.+[章节篇].+)$");
    private static final Pattern FAQ = Pattern.compile("^(Q[:：].+|问[:：].+|问题[:：].+)$", Pattern.CASE_INSENSITIVE);
    private static final Pattern LOG_OR_CODE = Pattern.compile("^(\\d{4}-\\d{2}-\\d{2}.+|\\[?(INFO|WARN|ERROR|DEBUG)\\]?.+|```.*)$", Pattern.CASE_INSENSITIVE);

    public List<KnowledgeChunk> split(KnowledgeDocument document) {
        String text = KnowledgeTextUtils.normalizeText(document.getContent());
        if (text.isBlank()) {
            return List.of();
        }
        List<Block> blocks = buildSemanticBlocks(text);
        List<ChunkDraft> drafts = packBlocks(blocks);
        List<KnowledgeChunk> chunks = new ArrayList<>();
        LocalDateTime now = LocalDateTime.now();
        for (int i = 0; i < drafts.size(); i++) {
            ChunkDraft draft = drafts.get(i);
            KnowledgeChunk chunk = new KnowledgeChunk();
            chunk.setDocId(document.getDocId());
            chunk.setChunkIndex(i);
            chunk.setChunkId(stableChunkId(document.getDocId(), i, draft.content()));
            chunk.setSectionPath(draft.sectionPath());
            chunk.setChunkContent(draft.content());
            chunk.setContentHash(KnowledgeTextUtils.sha256(draft.content()));
            chunk.setEmbeddingStatus("PENDING");
            chunk.setCreatedAt(now);
            chunk.setUpdatedAt(now);
            chunks.add(chunk);
        }
        return chunks;
    }

    private List<Block> buildSemanticBlocks(String text) {
        List<Block> blocks = new ArrayList<>();
        String currentSection = "";
        StringBuilder current = new StringBuilder();
        for (String rawLine : text.split("\\n")) {
            String line = rawLine.strip();
            if (line.isBlank()) {
                flushBlock(blocks, currentSection, current);
                continue;
            }
            if (isBoundary(line) && current.length() > 0) {
                flushBlock(blocks, currentSection, current);
            }
            if (HEADING.matcher(line).matches()) {
                currentSection = cleanHeading(line);
            }
            current.append(line).append('\n');
            if (current.length() >= HARD_MAX || (LOG_OR_CODE.matcher(line).matches() && current.length() >= TARGET_MIN)) {
                flushBlock(blocks, currentSection, current);
            }
        }
        flushBlock(blocks, currentSection, current);
        return blocks;
    }

    private List<ChunkDraft> packBlocks(List<Block> blocks) {
        List<ChunkDraft> drafts = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        String section = "";
        for (Block block : blocks) {
            if (block.content().length() > HARD_MAX) {
                flushDraft(drafts, section, current);
                drafts.addAll(splitLongBlock(block));
                continue;
            }
            if (current.length() > 0 && current.length() + block.content().length() > TARGET_MAX) {
                flushDraft(drafts, section, current);
            }
            if (current.length() == 0) {
                section = block.sectionPath();
            }
            current.append(block.content()).append("\n\n");
            if (current.length() >= TARGET_MIN && current.length() >= TARGET_MAX) {
                flushDraft(drafts, section, current);
            }
        }
        flushDraft(drafts, section, current);
        return drafts;
    }

    private List<ChunkDraft> splitLongBlock(Block block) {
        List<ChunkDraft> drafts = new ArrayList<>();
        String text = block.content();
        int start = 0;
        while (start < text.length()) {
            int end = Math.min(start + HARD_MAX, text.length());
            if (end < text.length()) {
                end = bestSemanticBreak(text, start, end);
            }
            String piece = text.substring(start, end).strip();
            if (!piece.isBlank()) {
                drafts.add(new ChunkDraft(block.sectionPath(), piece));
            }
            if (end >= text.length()) {
                start = end;
            } else {
                start = Math.max(start + 1, end - OVERLAP);
            }
        }
        return drafts;
    }

    private int bestSemanticBreak(String text, int start, int hardEnd) {
        int best = -1;
        String breakers = "。！？；\n";
        for (int i = hardEnd - 1; i > start + TARGET_MIN; i--) {
            if (breakers.indexOf(text.charAt(i)) >= 0) {
                best = i + 1;
                break;
            }
        }
        return best > 0 ? best : hardEnd;
    }

    private boolean isBoundary(String line) {
        return HEADING.matcher(line).matches()
                || FAQ.matcher(line).matches()
                || line.startsWith("|")
                || LOG_OR_CODE.matcher(line).matches();
    }

    private String cleanHeading(String line) {
        return line.replaceFirst("^#{1,6}\\s+", "").strip();
    }

    private void flushBlock(List<Block> blocks, String section, StringBuilder current) {
        String content = current.toString().strip();
        if (!content.isBlank()) {
            blocks.add(new Block(section, content));
        }
        current.setLength(0);
    }

    private void flushDraft(List<ChunkDraft> drafts, String section, StringBuilder current) {
        String content = current.toString().strip();
        if (!content.isBlank()) {
            drafts.add(new ChunkDraft(section, content));
        }
        current.setLength(0);
    }

    private String stableChunkId(String docId, int index, String content) {
        String seed = docId + ":" + index + ":" + KnowledgeTextUtils.sha256(content);
        return "chk_" + UUID.nameUUIDFromBytes(seed.getBytes(java.nio.charset.StandardCharsets.UTF_8))
                .toString()
                .replace("-", "");
    }

    private record Block(String sectionPath, String content) {
    }

    private record ChunkDraft(String sectionPath, String content) {
    }
}
