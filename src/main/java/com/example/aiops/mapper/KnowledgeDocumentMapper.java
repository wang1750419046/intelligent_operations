package com.example.aiops.mapper;

import com.example.aiops.entity.KnowledgeDocument;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface KnowledgeDocumentMapper {

    @Select("SELECT * FROM knowledge_document")
    List<KnowledgeDocument> findAll();

    @Select("""
            <script>
            SELECT * FROM knowledge_document WHERE doc_id IN
            <foreach collection='docIds' item='docId' open='(' separator=',' close=')'>
                #{docId}
            </foreach>
            </script>
            """)
    List<KnowledgeDocument> findByDocIds(@Param("docIds") List<String> docIds);
}
