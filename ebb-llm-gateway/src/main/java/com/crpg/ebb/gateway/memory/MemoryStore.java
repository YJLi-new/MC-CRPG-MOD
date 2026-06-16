package com.crpg.ebb.gateway.memory;

import com.crpg.ebb.gateway.chat.GatewayChatRequest;
import com.crpg.ebb.gateway.chat.GatewayChatResponse;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public final class MemoryStore {
    public static final String MIGRATION_VERSION = "V001__memory_store";
    private final String jdbcUrl;
    private final MemoryEmbeddingService embeddings = new MemoryEmbeddingService();
    private final MemoryFactExtractor factExtractor = new MemoryFactExtractor();

    public MemoryStore(String jdbcUrl) {
        this.jdbcUrl = jdbcUrl == null || jdbcUrl.isBlank()
                ? "jdbc:h2:./ebb-llm-gateway-data/memory;AUTO_SERVER=TRUE"
                : jdbcUrl;
        migrate();
    }

    public MemoryAppendResult appendTurn(GatewayChatRequest request, GatewayChatResponse response) {
        long now = Instant.now().toEpochMilli();
        List<MemoryRecord> records = new ArrayList<>();
        List<MemoryFact> facts = new ArrayList<>();
        List<MemoryConflict> conflicts = new ArrayList<>();
        try (Connection connection = connect()) {
            connection.setAutoCommit(false);
            MemoryRecord player = newRecord(request, now, "player", request.message());
            insertRecord(connection, player);
            records.add(player);
            if (response != null && !response.npcReply().isBlank()) {
                MemoryRecord npc = newRecord(request, now + 1, "npc", response.npcReply());
                insertRecord(connection, npc);
                records.add(npc);
            }
            for (MemoryFactExtractor.ExtractedFact extracted : factExtractor.extract(request)) {
                MemoryFact fact = newFact(request, player, extracted, now + 2);
                Optional<MemoryFact> previous = currentFact(connection, fact.serverId(), fact.worldId(), fact.subject(), fact.predicate());
                insertFact(connection, fact);
                facts.add(fact);
                if (previous.isPresent() && !previous.get().value().equalsIgnoreCase(fact.value())) {
                    supersedeFact(connection, previous.get().id(), fact.id());
                    MemoryConflict conflict = newConflict(previous.get(), fact, now + 3);
                    insertConflict(connection, conflict);
                    conflicts.add(conflict);
                }
            }
            connection.commit();
        } catch (SQLException ex) {
            throw new IllegalStateException("memory_append_failed", ex);
        }
        return new MemoryAppendResult(records, facts, conflicts);
    }

    public List<ScoredMemory> search(MemorySearchRequest request) {
        List<MemoryRecord> records = new ArrayList<>();
        try (Connection connection = connect(); PreparedStatement statement = connection.prepareStatement("""
                SELECT * FROM memory_records
                WHERE server_id = ? AND world_id = ?
                  AND (? = '' OR minecraft_player_uuid = ?)
                  AND (? = '' OR npc_key = ? OR entity_uuid = ?)
                ORDER BY created_at DESC
                LIMIT 200
                """)) {
            statement.setString(1, request.serverId());
            statement.setString(2, request.worldId());
            statement.setString(3, request.minecraftPlayerUuid());
            statement.setString(4, request.minecraftPlayerUuid());
            statement.setString(5, request.npcKey().isBlank() && request.entityUuid().isBlank() ? "" : "filter");
            statement.setString(6, request.npcKey());
            statement.setString(7, request.entityUuid());
            try (ResultSet rs = statement.executeQuery()) {
                while (rs.next()) {
                    records.add(record(rs));
                }
            }
        } catch (SQLException ex) {
            throw new IllegalStateException("memory_search_failed", ex);
        }
        double[] queryVector = embeddings.embed(request.query());
        long newest = records.stream().mapToLong(MemoryRecord::createdAt).max().orElse(0L);
        return records.stream()
                .map(record -> score(request, record, queryVector, newest))
                .filter(scored -> scored.score() > 0.01D || request.query().isBlank())
                .sorted(Comparator.comparingDouble(ScoredMemory::score).reversed())
                .limit(request.limit())
                .toList();
    }

    public Optional<Map<String, Object>> inspect(String id) {
        if (id == null || id.isBlank()) {
            return Optional.empty();
        }
        try (Connection connection = connect()) {
            Optional<MemoryRecord> record = recordById(connection, id);
            if (record.isPresent()) {
                Map<String, Object> values = new LinkedHashMap<>();
                values.put("type", "record");
                values.put("record", record.get().toJsonMap());
                return Optional.of(values);
            }
            Optional<MemoryFact> fact = factById(connection, id);
            if (fact.isPresent()) {
                Map<String, Object> values = new LinkedHashMap<>();
                values.put("type", "fact");
                values.put("fact", fact.get().toJsonMap());
                return Optional.of(values);
            }
            Optional<MemoryConflict> conflict = conflictById(connection, id);
            if (conflict.isPresent()) {
                Map<String, Object> values = new LinkedHashMap<>();
                values.put("type", "conflict");
                values.put("conflict", conflict.get().toJsonMap());
                return Optional.of(values);
            }
        } catch (SQLException ex) {
            throw new IllegalStateException("memory_inspect_failed", ex);
        }
        return Optional.empty();
    }

    public List<MemoryConflict> conflicts(String serverId, String worldId, int limit) {
        List<MemoryConflict> values = new ArrayList<>();
        try (Connection connection = connect(); PreparedStatement statement = connection.prepareStatement("""
                SELECT * FROM memory_conflicts
                WHERE server_id = ? AND world_id = ?
                ORDER BY created_at DESC
                LIMIT ?
                """)) {
            statement.setString(1, blank(serverId, "local-dev"));
            statement.setString(2, blank(worldId, "unknown-world"));
            statement.setInt(3, Math.max(1, Math.min(100, limit <= 0 ? 25 : limit)));
            try (ResultSet rs = statement.executeQuery()) {
                while (rs.next()) {
                    values.add(conflict(rs));
                }
            }
        } catch (SQLException ex) {
            throw new IllegalStateException("memory_conflicts_failed", ex);
        }
        return List.copyOf(values);
    }

    public Map<String, Object> summary() {
        try (Connection connection = connect()) {
            return Map.of(
                    "status", "ok",
                    "db", redactJdbcUrl(jdbcUrl),
                    "records", count(connection, "memory_records"),
                    "facts", count(connection, "memory_facts"),
                    "conflicts", count(connection, "memory_conflicts")
            );
        } catch (SQLException ex) {
            return Map.of("status", "error", "error", "memory_summary_failed");
        }
    }

    private void migrate() {
        try (Connection connection = connect(); Statement statement = connection.createStatement()) {
            String sql = migrationSql();
            for (String chunk : sql.split(";")) {
                if (!chunk.isBlank()) {
                    statement.execute(chunk);
                }
            }
            try (PreparedStatement insert = connection.prepareStatement("MERGE INTO schema_migrations(version, applied_at) KEY(version) VALUES (?, ?)")) {
                insert.setString(1, MIGRATION_VERSION);
                insert.setLong(2, Instant.now().toEpochMilli());
                insert.executeUpdate();
            }
        } catch (SQLException ex) {
            throw new IllegalStateException("memory_migration_failed", ex);
        }
    }

    private String migrationSql() {
        try (InputStream in = MemoryStore.class.getResourceAsStream("/db/migration/V001__memory_store.sql")) {
            if (in == null) {
                throw new IllegalStateException("missing_memory_migration");
            }
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException ex) {
            throw new IllegalStateException("memory_migration_read_failed", ex);
        }
    }

    private Connection connect() throws SQLException {
        return DriverManager.getConnection(jdbcUrl);
    }

    private MemoryRecord newRecord(GatewayChatRequest request, long now, String role, String text) {
        String id = "memrec_" + UUID.randomUUID().toString().replace("-", "");
        String citation = "memory:record:" + id;
        return new MemoryRecord(id, now, request.serverId(), request.worldId(), request.minecraftPlayerUuid(), request.npcKey(),
                request.entityUuid(), request.conversationId(), request.dialogueId(), request.sourceNodeId(), role,
                text == null ? "" : text, citation, embeddings.serialize(embeddings.embed(text)));
    }

    private MemoryFact newFact(GatewayChatRequest request, MemoryRecord record, MemoryFactExtractor.ExtractedFact extracted, long now) {
        String id = "memfact_" + UUID.randomUUID().toString().replace("-", "");
        String text = extracted.subject() + " " + extracted.predicate() + " " + extracted.value();
        return new MemoryFact(id, record.id(), now, request.serverId(), request.worldId(), extracted.subject(), extracted.predicate(),
                extracted.value(), "current", "", "memory:fact:" + id, embeddings.serialize(embeddings.embed(text)));
    }

    private MemoryConflict newConflict(MemoryFact oldFact, MemoryFact newFact, long now) {
        String id = "memconf_" + UUID.randomUUID().toString().replace("-", "");
        return new MemoryConflict(id, now, newFact.serverId(), newFact.worldId(), newFact.subject(), newFact.predicate(),
                oldFact.id(), newFact.id(), oldFact.value(), newFact.value(), oldFact.citationId() + "," + newFact.citationId(), "open");
    }

    private void insertRecord(Connection connection, MemoryRecord record) throws SQLException {
        try (PreparedStatement ps = connection.prepareStatement("""
                INSERT INTO memory_records(id, created_at, server_id, world_id, minecraft_player_uuid, npc_key, entity_uuid, conversation_id, dialogue_id, source_node_id, role, text, citation_id, embedding)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """)) {
            ps.setString(1, record.id());
            ps.setLong(2, record.createdAt());
            ps.setString(3, record.serverId());
            ps.setString(4, record.worldId());
            ps.setString(5, record.minecraftPlayerUuid());
            ps.setString(6, record.npcKey());
            ps.setString(7, record.entityUuid());
            ps.setString(8, record.conversationId());
            ps.setString(9, record.dialogueId());
            ps.setString(10, record.sourceNodeId());
            ps.setString(11, record.role());
            ps.setString(12, record.text());
            ps.setString(13, record.citationId());
            ps.setString(14, record.embedding());
            ps.executeUpdate();
        }
    }

    private void insertFact(Connection connection, MemoryFact fact) throws SQLException {
        try (PreparedStatement ps = connection.prepareStatement("""
                INSERT INTO memory_facts(id, record_id, created_at, server_id, world_id, subject, predicate, fact_value, status, superseded_by, citation_id, embedding)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """)) {
            ps.setString(1, fact.id());
            ps.setString(2, fact.recordId());
            ps.setLong(3, fact.createdAt());
            ps.setString(4, fact.serverId());
            ps.setString(5, fact.worldId());
            ps.setString(6, fact.subject());
            ps.setString(7, fact.predicate());
            ps.setString(8, fact.value());
            ps.setString(9, fact.status());
            ps.setString(10, fact.supersededBy());
            ps.setString(11, fact.citationId());
            ps.setString(12, fact.embedding());
            ps.executeUpdate();
        }
    }

    private void insertConflict(Connection connection, MemoryConflict conflict) throws SQLException {
        try (PreparedStatement ps = connection.prepareStatement("""
                INSERT INTO memory_conflicts(id, created_at, server_id, world_id, subject, predicate, old_fact_id, new_fact_id, old_fact_value, new_fact_value, citation_ids, status)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """)) {
            ps.setString(1, conflict.id());
            ps.setLong(2, conflict.createdAt());
            ps.setString(3, conflict.serverId());
            ps.setString(4, conflict.worldId());
            ps.setString(5, conflict.subject());
            ps.setString(6, conflict.predicate());
            ps.setString(7, conflict.oldFactId());
            ps.setString(8, conflict.newFactId());
            ps.setString(9, conflict.oldValue());
            ps.setString(10, conflict.newValue());
            ps.setString(11, conflict.citationIds());
            ps.setString(12, conflict.status());
            ps.executeUpdate();
        }
    }

    private Optional<MemoryFact> currentFact(Connection connection, String serverId, String worldId, String subject, String predicate) throws SQLException {
        try (PreparedStatement ps = connection.prepareStatement("""
                SELECT * FROM memory_facts
                WHERE server_id = ? AND world_id = ? AND subject = ? AND predicate = ? AND status = 'current'
                ORDER BY created_at DESC LIMIT 1
                """)) {
            ps.setString(1, serverId);
            ps.setString(2, worldId);
            ps.setString(3, subject);
            ps.setString(4, predicate);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(fact(rs)) : Optional.empty();
            }
        }
    }

    private void supersedeFact(Connection connection, String oldFactId, String newFactId) throws SQLException {
        try (PreparedStatement ps = connection.prepareStatement("UPDATE memory_facts SET status = 'superseded', superseded_by = ? WHERE id = ?")) {
            ps.setString(1, newFactId);
            ps.setString(2, oldFactId);
            ps.executeUpdate();
        }
    }

    private Optional<MemoryRecord> recordById(Connection connection, String id) throws SQLException {
        try (PreparedStatement ps = connection.prepareStatement("SELECT * FROM memory_records WHERE id = ?")) {
            ps.setString(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(record(rs)) : Optional.empty();
            }
        }
    }

    private Optional<MemoryFact> factById(Connection connection, String id) throws SQLException {
        try (PreparedStatement ps = connection.prepareStatement("SELECT * FROM memory_facts WHERE id = ?")) {
            ps.setString(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(fact(rs)) : Optional.empty();
            }
        }
    }

    private Optional<MemoryConflict> conflictById(Connection connection, String id) throws SQLException {
        try (PreparedStatement ps = connection.prepareStatement("SELECT * FROM memory_conflicts WHERE id = ?")) {
            ps.setString(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(conflict(rs)) : Optional.empty();
            }
        }
    }

    private ScoredMemory score(MemorySearchRequest request, MemoryRecord record, double[] queryVector, long newest) {
        String lower = record.text().toLowerCase(Locale.ROOT);
        double keyword = 0.0D;
        for (String token : request.query().toLowerCase(Locale.ROOT).split("[^\\p{L}\\p{N}_:.-]+")) {
            if (!token.isBlank() && lower.contains(token)) {
                keyword += 0.35D;
            }
        }
        double vector = embeddings.cosine(queryVector, embeddings.parse(record.embedding()));
        double entity = (!request.npcKey().isBlank() && request.npcKey().equals(record.npcKey()))
                || (!request.entityUuid().isBlank() && request.entityUuid().equals(record.entityUuid())) ? 0.25D : 0.0D;
        double recency = newest <= 0 ? 0.0D : Math.max(0.0D, 1.0D - ((double) (newest - record.createdAt()) / 86_400_000.0D)) * 0.15D;
        double score = keyword + vector * 0.45D + entity + recency;
        String reason = "keyword=" + round(keyword) + ",vector=" + round(vector) + ",entity=" + round(entity) + ",recent=" + round(recency);
        return new ScoredMemory(record, score, reason);
    }

    private static MemoryRecord record(ResultSet rs) throws SQLException {
        return new MemoryRecord(rs.getString("id"), rs.getLong("created_at"), rs.getString("server_id"), rs.getString("world_id"),
                rs.getString("minecraft_player_uuid"), rs.getString("npc_key"), rs.getString("entity_uuid"), rs.getString("conversation_id"),
                rs.getString("dialogue_id"), rs.getString("source_node_id"), rs.getString("role"), rs.getString("text"),
                rs.getString("citation_id"), rs.getString("embedding"));
    }

    private static MemoryFact fact(ResultSet rs) throws SQLException {
        return new MemoryFact(rs.getString("id"), rs.getString("record_id"), rs.getLong("created_at"), rs.getString("server_id"), rs.getString("world_id"),
                rs.getString("subject"), rs.getString("predicate"), rs.getString("fact_value"), rs.getString("status"), rs.getString("superseded_by"),
                rs.getString("citation_id"), rs.getString("embedding"));
    }

    private static MemoryConflict conflict(ResultSet rs) throws SQLException {
        return new MemoryConflict(rs.getString("id"), rs.getLong("created_at"), rs.getString("server_id"), rs.getString("world_id"),
                rs.getString("subject"), rs.getString("predicate"), rs.getString("old_fact_id"), rs.getString("new_fact_id"),
                rs.getString("old_fact_value"), rs.getString("new_fact_value"), rs.getString("citation_ids"), rs.getString("status"));
    }

    private static long count(Connection connection, String table) throws SQLException {
        try (Statement statement = connection.createStatement(); ResultSet rs = statement.executeQuery("SELECT COUNT(*) FROM " + table)) {
            return rs.next() ? rs.getLong(1) : 0L;
        }
    }

    private static String blank(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value.strip();
    }

    private static double round(double value) {
        return Math.round(value * 1000.0D) / 1000.0D;
    }

    private static String redactJdbcUrl(String url) {
        if (url == null) {
            return "";
        }
        int semi = url.indexOf(';');
        String base = semi >= 0 ? url.substring(0, semi) : url;
        return base.replaceAll("(?i)(password=)[^;]+", "$1redacted");
    }
}
