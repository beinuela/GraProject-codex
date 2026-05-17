CREATE TABLE IF NOT EXISTS ai_analysis_task (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    biz_type VARCHAR(50) NOT NULL,
    biz_id BIGINT NOT NULL,
    request_snapshot TEXT NOT NULL,
    status VARCHAR(30) NOT NULL DEFAULT 'PENDING',
    result_source VARCHAR(30) NOT NULL DEFAULT 'RULE_FALLBACK',
    result_json TEXT,
    error_message VARCHAR(500),
    created_by BIGINT,
    started_at DATETIME,
    finished_at DATETIME,
    deleted TINYINT NOT NULL DEFAULT 0,
    version INT NOT NULL DEFAULT 0,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

CREATE INDEX idx_ai_task_biz ON ai_analysis_task (biz_type, biz_id);
CREATE INDEX idx_ai_task_status ON ai_analysis_task (status);
CREATE INDEX idx_ai_task_created ON ai_analysis_task (created_at);

CREATE TABLE IF NOT EXISTS ai_call_log (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    task_id BIGINT NOT NULL,
    provider_name VARCHAR(50) NOT NULL,
    model_name VARCHAR(100) NOT NULL,
    prompt_template_code VARCHAR(100) NOT NULL,
    prompt_tokens INT NOT NULL DEFAULT 0,
    completion_tokens INT NOT NULL DEFAULT 0,
    total_tokens INT NOT NULL DEFAULT 0,
    latency_ms BIGINT NOT NULL DEFAULT 0,
    success_flag TINYINT NOT NULL DEFAULT 0,
    error_message VARCHAR(500),
    deleted TINYINT NOT NULL DEFAULT 0,
    version INT NOT NULL DEFAULT 0,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

CREATE INDEX idx_ai_call_task ON ai_call_log (task_id);
CREATE INDEX idx_ai_call_created ON ai_call_log (created_at);
