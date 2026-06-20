CREATE TABLE IF NOT EXISTS schema_migrations (
    version VARCHAR(64) PRIMARY KEY,
    applied_at BIGINT NOT NULL
);

CREATE TABLE IF NOT EXISTS memory_records (
    id VARCHAR(80) PRIMARY KEY,
    created_at BIGINT NOT NULL,
    server_id VARCHAR(128) NOT NULL,
    world_id VARCHAR(256) NOT NULL,
    minecraft_player_uuid VARCHAR(80) NOT NULL,
    npc_key VARCHAR(256) NOT NULL,
    entity_uuid VARCHAR(80),
    conversation_id VARCHAR(128) NOT NULL,
    dialogue_id VARCHAR(256),
    source_node_id VARCHAR(128),
    role VARCHAR(32) NOT NULL,
    text CLOB NOT NULL,
    citation_id VARCHAR(160) NOT NULL,
    embedding CLOB NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_memory_records_context ON memory_records(server_id, world_id, minecraft_player_uuid, npc_key, created_at);
CREATE INDEX IF NOT EXISTS idx_memory_records_conversation ON memory_records(conversation_id, created_at);

CREATE TABLE IF NOT EXISTS memory_facts (
    id VARCHAR(80) PRIMARY KEY,
    record_id VARCHAR(80) NOT NULL,
    created_at BIGINT NOT NULL,
    server_id VARCHAR(128) NOT NULL,
    world_id VARCHAR(256) NOT NULL,
    subject VARCHAR(256) NOT NULL,
    predicate VARCHAR(256) NOT NULL,
    fact_value CLOB NOT NULL,
    status VARCHAR(32) NOT NULL,
    superseded_by VARCHAR(80),
    citation_id VARCHAR(160) NOT NULL,
    embedding CLOB NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_memory_facts_key ON memory_facts(server_id, world_id, subject, predicate, status);
ALTER TABLE memory_facts ADD COLUMN IF NOT EXISTS source_type VARCHAR(64) DEFAULT 'LLM_INFERRED';
ALTER TABLE memory_facts ADD COLUMN IF NOT EXISTS authority_rank INT DEFAULT 20;
ALTER TABLE memory_facts ADD COLUMN IF NOT EXISTS certainty DOUBLE DEFAULT 0.5;
ALTER TABLE memory_facts ADD COLUMN IF NOT EXISTS visibility VARCHAR(64) DEFAULT 'private';
ALTER TABLE memory_facts ADD COLUMN IF NOT EXISTS valid_from BIGINT DEFAULT 0;
ALTER TABLE memory_facts ADD COLUMN IF NOT EXISTS valid_to BIGINT DEFAULT 0;
ALTER TABLE memory_facts ADD COLUMN IF NOT EXISTS world_tick BIGINT DEFAULT 0;
ALTER TABLE memory_facts ADD COLUMN IF NOT EXISTS mc_day BIGINT DEFAULT 0;
ALTER TABLE memory_facts ADD COLUMN IF NOT EXISTS created_by VARCHAR(128) DEFAULT 'legacy_memory_store';
ALTER TABLE memory_facts ADD COLUMN IF NOT EXISTS updated_by VARCHAR(128) DEFAULT 'legacy_memory_store';

CREATE TABLE IF NOT EXISTS memory_conflicts (
    id VARCHAR(80) PRIMARY KEY,
    created_at BIGINT NOT NULL,
    server_id VARCHAR(128) NOT NULL,
    world_id VARCHAR(256) NOT NULL,
    subject VARCHAR(256) NOT NULL,
    predicate VARCHAR(256) NOT NULL,
    old_fact_id VARCHAR(80) NOT NULL,
    new_fact_id VARCHAR(80) NOT NULL,
    old_fact_value CLOB NOT NULL,
    new_fact_value CLOB NOT NULL,
    citation_ids CLOB NOT NULL,
    status VARCHAR(32) NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_memory_conflicts_context ON memory_conflicts(server_id, world_id, subject, predicate, created_at);

ALTER TABLE memory_records ADD COLUMN IF NOT EXISTS summary CLOB DEFAULT '';
ALTER TABLE memory_records ADD COLUMN IF NOT EXISTS summary_updated_at BIGINT DEFAULT 0;
ALTER TABLE memory_records ADD COLUMN IF NOT EXISTS related_memory_ids CLOB DEFAULT '';

CREATE TABLE IF NOT EXISTS memory_operations (
    id VARCHAR(80) PRIMARY KEY,
    created_at BIGINT NOT NULL,
    server_id VARCHAR(128) NOT NULL,
    world_id VARCHAR(256) NOT NULL,
    record_id VARCHAR(80) NOT NULL,
    op_type VARCHAR(64) NOT NULL,
    subject VARCHAR(256) NOT NULL,
    predicate VARCHAR(256) NOT NULL,
    op_value CLOB NOT NULL,
    status VARCHAR(32) NOT NULL,
    reason CLOB NOT NULL,
    proposed_by VARCHAR(128) NOT NULL,
    confidence DOUBLE NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_memory_operations_record ON memory_operations(record_id, status, created_at);
CREATE INDEX IF NOT EXISTS idx_memory_operations_context ON memory_operations(server_id, world_id, subject, predicate, status);

CREATE TABLE IF NOT EXISTS memory_summaries (
    id VARCHAR(80) PRIMARY KEY,
    created_at BIGINT NOT NULL,
    updated_at BIGINT NOT NULL,
    server_id VARCHAR(128) NOT NULL,
    world_id VARCHAR(256) NOT NULL,
    record_id VARCHAR(80) NOT NULL,
    summary CLOB NOT NULL,
    raw_episode_citation_id VARCHAR(160) NOT NULL,
    related_memory_ids CLOB NOT NULL,
    evolution_count INT NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_memory_summaries_record ON memory_summaries(record_id, updated_at);

CREATE TABLE IF NOT EXISTS memory_links (
    id VARCHAR(80) PRIMARY KEY,
    created_at BIGINT NOT NULL,
    server_id VARCHAR(128) NOT NULL,
    world_id VARCHAR(256) NOT NULL,
    source_record_id VARCHAR(80) NOT NULL,
    target_record_id VARCHAR(80) NOT NULL,
    relation VARCHAR(64) NOT NULL,
    reason CLOB NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_memory_links_source ON memory_links(source_record_id, created_at);

CREATE TABLE IF NOT EXISTS memory_safety_lessons (
    id VARCHAR(80) PRIMARY KEY,
    created_at BIGINT NOT NULL,
    server_id VARCHAR(128) NOT NULL,
    world_id VARCHAR(256) NOT NULL,
    subject VARCHAR(256) NOT NULL,
    lesson CLOB NOT NULL,
    source_record_id VARCHAR(80) NOT NULL,
    conflict_id VARCHAR(80),
    citation_ids CLOB NOT NULL,
    status VARCHAR(32) NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_memory_safety_lessons_context ON memory_safety_lessons(server_id, world_id, subject, created_at);

CREATE TABLE IF NOT EXISTS npc_profiles (
    profile_key VARCHAR(520) PRIMARY KEY,
    world_id VARCHAR(256) NOT NULL,
    npc_key VARCHAR(256) NOT NULL,
    profile_json CLOB NOT NULL,
    updated_at BIGINT NOT NULL,
    privacy_deleted_at BIGINT DEFAULT 0
);

CREATE INDEX IF NOT EXISTS idx_npc_profiles_world ON npc_profiles(world_id, npc_key);

CREATE TABLE IF NOT EXISTS chat_sessions (
    conversation_id VARCHAR(128) PRIMARY KEY,
    server_id VARCHAR(128) NOT NULL,
    world_id VARCHAR(256) NOT NULL,
    minecraft_player_uuid VARCHAR(80) NOT NULL,
    npc_key VARCHAR(256) NOT NULL,
    session_json CLOB NOT NULL,
    status VARCHAR(32) NOT NULL,
    started_at BIGINT NOT NULL,
    updated_at BIGINT NOT NULL,
    cancel_reason CLOB DEFAULT '',
    privacy_deleted_at BIGINT DEFAULT 0
);

CREATE INDEX IF NOT EXISTS idx_chat_sessions_context ON chat_sessions(server_id, world_id, minecraft_player_uuid, npc_key, updated_at);

CREATE TABLE IF NOT EXISTS npc_knowledge_updates (
    id VARCHAR(80) PRIMARY KEY,
    created_at BIGINT NOT NULL,
    npc_key VARCHAR(256) NOT NULL,
    pack_id VARCHAR(256),
    fact CLOB,
    reason CLOB,
    update_json CLOB NOT NULL,
    privacy_deleted_at BIGINT DEFAULT 0
);

CREATE INDEX IF NOT EXISTS idx_npc_knowledge_updates_npc ON npc_knowledge_updates(npc_key, created_at);

CREATE TABLE IF NOT EXISTS quota_windows (
    quota_key VARCHAR(512) PRIMARY KEY,
    minute_window_start_ms BIGINT NOT NULL,
    minute_used BIGINT NOT NULL,
    day_start_ms BIGINT NOT NULL,
    daily_used BIGINT NOT NULL,
    updated_at BIGINT NOT NULL
);

CREATE TABLE IF NOT EXISTS gateway_token_audit (
    id VARCHAR(80) PRIMARY KEY,
    created_at BIGINT NOT NULL,
    minecraft_player_uuid VARCHAR(80) NOT NULL,
    provider VARCHAR(128) NOT NULL,
    event VARCHAR(64) NOT NULL,
    token_fingerprint VARCHAR(128) DEFAULT ''
);
