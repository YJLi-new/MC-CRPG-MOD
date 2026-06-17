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
    private final LlmMemoryOperationExtractor llmExtractor = new LlmMemoryOperationExtractor();
    private final DeterministicMemoryValidator validator = new DeterministicMemoryValidator();
    private final MemoryConsolidator consolidator = new MemoryConsolidator();

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
        List<MemoryOperation> operations = new ArrayList<>();
        List<MemorySummary> summaries = new ArrayList<>();
        List<MemoryLink> links = new ArrayList<>();
        List<MemorySafetyLesson> safetyLessons = new ArrayList<>();
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

            for (MemoryOperation proposed : llmExtractor.propose(request, response, player)) {
                MemoryOperation withRecord = proposed.recordId().isBlank() ? proposed.withRecord(player.id()) : proposed;
                MemoryValidationDecision decision = validator.validate(withRecord);
                MemoryOperation persisted = withRecord.withStatus(decision.status(), decision.reason());
                insertOperation(connection, persisted);
                operations.add(persisted);
                if (decision.accepted()) {
                    if (MemoryOperation.ADD_FACT.equals(persisted.type())) {
                        ApplyFactResult applied = applyFactOperation(connection, request, persisted, now + 2);
                        facts.add(applied.fact());
                        conflicts.addAll(applied.conflicts());
                        safetyLessons.addAll(applied.safetyLessons());
                    } else if (MemoryOperation.ADD_SAFETY_LESSON.equals(persisted.type())) {
                        MemorySafetyLesson lesson = newSafetyLesson(request, persisted.subject(), persisted.value(), persisted.recordId(), "", persisted.recordId(), now + 4);
                        insertSafetyLesson(connection, lesson);
                        safetyLessons.add(lesson);
                    }
                } else if (decision.safetyLessonRequired()) {
                    MemorySafetyLesson lesson = newSafetyLesson(request, persisted.subject(),
                            consolidator.safetyLessonForCanonicalRejection(persisted, decision.reason()),
                            persisted.recordId(), "", persisted.recordId(), now + 4);
                    insertSafetyLesson(connection, lesson);
                    safetyLessons.add(lesson);
                }
            }

            for (MemoryRecord record : records) {
                List<MemoryOperation> acceptedForRecord = operations.stream()
                        .filter(operation -> "accepted".equals(operation.status()) && operation.recordId().equals(record.id()))
                        .toList();
                List<MemoryLink> recordLinks = linkRelatedMemories(connection, record, now + 5);
                links.addAll(recordLinks);
                String relatedIds = joinTargetIds(recordLinks);
                String summary = consolidator.backgroundSummarize(request, record, acceptedForRecord);
                updateRecordSummary(connection, record.id(), summary, now + 6, relatedIds);
                MemorySummary memorySummary = new MemorySummary(id("memsum"), now + 6, now + 6, record.serverId(), record.worldId(),
                        record.id(), summary, record.citationId(), relatedIds, 0);
                insertSummary(connection, memorySummary);
                summaries.add(memorySummary);
            }
            connection.commit();
        } catch (SQLException ex) {
            throw new IllegalStateException("memory_append_failed", ex);
        }
        return new MemoryAppendResult(records, facts, conflicts, operations, summaries, links, safetyLessons);
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
                values.put("raw_episode", record.get().text());
                values.put("extracted_facts", factsByRecord(connection, record.get().id()).stream().map(MemoryFact::toJsonMap).toList());
                values.put("memory_operations", operationsByRecord(connection, record.get().id()).stream().map(MemoryOperation::toJsonMap).toList());
                values.put("summaries", summariesByRecord(connection, record.get().id()).stream().map(MemorySummary::toJsonMap).toList());
                values.put("related_links", linksByRecord(connection, record.get().id()).stream().map(MemoryLink::toJsonMap).toList());
                values.put("safety_lessons", safetyLessonsByRecord(connection, record.get().id()).stream().map(MemorySafetyLesson::toJsonMap).toList());
                return Optional.of(values);
            }
            Optional<MemoryFact> fact = factById(connection, id);
            if (fact.isPresent()) {
                Map<String, Object> values = new LinkedHashMap<>();
                values.put("type", "fact");
                values.put("fact", fact.get().toJsonMap());
                recordById(connection, fact.get().recordId()).ifPresent(source -> values.put("raw_episode", source.toJsonMap()));
                return Optional.of(values);
            }
            Optional<MemoryConflict> conflict = conflictById(connection, id);
            if (conflict.isPresent()) {
                Map<String, Object> values = new LinkedHashMap<>();
                values.put("type", "conflict");
                values.put("conflict", conflict.get().toJsonMap());
                return Optional.of(values);
            }
            Optional<MemoryOperation> operation = operationById(connection, id);
            if (operation.isPresent()) {
                return Optional.of(Map.of("type", "operation", "operation", operation.get().toJsonMap()));
            }
            Optional<MemorySafetyLesson> lesson = safetyLessonById(connection, id);
            if (lesson.isPresent()) {
                return Optional.of(Map.of("type", "safety_lesson", "safety_lesson", lesson.get().toJsonMap()));
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

    public List<MemoryRecord> episodes(String serverId, String worldId, int limit) {
        List<MemoryRecord> values = new ArrayList<>();
        try (Connection connection = connect(); PreparedStatement statement = connection.prepareStatement("""
                SELECT * FROM memory_records
                WHERE server_id = ? AND world_id = ?
                ORDER BY created_at DESC
                LIMIT ?
                """)) {
            statement.setString(1, blank(serverId, "local-dev"));
            statement.setString(2, blank(worldId, "unknown-world"));
            statement.setInt(3, Math.max(1, Math.min(100, limit <= 0 ? 25 : limit)));
            try (ResultSet rs = statement.executeQuery()) {
                while (rs.next()) {
                    values.add(record(rs));
                }
            }
        } catch (SQLException ex) {
            throw new IllegalStateException("memory_episodes_failed", ex);
        }
        return List.copyOf(values);
    }

    public List<MemorySafetyLesson> safetyLessons(String serverId, String worldId, int limit) {
        List<MemorySafetyLesson> values = new ArrayList<>();
        try (Connection connection = connect(); PreparedStatement statement = connection.prepareStatement("""
                SELECT * FROM memory_safety_lessons
                WHERE server_id = ? AND world_id = ?
                ORDER BY created_at DESC
                LIMIT ?
                """)) {
            statement.setString(1, blank(serverId, "local-dev"));
            statement.setString(2, blank(worldId, "unknown-world"));
            statement.setInt(3, Math.max(1, Math.min(100, limit <= 0 ? 25 : limit)));
            try (ResultSet rs = statement.executeQuery()) {
                while (rs.next()) {
                    values.add(safetyLesson(rs));
                }
            }
        } catch (SQLException ex) {
            throw new IllegalStateException("memory_lessons_failed", ex);
        }
        return List.copyOf(values);
    }

    public Map<String, Object> correctFact(String factId, String newValue, String reason) {
        if (factId == null || factId.isBlank() || newValue == null || newValue.isBlank()) {
            return Map.of("status", "error", "accepted", false, "error", "fact_id_and_new_value_required");
        }
        try (Connection connection = connect()) {
            Optional<MemoryFact> fact = factById(connection, factId);
            if (fact.isEmpty()) {
                return Map.of("status", "error", "accepted", false, "error", "fact_not_found", "fact_id", factId);
            }
            long now = Instant.now().toEpochMilli();
            MemorySafetyLesson correction = new MemorySafetyLesson(
                    id("memcorr"),
                    now,
                    fact.get().serverId(),
                    fact.get().worldId(),
                    fact.get().subject(),
                    "manual memory correction requested for " + factId + ": "
                            + fact.get().value() + " -> " + newValue
                            + " (" + blank(reason, "manual_correction") + ")",
                    fact.get().recordId(),
                    "",
                    fact.get().citationId(),
                    "active"
            );
            insertSafetyLesson(connection, correction);
            return Map.of(
                    "status", "ok",
                    "accepted", true,
                    "append_only", true,
                    "fact_id", factId,
                    "correction_lesson_id", correction.id()
            );
        } catch (SQLException ex) {
            return Map.of("status", "error", "accepted", false, "error", "memory_correction_failed");
        }
    }

    public Map<String, Object> deletePlayer(String playerUuid) {
        if (playerUuid == null || playerUuid.isBlank()) {
            return Map.of("status", "error", "deleted", false, "error", "player_uuid_required");
        }
        try (Connection connection = connect()) {
            connection.setAutoCommit(false);
            List<String> recordIds = new ArrayList<>();
            List<String> factIds = new ArrayList<>();
            try (PreparedStatement records = connection.prepareStatement("SELECT id FROM memory_records WHERE minecraft_player_uuid = ?")) {
                records.setString(1, playerUuid);
                try (ResultSet rs = records.executeQuery()) {
                    while (rs.next()) {
                        recordIds.add(rs.getString(1));
                    }
                }
            }
            if (!recordIds.isEmpty()) {
                try (PreparedStatement facts = connection.prepareStatement("SELECT id FROM memory_facts WHERE record_id = ?")) {
                    for (String recordId : recordIds) {
                        facts.setString(1, recordId);
                        try (ResultSet rs = facts.executeQuery()) {
                            while (rs.next()) {
                                factIds.add(rs.getString(1));
                            }
                        }
                    }
                }
            }
            int deletedConflicts = 0;
            for (String factId : factIds) {
                deletedConflicts += deleteWhere(connection, "DELETE FROM memory_conflicts WHERE old_fact_id = ? OR new_fact_id = ?", factId, factId);
            }
            int deletedFacts = 0;
            int deletedOperations = 0;
            int deletedSummaries = 0;
            int deletedLinks = 0;
            int deletedLessons = 0;
            for (String recordId : recordIds) {
                deletedFacts += deleteWhere(connection, "DELETE FROM memory_facts WHERE record_id = ?", recordId);
                deletedOperations += deleteWhere(connection, "DELETE FROM memory_operations WHERE record_id = ?", recordId);
                deletedSummaries += deleteWhere(connection, "DELETE FROM memory_summaries WHERE record_id = ?", recordId);
                deletedLinks += deleteWhere(connection, "DELETE FROM memory_links WHERE source_record_id = ? OR target_record_id = ?", recordId, recordId);
                deletedLessons += deleteWhere(connection, "DELETE FROM memory_safety_lessons WHERE source_record_id = ?", recordId);
            }
            int deletedRecords = deleteWhere(connection, "DELETE FROM memory_records WHERE minecraft_player_uuid = ?", playerUuid);
            connection.commit();
            return Map.of(
                    "status", "ok",
                    "deleted", true,
                    "player_uuid", playerUuid,
                    "records", deletedRecords,
                    "facts", deletedFacts,
                    "conflicts", deletedConflicts,
                    "operations", deletedOperations,
                    "summaries", deletedSummaries,
                    "links", deletedLinks,
                    "safety_lessons", deletedLessons
            );
        } catch (SQLException ex) {
            return Map.of("status", "error", "deleted", false, "error", "memory_delete_player_failed");
        }
    }

    public Map<String, Object> summary() {
        try (Connection connection = connect()) {
            return Map.of(
                    "status", "ok",
                    "db", redactJdbcUrl(jdbcUrl),
                    "records", count(connection, "memory_records"),
                    "facts", count(connection, "memory_facts"),
                    "conflicts", count(connection, "memory_conflicts"),
                    "operations", count(connection, "memory_operations"),
                    "summaries", count(connection, "memory_summaries"),
                    "links", count(connection, "memory_links"),
                    "safety_lessons", count(connection, "memory_safety_lessons"),
                    "canonical_facts", validator.canonicalFactsSummary()
            );
        } catch (SQLException ex) {
            return Map.of("status", "error", "error", "memory_summary_failed");
        }
    }

    private int deleteWhere(Connection connection, String sql, String first) throws SQLException {
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, first);
            return ps.executeUpdate();
        }
    }

    private int deleteWhere(Connection connection, String sql, String first, String second) throws SQLException {
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, first);
            ps.setString(2, second);
            return ps.executeUpdate();
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
        String id = id("memrec");
        String citation = "memory:record:" + id;
        return new MemoryRecord(id, now, request.serverId(), request.worldId(), request.minecraftPlayerUuid(), request.npcKey(),
                request.entityUuid(), request.conversationId(), request.dialogueId(), request.sourceNodeId(), role,
                text == null ? "" : text, citation, embeddings.serialize(embeddings.embed(text)), "", 0L, "");
    }

    private MemoryFact newFact(GatewayChatRequest request, MemoryOperation operation, long now) {
        String id = id("memfact");
        String text = operation.subject() + " " + operation.predicate() + " " + operation.value();
        return new MemoryFact(id, operation.recordId(), now, request.serverId(), request.worldId(), operation.subject(), operation.predicate(),
                operation.value(), "current", "", "memory:fact:" + id, embeddings.serialize(embeddings.embed(text)));
    }

    private MemoryConflict newConflict(MemoryFact oldFact, MemoryFact newFact, long now) {
        String id = id("memconf");
        return new MemoryConflict(id, now, newFact.serverId(), newFact.worldId(), newFact.subject(), newFact.predicate(),
                oldFact.id(), newFact.id(), oldFact.value(), newFact.value(), oldFact.citationId() + "," + newFact.citationId(), "open");
    }

    private MemorySafetyLesson newSafetyLesson(GatewayChatRequest request, String subject, String lesson, String sourceRecordId, String conflictId, String citationIds, long now) {
        String id = id("memsafe");
        String citations = citationIds == null || citationIds.isBlank() ? "memory:record:" + sourceRecordId : citationIds;
        return new MemorySafetyLesson(id, now, request.serverId(), request.worldId(), subject, lesson, sourceRecordId, conflictId, citations, "active");
    }

    private ApplyFactResult applyFactOperation(Connection connection, GatewayChatRequest request, MemoryOperation operation, long now) throws SQLException {
        List<MemoryConflict> conflicts = new ArrayList<>();
        List<MemorySafetyLesson> safetyLessons = new ArrayList<>();
        MemoryFact fact = newFact(request, operation, now);
        Optional<MemoryFact> previous = currentFact(connection, fact.serverId(), fact.worldId(), fact.subject(), fact.predicate());
        insertFact(connection, fact);
        if (previous.isPresent() && !previous.get().value().equalsIgnoreCase(fact.value())) {
            supersedeFact(connection, previous.get().id(), fact.id());
            MemoryConflict conflict = newConflict(previous.get(), fact, now + 1);
            insertConflict(connection, conflict);
            conflicts.add(conflict);
            evolveOldSummary(connection, previous.get(), fact, now + 2);
        }
        return new ApplyFactResult(fact, List.copyOf(conflicts), List.copyOf(safetyLessons));
    }

    private void insertRecord(Connection connection, MemoryRecord record) throws SQLException {
        try (PreparedStatement ps = connection.prepareStatement("""
                INSERT INTO memory_records(id, created_at, server_id, world_id, minecraft_player_uuid, npc_key, entity_uuid, conversation_id, dialogue_id, source_node_id, role, text, citation_id, embedding, summary, summary_updated_at, related_memory_ids)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
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
            ps.setString(15, record.summary());
            ps.setLong(16, record.summaryUpdatedAt());
            ps.setString(17, record.relatedMemoryIds());
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

    private void insertOperation(Connection connection, MemoryOperation operation) throws SQLException {
        try (PreparedStatement ps = connection.prepareStatement("""
                INSERT INTO memory_operations(id, created_at, server_id, world_id, record_id, op_type, subject, predicate, op_value, status, reason, proposed_by, confidence)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """)) {
            ps.setString(1, operation.id());
            ps.setLong(2, operation.createdAt());
            ps.setString(3, operation.serverId());
            ps.setString(4, operation.worldId());
            ps.setString(5, operation.recordId());
            ps.setString(6, operation.type());
            ps.setString(7, operation.subject());
            ps.setString(8, operation.predicate());
            ps.setString(9, operation.value());
            ps.setString(10, operation.status());
            ps.setString(11, operation.reason());
            ps.setString(12, operation.proposedBy());
            ps.setDouble(13, operation.confidence());
            ps.executeUpdate();
        }
    }

    private void insertSummary(Connection connection, MemorySummary summary) throws SQLException {
        try (PreparedStatement ps = connection.prepareStatement("""
                INSERT INTO memory_summaries(id, created_at, updated_at, server_id, world_id, record_id, summary, raw_episode_citation_id, related_memory_ids, evolution_count)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """)) {
            ps.setString(1, summary.id());
            ps.setLong(2, summary.createdAt());
            ps.setLong(3, summary.updatedAt());
            ps.setString(4, summary.serverId());
            ps.setString(5, summary.worldId());
            ps.setString(6, summary.recordId());
            ps.setString(7, summary.summary());
            ps.setString(8, summary.rawEpisodeCitationId());
            ps.setString(9, summary.relatedMemoryIds());
            ps.setInt(10, summary.evolutionCount());
            ps.executeUpdate();
        }
    }

    private void insertLink(Connection connection, MemoryLink link) throws SQLException {
        try (PreparedStatement ps = connection.prepareStatement("""
                INSERT INTO memory_links(id, created_at, server_id, world_id, source_record_id, target_record_id, relation, reason)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?)
                """)) {
            ps.setString(1, link.id());
            ps.setLong(2, link.createdAt());
            ps.setString(3, link.serverId());
            ps.setString(4, link.worldId());
            ps.setString(5, link.sourceRecordId());
            ps.setString(6, link.targetRecordId());
            ps.setString(7, link.relation());
            ps.setString(8, link.reason());
            ps.executeUpdate();
        }
    }

    private void insertSafetyLesson(Connection connection, MemorySafetyLesson lesson) throws SQLException {
        try (PreparedStatement ps = connection.prepareStatement("""
                INSERT INTO memory_safety_lessons(id, created_at, server_id, world_id, subject, lesson, source_record_id, conflict_id, citation_ids, status)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """)) {
            ps.setString(1, lesson.id());
            ps.setLong(2, lesson.createdAt());
            ps.setString(3, lesson.serverId());
            ps.setString(4, lesson.worldId());
            ps.setString(5, lesson.subject());
            ps.setString(6, lesson.lesson());
            ps.setString(7, lesson.sourceRecordId());
            ps.setString(8, lesson.conflictId());
            ps.setString(9, lesson.citationIds());
            ps.setString(10, lesson.status());
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

    private void updateRecordSummary(Connection connection, String recordId, String summary, long updatedAt, String relatedIds) throws SQLException {
        try (PreparedStatement ps = connection.prepareStatement("UPDATE memory_records SET summary = ?, summary_updated_at = ?, related_memory_ids = ? WHERE id = ?")) {
            ps.setString(1, summary == null ? "" : summary);
            ps.setLong(2, updatedAt);
            ps.setString(3, relatedIds == null ? "" : relatedIds);
            ps.setString(4, recordId);
            ps.executeUpdate();
        }
    }

    private void evolveOldSummary(Connection connection, MemoryFact oldFact, MemoryFact newFact, long now) throws SQLException {
        Optional<MemoryRecord> oldRecord = recordById(connection, oldFact.recordId());
        if (oldRecord.isEmpty()) {
            return;
        }
        String evolved = consolidator.evolveSummary(oldRecord.get().summary(), oldFact, newFact);
        updateRecordSummary(connection, oldRecord.get().id(), evolved, now, oldRecord.get().relatedMemoryIds());
        MemorySummary summary = new MemorySummary(id("memsum"), now, now, oldRecord.get().serverId(), oldRecord.get().worldId(), oldRecord.get().id(),
                evolved, oldRecord.get().citationId(), oldRecord.get().relatedMemoryIds(), 1);
        insertSummary(connection, summary);
    }

    private List<MemoryLink> linkRelatedMemories(Connection connection, MemoryRecord record, long now) throws SQLException {
        List<MemoryLink> links = new ArrayList<>();
        try (PreparedStatement ps = connection.prepareStatement("""
                SELECT * FROM memory_records
                WHERE server_id = ? AND world_id = ? AND id <> ?
                  AND (conversation_id = ? OR npc_key = ? OR minecraft_player_uuid = ?)
                  AND created_at <= ?
                ORDER BY created_at DESC
                LIMIT 5
                """)) {
            ps.setString(1, record.serverId());
            ps.setString(2, record.worldId());
            ps.setString(3, record.id());
            ps.setString(4, record.conversationId());
            ps.setString(5, record.npcKey());
            ps.setString(6, record.minecraftPlayerUuid());
            ps.setLong(7, record.createdAt());
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    MemoryRecord target = record(rs);
                    MemoryLink link = new MemoryLink(id("memlink"), now, record.serverId(), record.worldId(), record.id(), target.id(),
                            "related_episode", consolidator.relationReason(record, target));
                    insertLink(connection, link);
                    links.add(link);
                }
            }
        }
        return List.copyOf(links);
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

    private Optional<MemoryOperation> operationById(Connection connection, String id) throws SQLException {
        try (PreparedStatement ps = connection.prepareStatement("SELECT * FROM memory_operations WHERE id = ?")) {
            ps.setString(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(operation(rs)) : Optional.empty();
            }
        }
    }

    private Optional<MemorySafetyLesson> safetyLessonById(Connection connection, String id) throws SQLException {
        try (PreparedStatement ps = connection.prepareStatement("SELECT * FROM memory_safety_lessons WHERE id = ?")) {
            ps.setString(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(safetyLesson(rs)) : Optional.empty();
            }
        }
    }

    private List<MemoryFact> factsByRecord(Connection connection, String recordId) throws SQLException {
        List<MemoryFact> values = new ArrayList<>();
        try (PreparedStatement ps = connection.prepareStatement("SELECT * FROM memory_facts WHERE record_id = ? ORDER BY created_at")) {
            ps.setString(1, recordId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    values.add(fact(rs));
                }
            }
        }
        return values;
    }

    private List<MemoryOperation> operationsByRecord(Connection connection, String recordId) throws SQLException {
        List<MemoryOperation> values = new ArrayList<>();
        try (PreparedStatement ps = connection.prepareStatement("SELECT * FROM memory_operations WHERE record_id = ? ORDER BY created_at")) {
            ps.setString(1, recordId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    values.add(operation(rs));
                }
            }
        }
        return values;
    }

    private List<MemorySummary> summariesByRecord(Connection connection, String recordId) throws SQLException {
        List<MemorySummary> values = new ArrayList<>();
        try (PreparedStatement ps = connection.prepareStatement("SELECT * FROM memory_summaries WHERE record_id = ? ORDER BY updated_at DESC")) {
            ps.setString(1, recordId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    values.add(memorySummary(rs));
                }
            }
        }
        return values;
    }

    private List<MemoryLink> linksByRecord(Connection connection, String recordId) throws SQLException {
        List<MemoryLink> values = new ArrayList<>();
        try (PreparedStatement ps = connection.prepareStatement("SELECT * FROM memory_links WHERE source_record_id = ? OR target_record_id = ? ORDER BY created_at DESC")) {
            ps.setString(1, recordId);
            ps.setString(2, recordId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    values.add(memoryLink(rs));
                }
            }
        }
        return values;
    }

    private List<MemorySafetyLesson> safetyLessonsByRecord(Connection connection, String recordId) throws SQLException {
        List<MemorySafetyLesson> values = new ArrayList<>();
        try (PreparedStatement ps = connection.prepareStatement("SELECT * FROM memory_safety_lessons WHERE source_record_id = ? ORDER BY created_at DESC")) {
            ps.setString(1, recordId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    values.add(safetyLesson(rs));
                }
            }
        }
        return values;
    }

    private ScoredMemory score(MemorySearchRequest request, MemoryRecord record, double[] queryVector, long newest) {
        String searchable = (record.text() + " " + record.summary() + " " + record.relatedMemoryIds()).toLowerCase(Locale.ROOT);
        double keyword = 0.0D;
        for (String token : request.query().toLowerCase(Locale.ROOT).split("[^\\p{L}\\p{N}_:.-]+")) {
            if (!token.isBlank() && searchable.contains(token)) {
                keyword += 0.35D;
            }
        }
        double vector = embeddings.cosine(queryVector, embeddings.embed(record.text() + " " + record.summary()));
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
                rs.getString("citation_id"), rs.getString("embedding"), rs.getString("summary"), rs.getLong("summary_updated_at"),
                rs.getString("related_memory_ids"));
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

    private static MemoryOperation operation(ResultSet rs) throws SQLException {
        return new MemoryOperation(rs.getString("id"), rs.getLong("created_at"), rs.getString("server_id"), rs.getString("world_id"),
                rs.getString("record_id"), rs.getString("op_type"), rs.getString("subject"), rs.getString("predicate"), rs.getString("op_value"),
                rs.getString("status"), rs.getString("reason"), rs.getString("proposed_by"), rs.getDouble("confidence"));
    }

    private static MemorySummary memorySummary(ResultSet rs) throws SQLException {
        return new MemorySummary(rs.getString("id"), rs.getLong("created_at"), rs.getLong("updated_at"), rs.getString("server_id"),
                rs.getString("world_id"), rs.getString("record_id"), rs.getString("summary"), rs.getString("raw_episode_citation_id"),
                rs.getString("related_memory_ids"), rs.getInt("evolution_count"));
    }

    private static MemoryLink memoryLink(ResultSet rs) throws SQLException {
        return new MemoryLink(rs.getString("id"), rs.getLong("created_at"), rs.getString("server_id"), rs.getString("world_id"),
                rs.getString("source_record_id"), rs.getString("target_record_id"), rs.getString("relation"), rs.getString("reason"));
    }

    private static MemorySafetyLesson safetyLesson(ResultSet rs) throws SQLException {
        return new MemorySafetyLesson(rs.getString("id"), rs.getLong("created_at"), rs.getString("server_id"), rs.getString("world_id"),
                rs.getString("subject"), rs.getString("lesson"), rs.getString("source_record_id"), rs.getString("conflict_id"),
                rs.getString("citation_ids"), rs.getString("status"));
    }

    private static long count(Connection connection, String table) throws SQLException {
        try (Statement statement = connection.createStatement(); ResultSet rs = statement.executeQuery("SELECT COUNT(*) FROM " + table)) {
            return rs.next() ? rs.getLong(1) : 0L;
        }
    }

    private static String blank(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value.strip();
    }

    private static String id(String prefix) {
        return prefix + "_" + UUID.randomUUID().toString().replace("-", "");
    }

    private static String joinTargetIds(List<MemoryLink> links) {
        return links == null || links.isEmpty() ? "" : String.join(",", links.stream().map(MemoryLink::targetRecordId).toList());
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

    private record ApplyFactResult(MemoryFact fact, List<MemoryConflict> conflicts, List<MemorySafetyLesson> safetyLessons) {
    }
}
