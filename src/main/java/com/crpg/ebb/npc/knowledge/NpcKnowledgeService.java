package com.crpg.ebb.npc.knowledge;

import com.crpg.ebb.npc.profile.NpcProfileDefinition;
import com.crpg.ebb.npc.profile.NpcProfileRegistry;
import com.crpg.ebb.state.NarrativeSavedData;
import net.minecraft.resources.Identifier;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

public final class NpcKnowledgeService {
    public static final String PACK_TAG_PREFIX = "kb_pack:";
    public static final String FACT_TAG_PREFIX = "kb_fact:";
    public static final String STANCE_TAG_PREFIX = "stance:";
    private static final NpcKnowledgeIndex INDEX = new NpcKnowledgeIndex();

    private NpcKnowledgeService() {}

    public static String addPack(NarrativeSavedData state, UUID playerUuid, String npcKey, String packId) {
        String pack = normalizeId(packId);
        state.setPlayerNpcState(playerUuid, npcKey, PACK_TAG_PREFIX + pack, true);
        return "npc_kb_pack_added:" + normalizeId(npcKey) + ":" + pack;
    }

    public static String addFact(NarrativeSavedData state, UUID playerUuid, String npcKey, String factId) {
        String fact = normalizeId(factId);
        state.setPlayerNpcState(playerUuid, npcKey, FACT_TAG_PREFIX + fact, true);
        return "npc_kb_fact_added:" + normalizeId(npcKey) + ":" + fact;
    }

    public static String shiftStance(NarrativeSavedData state, UUID playerUuid, String npcKey, String stance) {
        state.playerNpcStateTags(playerUuid).stream()
                .filter(tag -> tag.startsWith(normalizeId(npcKey) + "#" + STANCE_TAG_PREFIX))
                .map(tag -> tag.substring((normalizeId(npcKey) + "#").length()))
                .forEach(tag -> state.setPlayerNpcState(playerUuid, npcKey, tag, false));
        String normalized = normalize(stance);
        state.setPlayerNpcState(playerUuid, npcKey, STANCE_TAG_PREFIX + normalized, true);
        return "npc_stance_shift:" + normalizeId(npcKey) + "=" + normalized;
    }

    public static String promptContext(String npcKey, String query, NarrativeSavedData state, UUID playerUuid, long dayTime, int limit) {
        InspectResult result = inspect(npcKey, query, state, playerUuid, dayTime, limit);
        StringBuilder out = new StringBuilder();
        out.append("NPC KB visible chunks only for ").append(result.npcKey()).append(':');
        for (VisibleChunk chunk : result.visibleChunks()) {
            out.append("\n- [").append(chunk.packId()).append('/').append(chunk.chunk().id()).append("] ").append(chunk.chunk().text());
        }
        for (String fact : result.addedFacts()) {
            out.append("\n- [player-added-fact] ").append(fact);
        }
        result.stance().ifPresent(stance -> out.append("\n- [stance] ").append(stance));
        return out.toString();
    }

    public static List<String> inspectLines(String npcKey, String query, NarrativeSavedData state, UUID playerUuid, long dayTime, int limit) {
        InspectResult result = inspect(npcKey, query, state, playerUuid, dayTime, limit);
        List<String> lines = new ArrayList<>();
        lines.add("NPC KB inspect: " + result.npcKey());
        result.stance().ifPresent(stance -> lines.add("stance=" + stance));
        lines.add("visible_chunks=" + result.visibleChunks().size());
        for (VisibleChunk chunk : result.visibleChunks()) {
            lines.add("+ " + chunk.packId() + "/" + chunk.chunk().id() + " score=" + round(chunk.score()) + " text=" + abbreviate(chunk.chunk().text(), 120));
        }
        lines.add("hidden_chunks=" + result.hiddenChunks().size());
        for (VisibleChunk chunk : result.hiddenChunks()) {
            lines.add("- hidden " + chunk.packId() + "/" + chunk.chunk().id() + " secret=" + chunk.chunk().secret());
        }
        if (!result.addedFacts().isEmpty()) {
            lines.add("added_facts=" + result.addedFacts());
        }
        return List.copyOf(lines);
    }

    public static InspectResult inspect(String npcKey, String query, NarrativeSavedData state, UUID playerUuid, long dayTime, int limit) {
        String normalizedNpc = normalizeId(npcKey);
        Set<Identifier> packIds = new LinkedHashSet<>();
        Optional<NpcProfileDefinition> profile = parseIdentifier(normalizedNpc).flatMap(NpcProfileRegistry::byId);
        profile.ifPresent(definition -> packIds.addAll(definition.knowledge().initialPacks()));
        for (String tag : state.playerNpcStateTags(playerUuid)) {
            String prefix = normalizedNpc + "#" + PACK_TAG_PREFIX;
            if (tag.startsWith(prefix)) {
                parseIdentifier(tag.substring(prefix.length())).ifPresent(packIds::add);
            }
        }
        List<String> addedFacts = state.playerNpcStateTags(playerUuid).stream()
                .filter(tag -> tag.startsWith(normalizedNpc + "#" + FACT_TAG_PREFIX))
                .map(tag -> tag.substring((normalizedNpc + "#" + FACT_TAG_PREFIX).length()))
                .sorted()
                .toList();
        Optional<String> stance = state.playerNpcStateTags(playerUuid).stream()
                .filter(tag -> tag.startsWith(normalizedNpc + "#" + STANCE_TAG_PREFIX))
                .map(tag -> tag.substring((normalizedNpc + "#" + STANCE_TAG_PREFIX).length()))
                .findFirst();
        List<VisibleChunk> visible = new ArrayList<>();
        List<VisibleChunk> hidden = new ArrayList<>();
        for (Identifier packId : packIds) {
            Optional<NpcKnowledgePackDefinition> pack = NpcKnowledgeRegistry.byId(packId);
            if (pack.isEmpty()) continue;
            for (NpcKnowledgePackDefinition.Chunk chunk : pack.get().chunks()) {
                double score = INDEX.score(query, chunk.text());
                VisibleChunk row = new VisibleChunk(packId, chunk, score);
                if (chunk.visible(state, playerUuid, dayTime)) {
                    visible.add(row);
                } else {
                    hidden.add(row);
                }
            }
        }
        visible.sort(Comparator.comparingDouble(VisibleChunk::score).reversed().thenComparing(chunk -> chunk.packId().toString()).thenComparing(chunk -> chunk.chunk().id()));
        int capped = Math.max(1, Math.min(16, limit <= 0 ? 6 : limit));
        return new InspectResult(normalizedNpc, visible.stream().limit(capped).toList(), List.copyOf(hidden), addedFacts, stance);
    }

    private static Optional<Identifier> parseIdentifier(String value) {
        try {
            String safe = normalizeId(value);
            return Optional.of(safe.contains(":") ? Identifier.parse(safe) : Identifier.fromNamespaceAndPath("ebb", safe));
        } catch (RuntimeException ex) {
            return Optional.empty();
        }
    }

    private static String normalizeId(String value) {
        String safe = value == null || value.isBlank() ? "unknown" : value.strip().toLowerCase(Locale.ROOT);
        return safe.contains(":") ? safe : "ebb:demo/" + safe;
    }

    private static String normalize(String value) {
        return value == null || value.isBlank() ? "neutral" : value.strip().toLowerCase(Locale.ROOT).replace(' ', '_');
    }

    private static double round(double value) {
        return Math.round(value * 1000.0D) / 1000.0D;
    }

    private static String abbreviate(String value, int max) {
        String safe = value == null ? "" : value.replace('\n', ' ').strip();
        return safe.length() <= max ? safe : safe.substring(0, Math.max(0, max - 1)) + "…";
    }

    public record VisibleChunk(Identifier packId, NpcKnowledgePackDefinition.Chunk chunk, double score) {}
    public record InspectResult(String npcKey, List<VisibleChunk> visibleChunks, List<VisibleChunk> hiddenChunks, List<String> addedFacts, Optional<String> stance) {
        public InspectResult {
            visibleChunks = visibleChunks == null ? List.of() : List.copyOf(visibleChunks);
            hiddenChunks = hiddenChunks == null ? List.of() : List.copyOf(hiddenChunks);
            addedFacts = addedFacts == null ? List.of() : List.copyOf(addedFacts);
            stance = stance == null ? Optional.empty() : stance;
        }
    }
}
