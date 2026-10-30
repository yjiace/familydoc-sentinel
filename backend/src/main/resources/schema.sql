-- 家医智核·流言粉碎 用户体系与持久化数据表结构 (PostgreSQL)

-- 1. 用户表
CREATE TABLE IF NOT EXISTS sys_user (
    id BIGSERIAL PRIMARY KEY,
    email VARCHAR(120) NOT NULL,
    password VARCHAR(100) NOT NULL,
    nickname VARCHAR(50) NOT NULL,
    avatar VARCHAR(255),
    role VARCHAR(20) NOT NULL DEFAULT 'USER',
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE UNIQUE INDEX IF NOT EXISTS uk_sys_user_email ON sys_user(email);

-- 3. 用户核验任务历史表（服务端持久化核验快照）
CREATE TABLE IF NOT EXISTS user_history_task (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES sys_user(id) ON DELETE CASCADE,
    task_id VARCHAR(64) NOT NULL,
    original_text TEXT NOT NULL,
    status VARCHAR(30) NOT NULL,
    summary TEXT,
    claim_count INT NOT NULL DEFAULT 0,
    claims_json TEXT,
    red_flag_level VARCHAR(20),
    tokens_used INT NOT NULL DEFAULT 0,
    search_used INT NOT NULL DEFAULT 0,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

ALTER TABLE user_history_task ADD COLUMN IF NOT EXISTS tokens_used INT NOT NULL DEFAULT 0;
ALTER TABLE user_history_task ADD COLUMN IF NOT EXISTS search_used INT NOT NULL DEFAULT 0;

CREATE UNIQUE INDEX IF NOT EXISTS uk_user_history_task_id ON user_history_task(task_id);
CREATE INDEX IF NOT EXISTS idx_user_history_user_created ON user_history_task(user_id, created_at DESC);

-- 4. 用户用量累计统计表 (Token与元宝搜索，含日/周/月双链额度统计)
CREATE TABLE IF NOT EXISTS sys_user_usage_stat (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL UNIQUE REFERENCES sys_user(id) ON DELETE CASCADE,
    total_prompt_tokens BIGINT NOT NULL DEFAULT 0,
    total_completion_tokens BIGINT NOT NULL DEFAULT 0,
    total_tokens BIGINT NOT NULL DEFAULT 0,
    total_search_count BIGINT NOT NULL DEFAULT 0,
    task_count INT NOT NULL DEFAULT 0,
    daily_tokens BIGINT NOT NULL DEFAULT 0,
    weekly_tokens BIGINT NOT NULL DEFAULT 0,
    monthly_tokens BIGINT NOT NULL DEFAULT 0,
    daily_search_count BIGINT NOT NULL DEFAULT 0,
    weekly_search_count BIGINT NOT NULL DEFAULT 0,
    monthly_search_count BIGINT NOT NULL DEFAULT 0,
    daily_period_date DATE,
    weekly_period_date DATE,
    monthly_period_date DATE,
    daily_token_limit BIGINT,
    weekly_token_limit BIGINT,
    monthly_token_limit BIGINT,
    daily_search_limit BIGINT,
    weekly_search_limit BIGINT,
    monthly_search_limit BIGINT,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

ALTER TABLE sys_user_usage_stat ADD COLUMN IF NOT EXISTS daily_tokens BIGINT NOT NULL DEFAULT 0;
ALTER TABLE sys_user_usage_stat ADD COLUMN IF NOT EXISTS weekly_tokens BIGINT NOT NULL DEFAULT 0;
ALTER TABLE sys_user_usage_stat ADD COLUMN IF NOT EXISTS monthly_tokens BIGINT NOT NULL DEFAULT 0;
ALTER TABLE sys_user_usage_stat ADD COLUMN IF NOT EXISTS daily_search_count BIGINT NOT NULL DEFAULT 0;
ALTER TABLE sys_user_usage_stat ADD COLUMN IF NOT EXISTS weekly_search_count BIGINT NOT NULL DEFAULT 0;
ALTER TABLE sys_user_usage_stat ADD COLUMN IF NOT EXISTS monthly_search_count BIGINT NOT NULL DEFAULT 0;
ALTER TABLE sys_user_usage_stat ADD COLUMN IF NOT EXISTS daily_period_date DATE;
ALTER TABLE sys_user_usage_stat ADD COLUMN IF NOT EXISTS weekly_period_date DATE;
ALTER TABLE sys_user_usage_stat ADD COLUMN IF NOT EXISTS monthly_period_date DATE;
ALTER TABLE sys_user_usage_stat ADD COLUMN IF NOT EXISTS daily_token_limit BIGINT;
ALTER TABLE sys_user_usage_stat ADD COLUMN IF NOT EXISTS weekly_token_limit BIGINT;
ALTER TABLE sys_user_usage_stat ADD COLUMN IF NOT EXISTS monthly_token_limit BIGINT;
ALTER TABLE sys_user_usage_stat ADD COLUMN IF NOT EXISTS daily_search_limit BIGINT;
ALTER TABLE sys_user_usage_stat ADD COLUMN IF NOT EXISTS weekly_search_limit BIGINT;
ALTER TABLE sys_user_usage_stat ADD COLUMN IF NOT EXISTS monthly_search_limit BIGINT;

-- 5. 服务调用审计流水日志表
CREATE TABLE IF NOT EXISTS sys_usage_log (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT REFERENCES sys_user(id) ON DELETE SET NULL,
    task_id VARCHAR(64),
    service_type VARCHAR(30) NOT NULL,
    call_action VARCHAR(50) NOT NULL,
    prompt_tokens INT NOT NULL DEFAULT 0,
    completion_tokens INT NOT NULL DEFAULT 0,
    total_tokens INT NOT NULL DEFAULT 0,
    search_count INT NOT NULL DEFAULT 0,
    cost_time_ms BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_usage_log_user_created ON sys_usage_log(user_id, created_at DESC);
CREATE INDEX IF NOT EXISTS idx_usage_log_task ON sys_usage_log(task_id);

