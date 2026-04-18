INSERT INTO knowledge_document (doc_id, title, content, source, tags, embedding_status)
SELECT 'kb-001', '数据库连接池耗尽排查', '当 RT 持续升高且日志出现 connection timeout 时，优先检查连接池配置、慢 SQL 与数据库负载。', 'wiki', 'database,timeout,rt', 'READY'
WHERE NOT EXISTS (SELECT 1 FROM knowledge_document WHERE doc_id = 'kb-001');

INSERT INTO knowledge_document (doc_id, title, content, source, tags, embedding_status)
SELECT 'kb-002', '发布后接口变慢复盘模板', '若异常时间与发布窗口重叠，需要检查变更内容、线程池参数与下游依赖版本。', 'postmortem', 'release,latency,dependency', 'READY'
WHERE NOT EXISTS (SELECT 1 FROM knowledge_document WHERE doc_id = 'kb-002');

INSERT INTO knowledge_document (doc_id, title, content, source, tags, embedding_status)
SELECT 'kb-003', '线程池耗尽处理手册', '若日志存在 RejectedExecutionException 或 queue full，说明业务线程池可能耗尽。', 'runbook', 'threadpool,queue,reject', 'READY'
WHERE NOT EXISTS (SELECT 1 FROM knowledge_document WHERE doc_id = 'kb-003');

INSERT INTO llm_config (name, provider, base_url, api_key, model_name, temperature, max_tokens, enabled, is_default)
SELECT 'OpenAI GPT 默认', 'OPENAI', 'https://api.openai.com/v1', '', 'gpt-4.1-mini', 0.20, 4096, 1, 1
WHERE NOT EXISTS (SELECT 1 FROM llm_config WHERE name = 'OpenAI GPT 默认');

INSERT INTO llm_config (name, provider, base_url, api_key, model_name, temperature, max_tokens, enabled, is_default)
SELECT 'Qwen 默认', 'QWEN', 'https://dashscope.aliyuncs.com/compatible-mode/v1', '', 'qwen-plus', 0.20, 4096, 1, 0
WHERE NOT EXISTS (SELECT 1 FROM llm_config WHERE name = 'Qwen 默认');

INSERT INTO llm_config (name, provider, base_url, api_key, model_name, temperature, max_tokens, enabled, is_default)
SELECT 'MiniMax 默认', 'MINIMAX', 'https://api.minimaxi.com/v1', '', 'MiniMax-M2.5', 0.20, 4096, 1, 0
WHERE NOT EXISTS (SELECT 1 FROM llm_config WHERE name = 'MiniMax 默认');
