package com.example.aiops.dto;

public class KnowledgeImportResponse {

    private String docId;
    private String title;
    private String filename;
    private int extractedCharacters;
    private int chunkCount;
    private String indexStatus;

    public KnowledgeImportResponse() {
    }

    public KnowledgeImportResponse(String docId, String title, String filename, int extractedCharacters, int chunkCount, String indexStatus) {
        this.docId = docId;
        this.title = title;
        this.filename = filename;
        this.extractedCharacters = extractedCharacters;
        this.chunkCount = chunkCount;
        this.indexStatus = indexStatus;
    }

    public String getDocId() {
        return docId;
    }

    public void setDocId(String docId) {
        this.docId = docId;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getFilename() {
        return filename;
    }

    public void setFilename(String filename) {
        this.filename = filename;
    }

    public int getExtractedCharacters() {
        return extractedCharacters;
    }

    public void setExtractedCharacters(int extractedCharacters) {
        this.extractedCharacters = extractedCharacters;
    }

    public int getChunkCount() {
        return chunkCount;
    }

    public void setChunkCount(int chunkCount) {
        this.chunkCount = chunkCount;
    }

    public String getIndexStatus() {
        return indexStatus;
    }

    public void setIndexStatus(String indexStatus) {
        this.indexStatus = indexStatus;
    }
}
