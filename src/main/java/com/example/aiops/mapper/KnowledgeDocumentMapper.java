package com.example.aiops.mapper;

import com.example.aiops.entity.KnowledgeDocument;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface KnowledgeDocumentMapper {

    @Select("SELECT * FROM knowledge_document")
    List<KnowledgeDocument> findAll();
}
