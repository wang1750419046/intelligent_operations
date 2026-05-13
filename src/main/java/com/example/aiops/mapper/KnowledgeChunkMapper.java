package com.example.aiops.mapper;

import com.example.aiops.entity.KnowledgeChunk;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.time.LocalDateTime;
import java.util.List;

@Mapper
public interface KnowledgeChunkMapper {

    @Insert("""
            INSERT INTO knowledge_chunk(chunk_id, doc_id, chunk_index, section_path, chunk_content, content_hash,
                                        embedding_status, embedding_model, indexed_at, created_at, updated_at)
            VALUES(#{chunkId}, #{docId}, #{chunkIndex}, #{sectionPath}, #{chunkContent}, #{contentHash},
                   #{embeddingStatus}, #{embeddingModel}, #{indexedAt}, #{createdAt}, #{updatedAt})
            """)
    int insert(KnowledgeChunk chunk);

    @Select("""
            SELECT c.*, d.title, d.source, d.tags, d.country, d.business_line, d.system_name, d.event_time, d.permission_codes
            FROM knowledge_chunk c
            JOIN knowledge_document d ON d.doc_id = c.doc_id
            WHERE c.doc_id = #{docId}
            ORDER BY c.chunk_index ASC
            """)
    List<KnowledgeChunk> findByDocId(@Param("docId") String docId);

    @Select("""
            <script>
            SELECT c.*, d.title, d.source, d.tags, d.country, d.business_line, d.system_name, d.event_time, d.permission_codes
            FROM knowledge_chunk c
            JOIN knowledge_document d ON d.doc_id = c.doc_id
            WHERE c.chunk_id IN
            <foreach collection='chunkIds' item='chunkId' open='(' separator=',' close=')'>
                #{chunkId}
            </foreach>
            </script>
            """)
    List<KnowledgeChunk> findByChunkIds(@Param("chunkIds") List<String> chunkIds);

    @Select("""
            <script>
            SELECT c.*, d.title, d.source, d.tags, d.country, d.business_line, d.system_name, d.event_time, d.permission_codes
            FROM knowledge_chunk c
            JOIN knowledge_document d ON d.doc_id = c.doc_id
            <where>
                <if test='query != null and query != ""'>
                    AND (c.chunk_content LIKE CONCAT('%', #{query}, '%')
                         OR d.title LIKE CONCAT('%', #{query}, '%')
                         OR d.tags LIKE CONCAT('%', #{query}, '%'))
                </if>
                <if test='country != null and country != ""'>AND d.country = #{country}</if>
                <if test='businessLine != null and businessLine != ""'>AND d.business_line = #{businessLine}</if>
                <if test='systemName != null and systemName != ""'>AND d.system_name = #{systemName}</if>
                <if test='startTime != null'>AND (d.event_time IS NULL OR d.event_time &gt;= #{startTime})</if>
                <if test='endTime != null'>AND (d.event_time IS NULL OR d.event_time &lt;= #{endTime})</if>
                <if test='permissionCodes != null and permissionCodes.size() > 0'>
                    AND (
                        d.permission_codes IS NULL OR d.permission_codes = ''
                        <foreach collection='permissionCodes' item='code'>
                            OR d.permission_codes LIKE CONCAT('%', #{code}, '%')
                        </foreach>
                    )
                </if>
            </where>
            ORDER BY c.updated_at DESC, c.id DESC
            LIMIT #{limit}
            </script>
            """)
    List<KnowledgeChunk> searchKeyword(@Param("query") String query,
                                       @Param("country") String country,
                                       @Param("businessLine") String businessLine,
                                       @Param("systemName") String systemName,
                                       @Param("startTime") LocalDateTime startTime,
                                       @Param("endTime") LocalDateTime endTime,
                                       @Param("permissionCodes") List<String> permissionCodes,
                                       @Param("limit") int limit);

    @Update("""
            UPDATE knowledge_chunk
            SET embedding_status = #{embeddingStatus},
                embedding_model = #{embeddingModel},
                indexed_at = #{indexedAt},
                updated_at = #{updatedAt}
            WHERE chunk_id = #{chunkId}
            """)
    int updateEmbeddingStatus(@Param("chunkId") String chunkId,
                              @Param("embeddingStatus") String embeddingStatus,
                              @Param("embeddingModel") String embeddingModel,
                              @Param("indexedAt") LocalDateTime indexedAt,
                              @Param("updatedAt") LocalDateTime updatedAt);

    @Select("SELECT COUNT(*) FROM knowledge_chunk WHERE doc_id = #{docId}")
    int countByDocId(@Param("docId") String docId);

    @Delete("DELETE FROM knowledge_chunk WHERE doc_id = #{docId}")
    int deleteByDocId(@Param("docId") String docId);
}
