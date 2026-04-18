package com.example.aiops.mapper;

import com.example.aiops.entity.ChatSession;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Result;
import org.apache.ibatis.annotations.ResultMap;
import org.apache.ibatis.annotations.Results;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;
import java.util.Optional;

@Mapper
public interface ChatSessionMapper {

    @Insert("""
            INSERT INTO chat_session(session_id, user_id, title, model_config_id, created_at, updated_at)
            VALUES(#{sessionId}, #{userId}, #{title}, #{modelConfigId}, #{createdAt}, #{updatedAt})
            """)
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(ChatSession session);

    @Select("SELECT * FROM chat_session WHERE session_id = #{sessionId}")
    @Results(id = "chatSessionMap", value = {
            @Result(property = "modelConfigId", column = "model_config_id")
    })
    ChatSession findOne(String sessionId);

    default Optional<ChatSession> findOptional(String sessionId) {
        return Optional.ofNullable(findOne(sessionId));
    }

    @Select("SELECT * FROM chat_session ORDER BY updated_at DESC")
    @ResultMap("chatSessionMap")
    List<ChatSession> findAll();

    @Update("""
            UPDATE chat_session
            SET title = #{title},
                model_config_id = #{modelConfigId},
                updated_at = #{updatedAt}
            WHERE session_id = #{sessionId}
            """)
    int update(ChatSession session);

    @Delete("DELETE FROM chat_session WHERE session_id = #{sessionId}")
    int deleteBySessionId(String sessionId);
}
