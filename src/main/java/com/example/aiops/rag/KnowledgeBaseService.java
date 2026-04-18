package com.example.aiops.rag;

import com.example.aiops.entity.KnowledgeDocument;

import java.util.List;

public interface KnowledgeBaseService {

    List<KnowledgeDocument> search(String query, int limit);
}
