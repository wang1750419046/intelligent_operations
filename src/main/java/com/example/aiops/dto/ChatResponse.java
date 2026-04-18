package com.example.aiops.dto;

import java.util.List;

public class ChatResponse {

    private String answer;
    private List<String> usedTools;
    private List<ReferenceItem> references;

    public ChatResponse() {
    }

    public ChatResponse(String answer, List<String> usedTools, List<ReferenceItem> references) {
        this.answer = answer;
        this.usedTools = usedTools;
        this.references = references;
    }

    public String getAnswer() {
        return answer;
    }

    public void setAnswer(String answer) {
        this.answer = answer;
    }

    public List<String> getUsedTools() {
        return usedTools;
    }

    public void setUsedTools(List<String> usedTools) {
        this.usedTools = usedTools;
    }

    public List<ReferenceItem> getReferences() {
        return references;
    }

    public void setReferences(List<ReferenceItem> references) {
        this.references = references;
    }
}
