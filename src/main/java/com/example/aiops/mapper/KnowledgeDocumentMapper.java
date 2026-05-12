package com.example.aiops.mapper;

import com.example.aiops.entity.KnowledgeDocument;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;
import java.time.LocalDateTime;

@Mapper
public interface KnowledgeDocumentMapper {

    @Insert("""
            INSERT INTO knowledge_document(doc_id, title, content, source, tags, filename, mime_type, country, business_line,
                                           system_name, event_time, permission_codes, content_hash, index_status, embedding_status,
                                           created_at, updated_at)
            VALUES(#{docId}, #{title}, #{content}, #{source}, #{tags}, #{filename}, #{mimeType}, #{country}, #{businessLine},
                   #{systemName}, #{eventTime}, #{permissionCodes}, #{contentHash}, #{indexStatus}, #{embeddingStatus},
                   #{createdAt}, #{updatedAt})
            """)
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(KnowledgeDocument document);

    @Update("""
            UPDATE knowledge_document
            SET title = #{title},
                content = #{content},
                source = #{source},
                tags = #{tags},
                filename = #{filename},
                mime_type = #{mimeType},
                country = #{country},
                business_line = #{businessLine},
                system_name = #{systemName},
                event_time = #{eventTime},
                permission_codes = #{permissionCodes},
                content_hash = #{contentHash},
                index_status = #{indexStatus},
                embedding_status = #{embeddingStatus},
                updated_at = #{updatedAt}
            WHERE doc_id = #{docId}
            """)
    int updateByDocId(KnowledgeDocument document);

    @Update("""
            UPDATE knowledge_document
            SET index_status = #{indexStatus},
                embedding_status = #{embeddingStatus},
                updated_at = #{updatedAt}
            WHERE doc_id = #{docId}
            """)
    int updateIndexStatus(@Param("docId") String docId,
                          @Param("indexStatus") String indexStatus,
                          @Param("embeddingStatus") String embeddingStatus,
                          @Param("updatedAt") LocalDateTime updatedAt);

    @Select("SELECT * FROM knowledge_document ORDER BY updated_at DESC, id DESC")
    List<KnowledgeDocument> findAll();

    @Select("SELECT * FROM knowledge_document WHERE doc_id = #{docId}")
    KnowledgeDocument findByDocId(@Param("docId") String docId);

    @Select("""
            <script>
            SELECT * FROM knowledge_document WHERE doc_id IN
            <foreach collection='docIds' item='docId' open='(' separator=',' close=')'>
                #{docId}
            </foreach>
            </script>
            """)
    List<KnowledgeDocument> findByDocIds(@Param("docIds") List<String> docIds);

    @Select("""
            <script>
            SELECT * FROM knowledge_document
            <where>
                <if test='keyword != null and keyword != ""'>
                    AND (title LIKE CONCAT('%', #{keyword}, '%')
                         OR content LIKE CONCAT('%', #{keyword}, '%')
                         OR tags LIKE CONCAT('%', #{keyword}, '%'))
                </if>
                <if test='source != null and source != ""'>AND source = #{source}</if>
                <if test='country != null and country != ""'>AND country = #{country}</if>
                <if test='businessLine != null and businessLine != ""'>AND business_line = #{businessLine}</if>
                <if test='systemName != null and systemName != ""'>AND system_name = #{systemName}</if>
                <if test='startTime != null'>AND (event_time IS NULL OR event_time &gt;= #{startTime})</if>
                <if test='endTime != null'>AND (event_time IS NULL OR event_time &lt;= #{endTime})</if>
                <if test='permissionCodes != null and permissionCodes.size() > 0'>
                    AND (
                        permission_codes IS NULL OR permission_codes = ''
                        <foreach collection='permissionCodes' item='code'>
                            OR permission_codes LIKE CONCAT('%', #{code}, '%')
                        </foreach>
                    )
                </if>
            </where>
            ORDER BY updated_at DESC, id DESC
            </script>
            """)
    List<KnowledgeDocument> searchDocuments(@Param("keyword") String keyword,
                                            @Param("source") String source,
                                            @Param("country") String country,
                                            @Param("businessLine") String businessLine,
                                            @Param("systemName") String systemName,
                                            @Param("startTime") LocalDateTime startTime,
                                            @Param("endTime") LocalDateTime endTime,
                                            @Param("permissionCodes") List<String> permissionCodes);

    @Delete("DELETE FROM knowledge_document WHERE doc_id = #{docId}")
    int deleteByDocId(@Param("docId") String docId);
}
