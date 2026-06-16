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
