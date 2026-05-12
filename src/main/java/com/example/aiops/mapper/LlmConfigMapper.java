package com.example.aiops.mapper;

import com.example.aiops.entity.LlmConfig;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Result;
import org.apache.ibatis.annotations.ResultMap;
import org.apache.ibatis.annotations.Results;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

@Mapper
public interface LlmConfigMapper {

    @Insert("""
            INSERT INTO llm_config(name, provider, config_type, base_url, api_key, model_name, temperature, max_tokens, enabled, is_default, created_at, updated_at)
            VALUES(#{name}, #{provider}, #{configType}, #{baseUrl}, #{apiKey}, #{modelName}, #{temperature}, #{maxTokens}, #{enabled}, #{defaultConfig}, #{createdAt}, #{updatedAt})
            """)
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(LlmConfig config);

    @Update("""
            UPDATE llm_config
            SET name = #{name},
                provider = #{provider},
                config_type = #{configType},
                base_url = #{baseUrl},
                api_key = #{apiKey},
                model_name = #{modelName},
                temperature = #{temperature},
                max_tokens = #{maxTokens},
                enabled = #{enabled},
                is_default = #{defaultConfig},
                updated_at = #{updatedAt}
            WHERE id = #{id}
            """)
    int update(LlmConfig config);

    @Select("SELECT * FROM llm_config WHERE id = #{id}")
    @Results(id = "llmConfigMap", value = {
            @Result(property = "baseUrl", column = "base_url"),
            @Result(property = "configType", column = "config_type"),
            @Result(property = "modelName", column = "model_name"),
            @Result(property = "maxTokens", column = "max_tokens"),
            @Result(property = "defaultConfig", column = "is_default")
    })
    LlmConfig findById(Long id);

    @Select("SELECT * FROM llm_config WHERE enabled = 1 ORDER BY is_default DESC, updated_at DESC, id DESC")
    @ResultMap("llmConfigMap")
    List<LlmConfig> findEnabledList();

    @Select("SELECT * FROM llm_config ORDER BY is_default DESC, updated_at DESC, id DESC")
    @ResultMap("llmConfigMap")
    List<LlmConfig> findAll();

    @Select("SELECT * FROM llm_config WHERE config_type = #{configType} ORDER BY is_default DESC, updated_at DESC, id DESC")
    @ResultMap("llmConfigMap")
    List<LlmConfig> findByConfigType(@Param("configType") String configType);

    @Select("SELECT * FROM llm_config WHERE config_type = 'CHAT' AND is_default = 1 LIMIT 1")
    @ResultMap("llmConfigMap")
    LlmConfig findDefault();

    @Select("SELECT * FROM llm_config WHERE config_type = 'EMBEDDING' AND enabled = 1 ORDER BY is_default DESC, updated_at DESC, id DESC LIMIT 1")
    @ResultMap("llmConfigMap")
    LlmConfig findActiveEmbedding();

    @Select("SELECT * FROM llm_config WHERE config_type = 'RERANK' AND enabled = 1 ORDER BY is_default DESC, updated_at DESC, id DESC LIMIT 1")
    @ResultMap("llmConfigMap")
    LlmConfig findActiveRerank();

    @Update("UPDATE llm_config SET is_default = 0 WHERE config_type = #{configType}")
    int clearDefaultFlag(@Param("configType") String configType);

    @Delete("DELETE FROM llm_config WHERE id = #{id}")
    int deleteById(Long id);
}
