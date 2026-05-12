ALTER DATABASE ai_ops_agent CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS chat_session (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    session_id VARCHAR(64) NOT NULL UNIQUE,
    user_id VARCHAR(64) NOT NULL,
    title VARCHAR(255) NOT NULL,
    model_config_id BIGINT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
ALTER TABLE chat_session CONVERT TO CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS chat_message (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    message_id VARCHAR(64) NOT NULL UNIQUE,
    session_id VARCHAR(64) NOT NULL,
    scope VARCHAR(32) NOT NULL,
    role VARCHAR(32) NOT NULL,
    content TEXT NOT NULL,
    message_json LONGTEXT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_chat_message_session_scope_created (session_id, scope, created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
ALTER TABLE chat_message CONVERT TO CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS agent_trace (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    trace_id VARCHAR(64) NOT NULL,
    session_id VARCHAR(64) NOT NULL,
    step_no INT NOT NULL,
    thought_summary VARCHAR(255) NULL,
    action_name VARCHAR(128) NULL,
    action_params TEXT NULL,
    observation TEXT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_agent_trace_trace_step (trace_id, step_no)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
ALTER TABLE agent_trace CONVERT TO CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS knowledge_document (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    doc_id VARCHAR(64) NOT NULL UNIQUE,
    title VARCHAR(255) NOT NULL,
    content TEXT NOT NULL,
    source VARCHAR(128) NOT NULL,
    tags VARCHAR(255) NULL,
    filename VARCHAR(255) NULL,
    mime_type VARCHAR(128) NULL,
    country VARCHAR(64) NULL,
    business_line VARCHAR(128) NULL,
    system_name VARCHAR(128) NULL,
    event_time TIMESTAMP NULL,
    permission_codes VARCHAR(512) NULL,
    content_hash VARCHAR(64) NULL,
    index_status VARCHAR(32) NOT NULL DEFAULT 'PENDING',
    embedding_status VARCHAR(32) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
ALTER TABLE knowledge_document CONVERT TO CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

SET @add_kb_filename_sql = (
    SELECT IF(COUNT(*) = 0, 'ALTER TABLE knowledge_document ADD COLUMN filename VARCHAR(255) NULL AFTER tags', 'SELECT 1')
    FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'knowledge_document' AND COLUMN_NAME = 'filename'
);
PREPARE add_kb_filename_stmt FROM @add_kb_filename_sql;
EXECUTE add_kb_filename_stmt;
DEALLOCATE PREPARE add_kb_filename_stmt;

SET @add_kb_mime_type_sql = (
    SELECT IF(COUNT(*) = 0, 'ALTER TABLE knowledge_document ADD COLUMN mime_type VARCHAR(128) NULL AFTER filename', 'SELECT 1')
    FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'knowledge_document' AND COLUMN_NAME = 'mime_type'
);
PREPARE add_kb_mime_type_stmt FROM @add_kb_mime_type_sql;
EXECUTE add_kb_mime_type_stmt;
DEALLOCATE PREPARE add_kb_mime_type_stmt;

SET @add_kb_country_sql = (
    SELECT IF(COUNT(*) = 0, 'ALTER TABLE knowledge_document ADD COLUMN country VARCHAR(64) NULL AFTER mime_type', 'SELECT 1')
    FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'knowledge_document' AND COLUMN_NAME = 'country'
);
PREPARE add_kb_country_stmt FROM @add_kb_country_sql;
EXECUTE add_kb_country_stmt;
DEALLOCATE PREPARE add_kb_country_stmt;

SET @add_kb_business_line_sql = (
    SELECT IF(COUNT(*) = 0, 'ALTER TABLE knowledge_document ADD COLUMN business_line VARCHAR(128) NULL AFTER country', 'SELECT 1')
    FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'knowledge_document' AND COLUMN_NAME = 'business_line'
);
PREPARE add_kb_business_line_stmt FROM @add_kb_business_line_sql;
EXECUTE add_kb_business_line_stmt;
DEALLOCATE PREPARE add_kb_business_line_stmt;

SET @add_kb_system_name_sql = (
    SELECT IF(COUNT(*) = 0, 'ALTER TABLE knowledge_document ADD COLUMN system_name VARCHAR(128) NULL AFTER business_line', 'SELECT 1')
    FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'knowledge_document' AND COLUMN_NAME = 'system_name'
);
PREPARE add_kb_system_name_stmt FROM @add_kb_system_name_sql;
EXECUTE add_kb_system_name_stmt;
DEALLOCATE PREPARE add_kb_system_name_stmt;

SET @add_kb_event_time_sql = (
    SELECT IF(COUNT(*) = 0, 'ALTER TABLE knowledge_document ADD COLUMN event_time TIMESTAMP NULL AFTER system_name', 'SELECT 1')
    FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'knowledge_document' AND COLUMN_NAME = 'event_time'
);
PREPARE add_kb_event_time_stmt FROM @add_kb_event_time_sql;
EXECUTE add_kb_event_time_stmt;
DEALLOCATE PREPARE add_kb_event_time_stmt;

SET @add_kb_permission_codes_sql = (
    SELECT IF(COUNT(*) = 0, 'ALTER TABLE knowledge_document ADD COLUMN permission_codes VARCHAR(512) NULL AFTER event_time', 'SELECT 1')
    FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'knowledge_document' AND COLUMN_NAME = 'permission_codes'
);
PREPARE add_kb_permission_codes_stmt FROM @add_kb_permission_codes_sql;
EXECUTE add_kb_permission_codes_stmt;
DEALLOCATE PREPARE add_kb_permission_codes_stmt;

SET @add_kb_content_hash_sql = (
    SELECT IF(COUNT(*) = 0, 'ALTER TABLE knowledge_document ADD COLUMN content_hash VARCHAR(64) NULL AFTER permission_codes', 'SELECT 1')
    FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'knowledge_document' AND COLUMN_NAME = 'content_hash'
);
PREPARE add_kb_content_hash_stmt FROM @add_kb_content_hash_sql;
EXECUTE add_kb_content_hash_stmt;
DEALLOCATE PREPARE add_kb_content_hash_stmt;

SET @add_kb_index_status_sql = (
    SELECT IF(COUNT(*) = 0, 'ALTER TABLE knowledge_document ADD COLUMN index_status VARCHAR(32) NOT NULL DEFAULT ''PENDING'' AFTER content_hash', 'SELECT 1')
    FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'knowledge_document' AND COLUMN_NAME = 'index_status'
);
PREPARE add_kb_index_status_stmt FROM @add_kb_index_status_sql;
EXECUTE add_kb_index_status_stmt;
DEALLOCATE PREPARE add_kb_index_status_stmt;

SET @add_kb_updated_at_sql = (
    SELECT IF(COUNT(*) = 0, 'ALTER TABLE knowledge_document ADD COLUMN updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP AFTER created_at', 'SELECT 1')
    FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'knowledge_document' AND COLUMN_NAME = 'updated_at'
);
PREPARE add_kb_updated_at_stmt FROM @add_kb_updated_at_sql;
EXECUTE add_kb_updated_at_stmt;
DEALLOCATE PREPARE add_kb_updated_at_stmt;

CREATE TABLE IF NOT EXISTS knowledge_chunk (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    chunk_id VARCHAR(96) NOT NULL UNIQUE,
    doc_id VARCHAR(64) NOT NULL,
    chunk_index INT NOT NULL,
    section_path VARCHAR(512) NULL,
    chunk_content TEXT NOT NULL,
    content_hash VARCHAR(64) NOT NULL,
    embedding_status VARCHAR(32) NOT NULL DEFAULT 'PENDING',
    embedding_model VARCHAR(128) NULL,
    indexed_at TIMESTAMP NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_knowledge_chunk_doc (doc_id),
    INDEX idx_knowledge_chunk_status (embedding_status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
ALTER TABLE knowledge_chunk CONVERT TO CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS llm_config (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    name VARCHAR(128) NOT NULL,
    provider VARCHAR(32) NOT NULL,
    config_type VARCHAR(32) NOT NULL DEFAULT 'CHAT',
    base_url VARCHAR(255) NOT NULL,
    api_key VARCHAR(255) NULL,
    model_name VARCHAR(128) NOT NULL,
    temperature DECIMAL(4,2) NOT NULL DEFAULT 0.20,
    max_tokens INT NULL,
    enabled TINYINT NOT NULL DEFAULT 1,
    is_default TINYINT NOT NULL DEFAULT 0,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
SET @add_config_type_sql = (
    SELECT IF(
        COUNT(*) = 0,
        'ALTER TABLE llm_config ADD COLUMN config_type VARCHAR(32) NOT NULL DEFAULT ''CHAT'' AFTER provider',
        'SELECT 1'
    )
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'llm_config'
      AND COLUMN_NAME = 'config_type'
);
PREPARE add_config_type_stmt FROM @add_config_type_sql;
EXECUTE add_config_type_stmt;
DEALLOCATE PREPARE add_config_type_stmt;
ALTER TABLE llm_config CONVERT TO CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
