package com.example.aiops.dto;

public class ReferenceItem {

    private String type;
    private String title;
    private String content;
    private String source;
    private Double score;

    public ReferenceItem() {
    }

    public ReferenceItem(String type, String title, String content, String source) {
        this.type = type;
        this.title = title;
        this.content = content;
        this.source = source;
    }

    public ReferenceItem(String type, String title, String content, String source, Double score) {
        this.type = type;
        this.title = title;
        this.content = content;
        this.source = source;
        this.score = score;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
    }

    public Double getScore() {
        return score;
    }

    public void setScore(Double score) {
        this.score = score;
    }
}
