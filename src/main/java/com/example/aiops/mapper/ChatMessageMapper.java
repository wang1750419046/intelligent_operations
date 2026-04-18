package com.example.aiops.mapper;

import com.example.aiops.entity.ChatMessage;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface ChatMessageMapper {

    @Insert("""
            INSERT INTO chat_message(message_id, session_id, scope, role, content, message_json, created_at)
            VALUES(#{messageId}, #{sessionId}, #{scope}, #{role}, #{content}, #{messageJson}, #{createdAt})
            """)
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(ChatMessage message);

    @Select("""
            SELECT * FROM chat_message
            WHERE session_id = #{sessionId} AND scope = #{scope}
            ORDER BY created_at ASC, id ASC
            """)
    List<ChatMessage> findBySessionIdAndScope(@Param("sessionId") String sessionId, @Param("scope") String scope);

    @Select("""
            SELECT * FROM chat_message
            WHERE session_id = #{sessionId} AND scope = #{scope}
            ORDER BY created_at DESC, id DESC
            LIMIT #{limit}
            """)
    List<ChatMessage> findLatestBySessionIdAndScope(@Param("sessionId") String sessionId, @Param("scope") String scope, @Param("limit") int limit);

    @Delete("DELETE FROM chat_message WHERE session_id = #{sessionId}")
    int deleteBySessionId(String sessionId);

    @Delete("DELETE FROM chat_message WHERE session_id = #{sessionId} AND scope = #{scope}")
    int deleteBySessionIdAndScope(@Param("sessionId") String sessionId, @Param("scope") String scope);
}
