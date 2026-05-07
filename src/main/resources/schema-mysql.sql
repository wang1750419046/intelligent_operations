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
    embedding_status VARCHAR(32) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
ALTER TABLE knowledge_document CONVERT TO CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

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
