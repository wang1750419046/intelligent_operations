package com.example.aiops.mapper;

import com.example.aiops.entity.AgentTraceRecord;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface AgentTraceMapper {

    @Insert("""
            INSERT INTO agent_trace(trace_id, session_id, step_no, thought_summary, action_name, action_params, observation, created_at)
            VALUES(#{traceId}, #{sessionId}, #{stepNo}, #{thoughtSummary}, #{actionName}, #{actionParams}, #{observation}, #{createdAt})
            """)
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(AgentTraceRecord record);

    @Select("SELECT * FROM agent_trace WHERE trace_id = #{traceId} ORDER BY step_no ASC, id ASC")
    List<AgentTraceRecord> findByTraceId(@Param("traceId") String traceId);

    @Delete("DELETE FROM agent_trace WHERE session_id = #{sessionId}")
    int deleteBySessionId(@Param("sessionId") String sessionId);
}
