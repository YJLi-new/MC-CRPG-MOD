package com.crpg.ebb.state;

import com.crpg.ebb.EbbMod;
import com.crpg.ebb.attribute.AttributeDefinition;
import com.crpg.ebb.attribute.AttributeRegistry;
import com.crpg.ebb.relationship.RelationshipRegistry;
import com.crpg.ebb.story.StoryVarLayer;
import com.crpg.ebb.story.StoryVarValue;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.datafix.DataFixTypes;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

public final class NarrativeSavedData extends SavedData {
    public static final int CURRENT_SCHEMA_VERSION = 3;
    public static final int LEGACY_SCHEMA_VERSION = 1;
    public static final Codec<NarrativeSavedData> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.unboundedMap(Codec.STRING, PlayerNarrativeState.CODEC).optionalFieldOf("players", Map.of()).forGetter(NarrativeSavedData::playersForCodec),
            Codec.STRING.listOf().optionalFieldOf("world_flags", List.of()).forGetter(NarrativeSavedData::worldFlagsForCodec),
            Codec.unboundedMap(Codec.STRING, Codec.STRING).optionalFieldOf("world_variables", Map.of()).forGetter(NarrativeSavedData::worldVariablesForCodec),
            Codec.unboundedMap(Codec.STRING, Codec.STRING).optionalFieldOf("world_story_branch_vars", Map.of()).forGetter(NarrativeSavedData::worldStoryBranchVariablesForCodec),
            Codec.unboundedMap(Codec.STRING, Codec.STRING).optionalFieldOf("world_story_major_vars", Map.of()).forGetter(NarrativeSavedData::worldStoryMajorVariablesForCodec),
            Codec.unboundedMap(Codec.STRING, Codec.STRING).optionalFieldOf("world_story_minor_vars", Map.of()).forGetter(NarrativeSavedData::worldStoryMinorVariablesForCodec),
            Codec.STRING.listOf().optionalFieldOf("world_npc_state_tags", List.of()).forGetter(NarrativeSavedData::worldNpcStateTagsForCodec),
            Codec.unboundedMap(Codec.STRING, Codec.STRING).optionalFieldOf("promoted_npc_profiles", Map.of()).forGetter(NarrativeSavedData::promotedNpcProfilesForCodec),
            Codec.INT.optionalFieldOf("version", LEGACY_SCHEMA_VERSION).forGetter(NarrativeSavedData::versionForCodec)
    ).apply(instance, NarrativeSavedData::new));

    public static final SavedDataType<NarrativeSavedData> TYPE = new SavedDataType<>(
            EbbMod.id("narrative_state"),
            NarrativeSavedData::new,
            CODEC,
            DataFixTypes.SAVED_DATA_COMMAND_STORAGE
    );

    private final Map<String, PlayerNarrativeState> players = new LinkedHashMap<>();
    private final Set<String> worldFlags = new LinkedHashSet<>();
    private final Map<String, String> worldVariables = new LinkedHashMap<>();
    private final Map<String, String> worldStoryBranchVariables = new LinkedHashMap<>();
    private final Map<String, String> worldStoryMajorVariables = new LinkedHashMap<>();
    private final Map<String, String> worldStoryMinorVariables = new LinkedHashMap<>();
    private final Set<String> worldNpcStateTags = new LinkedHashSet<>();
    private final Map<String, String> promotedNpcProfiles = new LinkedHashMap<>();
    private final int version;

    public NarrativeSavedData() {
        this.version = CURRENT_SCHEMA_VERSION;
    }

    private NarrativeSavedData(
            Map<String, PlayerNarrativeState> players,
            List<String> worldFlags,
            Map<String, String> worldVariables,
            Map<String, String> worldStoryBranchVariables,
            Map<String, String> worldStoryMajorVariables,
            Map<String, String> worldStoryMinorVariables,
            List<String> worldNpcStateTags,
            Map<String, String> promotedNpcProfiles,
            int version
    ) {
        this.players.putAll(players);
        this.worldFlags.addAll(worldFlags);
        this.worldVariables.putAll(worldVariables);
        this.worldStoryBranchVariables.putAll(worldStoryBranchVariables);
        this.worldStoryMajorVariables.putAll(worldStoryMajorVariables);
        this.worldStoryMinorVariables.putAll(worldStoryMinorVariables);
        this.worldNpcStateTags.addAll(worldNpcStateTags);
        this.promotedNpcProfiles.putAll(promotedNpcProfiles);
        int loadedVersion = Math.max(LEGACY_SCHEMA_VERSION, version);
        migrateLoadedData(loadedVersion);
        this.version = CURRENT_SCHEMA_VERSION;
    }

    public static NarrativeSavedData get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(TYPE);
    }

    public static NarrativeSavedData get(ServerLevel level) {
        return get(level.getServer());
    }

    public PlayerNarrativeState player(UUID playerUuid) {
        return players.computeIfAbsent(playerUuid.toString(), ignored -> new PlayerNarrativeState());
    }

    public int getAttribute(UUID playerUuid, String key) {
        String normalized = normalize(key);
        String canonical = AttributeRegistry.canonicalKey(normalized);
        Map<String, Integer> attributes = player(playerUuid).attributes();
        if (attributes.containsKey(canonical)) {
            return attributes.get(canonical);
        }
        if (!canonical.equals(normalized) && attributes.containsKey(normalized)) {
            return attributes.get(normalized);
        }
        return AttributeRegistry.defaultScore(canonical);
    }

    public void setAttribute(UUID playerUuid, String key, int value) {
        String canonical = AttributeRegistry.canonicalKey(key);
        player(playerUuid).attributes().put(canonical, AttributeRegistry.clamp(canonical, value));
        setDirty();
    }

    public int getAttributePoints(UUID playerUuid) {
        return player(playerUuid).attributePoints();
    }

    public void setAttributePoints(UUID playerUuid, int points) {
        player(playerUuid).setAttributePoints(points);
        setDirty();
    }

    public void addAttributePoints(UUID playerUuid, int points) {
        PlayerNarrativeState state = player(playerUuid);
        state.setAttributePoints(state.attributePoints() + points);
        setDirty();
    }

    public SpendResult spendAttributePoint(UUID playerUuid, String key, int amount) {
        if (amount <= 0) {
            return new SpendResult(false, "amount_must_be_positive", getAttribute(playerUuid, key), getAttributePoints(playerUuid));
        }
        String canonical = AttributeRegistry.canonicalKey(key);
        AttributeDefinition definition = AttributeRegistry.byKey(canonical).orElse(null);
        if (definition == null) {
            return new SpendResult(false, "unknown_attribute:" + key, 0, getAttributePoints(playerUuid));
        }
        PlayerNarrativeState state = player(playerUuid);
        if (state.attributePoints() < amount) {
            return new SpendResult(false, "not_enough_attribute_points", getAttribute(playerUuid, canonical), state.attributePoints());
        }
        int current = getAttribute(playerUuid, canonical);
        int next = current + amount;
        if (next > definition.max()) {
            return new SpendResult(false, "attribute_max:" + definition.max(), current, state.attributePoints());
        }
        state.attributes().put(canonical, next);
        state.setAttributePoints(state.attributePoints() - amount);
        setDirty();
        return new SpendResult(true, canonical, next, state.attributePoints());
    }

    public void resetAttributes(UUID playerUuid) {
        PlayerNarrativeState state = player(playerUuid);
        state.attributes().clear();
        state.setAttributePoints(PlayerNarrativeState.DEFAULT_ATTRIBUTE_POINTS);
        setDirty();
    }

    public String attributeLine(UUID playerUuid, String key) {
        String canonical = AttributeRegistry.canonicalKey(key);
        AttributeDefinition definition = AttributeRegistry.byKey(canonical).orElse(null);
        String name = definition == null ? canonical : definition.displayName();
        return canonical + " (" + name + ") = " + getAttribute(playerUuid, canonical);
    }

    public boolean hasPlayerFlag(UUID playerUuid, String flag) {
        return player(playerUuid).flags().contains(flag);
    }

    public void setPlayerFlag(UUID playerUuid, String flag, boolean value) {
        if (value) {
            player(playerUuid).flags().add(flag);
        } else {
            player(playerUuid).flags().remove(flag);
        }
        setDirty();
    }

    public boolean hasWorldFlag(String flag) {
        return worldFlags.contains(flag);
    }

    public void setWorldFlag(String flag, boolean value) {
        if (value) {
            worldFlags.add(flag);
        } else {
            worldFlags.remove(flag);
        }
        setDirty();
    }

    public String getPlayerVariable(UUID playerUuid, String key) {
        return player(playerUuid).variables().getOrDefault(normalize(key), "");
    }

    public void setPlayerVariable(UUID playerUuid, String key, String value) {
        String normalized = normalize(key);
        if (value == null || value.isBlank()) {
            player(playerUuid).variables().remove(normalized);
        } else {
            player(playerUuid).variables().put(normalized, value);
        }
        setDirty();
    }

    public String getWorldVariable(String key) {
        return worldVariables.getOrDefault(normalize(key), "");
    }

    public void setWorldVariable(String key, String value) {
        String normalized = normalize(key);
        if (value == null || value.isBlank()) {
            worldVariables.remove(normalized);
        } else {
            worldVariables.put(normalized, value);
        }
        setDirty();
    }

    public String getPlayerStoryVariable(UUID playerUuid, StoryVarLayer layer, String key) {
        return player(playerUuid).storyVariables(layer).getOrDefault(normalize(key), "");
    }

    public void setPlayerStoryVariable(UUID playerUuid, StoryVarLayer layer, String key, String value) {
        setStoryValue(player(playerUuid).storyVariables(layer), key, value);
    }

    public int addPlayerStoryInt(UUID playerUuid, StoryVarLayer layer, String key, int amount) {
        Map<String, String> variables = player(playerUuid).storyVariables(layer);
        int current = StoryVarValue.ofString(variables.getOrDefault(normalize(key), "0")).asInt().orElse(0);
        int next = current + amount;
        setStoryValue(variables, key, StoryVarValue.ofInt(next).raw());
        return next;
    }

    public String getWorldStoryVariable(StoryVarLayer layer, String key) {
        return worldStoryVariables(layer).getOrDefault(normalize(key), "");
    }

    public void setWorldStoryVariable(StoryVarLayer layer, String key, String value) {
        setStoryValue(worldStoryVariables(layer), key, value);
    }

    public int addWorldStoryInt(StoryVarLayer layer, String key, int amount) {
        Map<String, String> variables = worldStoryVariables(layer);
        int current = StoryVarValue.ofString(variables.getOrDefault(normalize(key), "0")).asInt().orElse(0);
        int next = current + amount;
        setStoryValue(variables, key, StoryVarValue.ofInt(next).raw());
        return next;
    }

    public String getQuestState(UUID playerUuid, String questId) {
        return player(playerUuid).questStates().getOrDefault(normalizeId(questId), "not_started");
    }

    public void setQuestState(UUID playerUuid, String questId, String state) {
        String normalizedId = normalizeId(questId);
        if (state == null || state.isBlank() || "not_started".equalsIgnoreCase(state)) {
            player(playerUuid).questStates().remove(normalizedId);
        } else {
            player(playerUuid).questStates().put(normalizedId, normalize(state));
        }
        setDirty();
    }

    public boolean unlockFeat(UUID playerUuid, String featId) {
        boolean added = player(playerUuid).unlockedFeats().add(normalizeId(featId));
        if (added) {
            setDirty();
        }
        return added;
    }

    public FeatActivationResult activateFeat(UUID playerUuid, String featId) {
        PlayerNarrativeState state = player(playerUuid);
        String normalizedId = normalizeId(featId);
        if (!state.unlockedFeats().contains(normalizedId)) {
            return new FeatActivationResult(false, "feat_not_unlocked", List.copyOf(state.activeFeats()));
        }
        if (state.activeFeats().contains(normalizedId)) {
            return new FeatActivationResult(true, "already_active", List.copyOf(state.activeFeats()));
        }
        if (state.activeFeats().size() >= PlayerNarrativeState.MAX_ACTIVE_FEATS) {
            return new FeatActivationResult(false, "active_feat_slots_full", List.copyOf(state.activeFeats()));
        }
        state.activeFeats().add(normalizedId);
        setDirty();
        return new FeatActivationResult(true, "activated", List.copyOf(state.activeFeats()));
    }

    public boolean hasFeat(UUID playerUuid, String featId) {
        return player(playerUuid).unlockedFeats().contains(normalizeId(featId));
    }

    public boolean isFeatActive(UUID playerUuid, String featId) {
        return player(playerUuid).activeFeats().contains(normalizeId(featId));
    }

    public Set<String> unlockedFeatIds(UUID playerUuid) {
        return Set.copyOf(player(playerUuid).unlockedFeats());
    }

    public List<String> activeFeatIds(UUID playerUuid) {
        return List.copyOf(player(playerUuid).activeFeats());
    }

    public boolean unlockJournalEntry(UUID playerUuid, String entryId) {
        boolean added = player(playerUuid).journalEntries().add(normalizeId(entryId));
        if (added) {
            setDirty();
        }
        return added;
    }

    public boolean hasJournalEntry(UUID playerUuid, String entryId) {
        return player(playerUuid).journalEntries().contains(normalizeId(entryId));
    }

    public Set<String> journalEntryIds(UUID playerUuid) {
        return Set.copyOf(player(playerUuid).journalEntries());
    }

    public boolean revealClue(UUID playerUuid, String clueId) {
        boolean added = player(playerUuid).discoveredClues().add(normalizeId(clueId));
        if (added) {
            setDirty();
        }
        return added;
    }

    public boolean hasClue(UUID playerUuid, String clueId) {
        return player(playerUuid).discoveredClues().contains(normalizeId(clueId));
    }

    public Set<String> clueIds(UUID playerUuid) {
        return Set.copyOf(player(playerUuid).discoveredClues());
    }

    public String getScenePhase(UUID playerUuid, String sceneId) {
        return player(playerUuid).narrativeStates().getOrDefault(narrativeStateKey("scene", sceneId), "default");
    }

    public void setScenePhase(UUID playerUuid, String sceneId, String phase) {
        setNarrativeState(playerUuid, narrativeStateKey("scene", sceneId), phase);
    }

    public String getConflictState(UUID playerUuid, String conflictId) {
        return player(playerUuid).narrativeStates().getOrDefault(narrativeStateKey("conflict", conflictId), "not_started");
    }

    public void setConflictState(UUID playerUuid, String conflictId, String state) {
        setNarrativeState(playerUuid, narrativeStateKey("conflict", conflictId), state);
    }

    public String getConflictPhase(UUID playerUuid, String conflictId) {
        return player(playerUuid).narrativeStates().getOrDefault(narrativeStateKey("conflict_phase", conflictId), "not_started");
    }

    public void setConflictPhase(UUID playerUuid, String conflictId, String phase) {
        setNarrativeState(playerUuid, narrativeStateKey("conflict_phase", conflictId), phase);
    }

    public int getConflictScore(UUID playerUuid, String scoreKind, String conflictId) {
        return player(playerUuid).conflictScores().getOrDefault(conflictScoreKey(scoreKind, conflictId), 0);
    }

    public void setConflictScore(UUID playerUuid, String scoreKind, String conflictId, int value) {
        player(playerUuid).conflictScores().put(conflictScoreKey(scoreKind, conflictId), value);
        setDirty();
    }

    public int addConflictScore(UUID playerUuid, String scoreKind, String conflictId, int amount) {
        String key = conflictScoreKey(scoreKind, conflictId);
        int next = player(playerUuid).conflictScores().getOrDefault(key, 0) + amount;
        player(playerUuid).conflictScores().put(key, next);
        setDirty();
        return next;
    }

    public int getRelation(UUID playerUuid, String relationshipKey) {
        String normalized = normalizeId(relationshipKey);
        return player(playerUuid).relationships().getOrDefault(normalized, RelationshipRegistry.defaultScore(normalized));
    }

    public void setRelation(UUID playerUuid, String relationshipKey, int value) {
        player(playerUuid).relationships().put(normalizeId(relationshipKey), value);
        setDirty();
    }

    public int addRelation(UUID playerUuid, String relationshipKey, int amount) {
        String normalized = normalizeId(relationshipKey);
        int next = getRelation(playerUuid, normalized) + amount;
        player(playerUuid).relationships().put(normalized, next);
        setDirty();
        return next;
    }

    public boolean hasPlayerNpcState(UUID playerUuid, String npcKey, String tag) {
        return player(playerUuid).npcStateTags().contains(npcStateKey(npcKey, tag));
    }

    public void setPlayerNpcState(UUID playerUuid, String npcKey, String tag, boolean value) {
        Set<String> tags = player(playerUuid).npcStateTags();
        if (value) {
            tags.add(npcStateKey(npcKey, tag));
        } else {
            tags.remove(npcStateKey(npcKey, tag));
        }
        setDirty();
    }

    public boolean hasWorldNpcState(String npcKey, String tag) {
        return worldNpcStateTags.contains(npcStateKey(npcKey, tag));
    }

    public void setWorldNpcState(String npcKey, String tag, boolean value) {
        if (value) {
            worldNpcStateTags.add(npcStateKey(npcKey, tag));
        } else {
            worldNpcStateTags.remove(npcStateKey(npcKey, tag));
        }
        setDirty();
    }

    public Set<String> playerNpcStateTags(UUID playerUuid) {
        return Set.copyOf(player(playerUuid).npcStateTags());
    }

    public Set<String> worldNpcStateTags() {
        return Set.copyOf(worldNpcStateTags);
    }

    public void putPromotedNpcProfile(String profileId, JsonObject profileJson) {
        String normalized = normalizeId(profileId);
        promotedNpcProfiles.put(normalized, profileJson == null ? "{}" : profileJson.toString());
        setDirty();
    }

    public Optional<JsonObject> promotedNpcProfile(String profileId) {
        String raw = promotedNpcProfiles.get(normalizeId(profileId));
        if (raw == null || raw.isBlank()) {
            return Optional.empty();
        }
        try {
            return Optional.of(JsonParser.parseString(raw).getAsJsonObject());
        } catch (RuntimeException ex) {
            return Optional.empty();
        }
    }

    public boolean hasPromotedNpcProfile(String profileId) {
        return promotedNpcProfiles.containsKey(normalizeId(profileId));
    }

    public boolean removePromotedNpcProfile(String profileId) {
        boolean removed = promotedNpcProfiles.remove(normalizeId(profileId)) != null;
        if (removed) {
            setDirty();
        }
        return removed;
    }

    public int promotedNpcProfileCount() {
        return promotedNpcProfiles.size();
    }

    public Set<String> promotedNpcProfileIds() {
        return Set.copyOf(promotedNpcProfiles.keySet());
    }

    public int playerCount() {
        return players.size();
    }

    public int worldFlagCount() {
        return worldFlags.size();
    }

    public int worldVariableCount() {
        return worldVariables.size();
    }

    public int worldStoryVariableCount() {
        return worldStoryBranchVariables.size() + worldStoryMajorVariables.size() + worldStoryMinorVariables.size();
    }

    public int playerStoryVariableCount() {
        return players.values().stream()
                .mapToInt(player -> player.storyVariables(StoryVarLayer.BRANCH).size()
                        + player.storyVariables(StoryVarLayer.MAJOR).size()
                        + player.storyVariables(StoryVarLayer.MINOR).size())
                .sum();
    }

    public int questStateCount() {
        return players.values().stream().mapToInt(player -> player.questStates().size()).sum();
    }

    public int featCount() {
        return players.values().stream().mapToInt(player -> player.unlockedFeats().size()).sum();
    }

    public int journalEntryCount() {
        return players.values().stream().mapToInt(player -> player.journalEntries().size()).sum();
    }

    public int clueCount() {
        return players.values().stream().mapToInt(player -> player.discoveredClues().size()).sum();
    }

    public int narrativeStateCount() {
        return players.values().stream().mapToInt(player -> player.narrativeStates().size()).sum();
    }

    public int conflictScoreCount() {
        return players.values().stream().mapToInt(player -> player.conflictScores().size()).sum();
    }

    public int relationshipCount() {
        return players.values().stream().mapToInt(player -> player.relationships().size()).sum();
    }

    public int npcStateTagCount() {
        return worldNpcStateTags.size() + players.values().stream().mapToInt(player -> player.npcStateTags().size()).sum();
    }

    public String summaryLine() {
        return "narrative_state(players=" + players.size()
                + ", world_flags=" + worldFlags.size()
                + ", world_variables=" + worldVariables.size()
                + ", story_vars=" + (worldStoryVariableCount() + playerStoryVariableCount())
                + ", quest_states=" + questStateCount()
                + ", feats=" + featCount()
                + ", journal_entries=" + journalEntryCount()
                + ", relationships=" + relationshipCount()
                + ", npc_state_tags=" + npcStateTagCount()
                + ", promoted_npc_profiles=" + promotedNpcProfileCount()
                + ", clues=" + clueCount()
                + ", narrative_states=" + narrativeStateCount()
                + ", conflict_scores=" + conflictScoreCount()
                + ", version=" + version + ")";
    }

    public JsonObject debugSnapshot() {
        JsonObject root = new JsonObject();
        root.addProperty("version", version);
        JsonArray worldFlagArray = new JsonArray();
        worldFlags.forEach(worldFlagArray::add);
        root.add("world_flags", worldFlagArray);
        JsonObject worldVariableObject = new JsonObject();
        worldVariables.forEach(worldVariableObject::addProperty);
        root.add("world_variables", worldVariableObject);
        root.add("world_story_vars", storyVarObject(worldStoryBranchVariables, worldStoryMajorVariables, worldStoryMinorVariables));
        JsonArray worldNpcStateTagsJson = new JsonArray();
        worldNpcStateTags.forEach(worldNpcStateTagsJson::add);
        root.add("world_npc_state_tags", worldNpcStateTagsJson);
        JsonObject promotedProfilesJson = new JsonObject();
        promotedNpcProfiles.forEach((id, raw) -> {
            try {
                promotedProfilesJson.add(id, JsonParser.parseString(raw));
            } catch (RuntimeException ex) {
                promotedProfilesJson.addProperty(id, raw);
            }
        });
        root.add("promoted_npc_profiles", promotedProfilesJson);
        JsonObject playerObject = new JsonObject();
        for (Map.Entry<String, PlayerNarrativeState> entry : players.entrySet()) {
            PlayerNarrativeState player = entry.getValue();
            JsonObject playerJson = new JsonObject();
            JsonObject attributesJson = new JsonObject();
            player.attributes().forEach(attributesJson::addProperty);
            playerJson.add("attributes", attributesJson);
            playerJson.addProperty("attribute_points", player.attributePoints());
            JsonArray flagsJson = new JsonArray();
            player.flags().forEach(flagsJson::add);
            playerJson.add("flags", flagsJson);
            JsonObject variablesJson = new JsonObject();
            player.variables().forEach(variablesJson::addProperty);
            playerJson.add("variables", variablesJson);
            playerJson.add("story_vars", storyVarObject(
                    player.storyVariables(StoryVarLayer.BRANCH),
                    player.storyVariables(StoryVarLayer.MAJOR),
                    player.storyVariables(StoryVarLayer.MINOR)
            ));
            JsonObject questsJson = new JsonObject();
            player.questStates().forEach(questsJson::addProperty);
            playerJson.add("quest_states", questsJson);
            JsonArray unlockedFeatsJson = new JsonArray();
            player.unlockedFeats().forEach(unlockedFeatsJson::add);
            playerJson.add("unlocked_feats", unlockedFeatsJson);
            JsonArray activeFeatsJson = new JsonArray();
            player.activeFeats().forEach(activeFeatsJson::add);
            playerJson.add("active_feats", activeFeatsJson);
            JsonObject relationshipsJson = new JsonObject();
            player.relationships().forEach(relationshipsJson::addProperty);
            playerJson.add("relationships", relationshipsJson);
            JsonArray npcStateTagsJson = new JsonArray();
            player.npcStateTags().forEach(npcStateTagsJson::add);
            playerJson.add("npc_state_tags", npcStateTagsJson);
            JsonArray journalEntriesJson = new JsonArray();
            player.journalEntries().forEach(journalEntriesJson::add);
            playerJson.add("journal_entries", journalEntriesJson);
            JsonArray cluesJson = new JsonArray();
            player.discoveredClues().forEach(cluesJson::add);
            playerJson.add("discovered_clues", cluesJson);
            JsonObject narrativeStatesJson = new JsonObject();
            player.narrativeStates().forEach(narrativeStatesJson::addProperty);
            playerJson.add("narrative_states", narrativeStatesJson);
            JsonObject conflictScoresJson = new JsonObject();
            player.conflictScores().forEach(conflictScoresJson::addProperty);
            playerJson.add("conflict_scores", conflictScoresJson);
            playerObject.add(entry.getKey(), playerJson);
        }
        root.add("players", playerObject);
        return root;
    }

    public List<String> storyVariableDebugLines(int limit) {
        List<String> lines = new ArrayList<>();
        lines.add("Story variables (world=" + worldStoryVariableCount() + ", player=" + playerStoryVariableCount() + "):");
        appendLayerLines(lines, "- world", worldStoryBranchVariables, worldStoryMajorVariables, worldStoryMinorVariables, limit);
        players.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .limit(Math.max(0, limit))
                .forEach(entry -> appendLayerLines(lines, "- player " + entry.getKey(),
                        entry.getValue().storyVariables(StoryVarLayer.BRANCH),
                        entry.getValue().storyVariables(StoryVarLayer.MAJOR),
                        entry.getValue().storyVariables(StoryVarLayer.MINOR),
                        limit));
        if (worldStoryVariableCount() + playerStoryVariableCount() == 0) {
            lines.add("- none");
        }
        return List.copyOf(lines);
    }

    public List<String> questFeatDebugLines(int limit) {
        List<String> lines = new ArrayList<>();
        lines.add("Quest/Feat/Journal state (quest_states=" + questStateCount()
                + ", feats=" + featCount()
                + ", journal_entries=" + journalEntryCount() + "):");
        if (players.isEmpty() || (questStateCount() == 0 && featCount() == 0 && journalEntryCount() == 0)) {
            lines.add("- none");
            return List.copyOf(lines);
        }
        players.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .limit(Math.max(0, limit))
                .forEach(entry -> {
                    PlayerNarrativeState player = entry.getValue();
                    lines.add("- player " + entry.getKey());
                    player.questStates().entrySet().stream()
                            .sorted(Map.Entry.comparingByKey())
                            .limit(Math.max(0, limit))
                            .forEach(quest -> lines.add("  quest." + quest.getKey() + " = " + quest.getValue()));
                    lines.add("  unlocked_feats=" + player.unlockedFeats());
                    lines.add("  active_feats=" + player.activeFeats());
                    lines.add("  journal_entries=" + player.journalEntries());
                });
        return List.copyOf(lines);
    }

    public List<String> relationshipDebugLines(int limit) {
        List<String> lines = new ArrayList<>();
        lines.add("Relationship/NPC memory state (relationships=" + relationshipCount()
                + ", npc_state_tags=" + npcStateTagCount() + "):");
        if (relationshipCount() == 0 && npcStateTagCount() == 0) {
            lines.add("- none");
            return List.copyOf(lines);
        }
        appendSetLines(lines, "- world.npc_state", worldNpcStateTags, limit);
        players.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .limit(Math.max(0, limit))
                .forEach(entry -> {
                    PlayerNarrativeState player = entry.getValue();
                    lines.add("- player " + entry.getKey());
                    appendIntMapLines(lines, "  relation", player.relationships(), limit);
                    appendSetLines(lines, "  npc_state", player.npcStateTags(), limit);
                });
        return List.copyOf(lines);
    }

    public List<String> promotedNpcProfileDebugLines(int limit) {
        List<String> lines = new ArrayList<>();
        lines.add("Promoted NPC profiles (" + promotedNpcProfiles.size() + "):");
        if (promotedNpcProfiles.isEmpty()) {
            lines.add("- none");
            return List.copyOf(lines);
        }
        promotedNpcProfiles.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .limit(Math.max(0, limit))
                .forEach(entry -> promotedNpcProfile(entry.getKey()).ifPresentOrElse(profile -> {
                    String display = profile.has("display_name") ? profile.get("display_name").getAsString() : entry.getKey();
                    String tier = profile.has("tier") ? profile.get("tier").getAsString() : "-";
                    String entity = profile.has("entity_uuid") ? profile.get("entity_uuid").getAsString() : "-";
                    lines.add("- " + entry.getKey() + " tier=" + tier + " display='" + display + "' entity=" + entity);
                }, () -> lines.add("- " + entry.getKey() + " invalid_json")));
        if (promotedNpcProfiles.size() > limit) {
            lines.add("- ... " + (promotedNpcProfiles.size() - limit) + " more");
        }
        return List.copyOf(lines);
    }

    public List<String> investigationDebugLines(int limit) {
        List<String> lines = new ArrayList<>();
        lines.add("Investigation/Conflict state (clues=" + clueCount()
                + ", narrative_states=" + narrativeStateCount()
                + ", conflict_scores=" + conflictScoreCount() + "):");
        if (clueCount() == 0 && narrativeStateCount() == 0 && conflictScoreCount() == 0) {
            lines.add("- none");
            return List.copyOf(lines);
        }
        players.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .limit(Math.max(0, limit))
                .forEach(entry -> {
                    PlayerNarrativeState player = entry.getValue();
                    lines.add("- player " + entry.getKey());
                    appendSetLines(lines, "  clue", player.discoveredClues(), limit);
                    appendMapLines(lines, "  state", player.narrativeStates(), limit);
                    appendIntMapLines(lines, "  conflict_score", player.conflictScores(), limit);
                });
        return List.copyOf(lines);
    }

    private Map<String, PlayerNarrativeState> playersForCodec() {
        return Map.copyOf(players);
    }

    private List<String> worldFlagsForCodec() {
        return List.copyOf(worldFlags);
    }

    private Map<String, String> worldVariablesForCodec() {
        return Map.copyOf(worldVariables);
    }

    private Map<String, String> worldStoryBranchVariablesForCodec() {
        return Map.copyOf(worldStoryBranchVariables);
    }

    private Map<String, String> worldStoryMajorVariablesForCodec() {
        return Map.copyOf(worldStoryMajorVariables);
    }

    private Map<String, String> worldStoryMinorVariablesForCodec() {
        return Map.copyOf(worldStoryMinorVariables);
    }

    private List<String> worldNpcStateTagsForCodec() {
        return List.copyOf(worldNpcStateTags);
    }

    private Map<String, String> promotedNpcProfilesForCodec() {
        return Map.copyOf(promotedNpcProfiles);
    }

    public int schemaVersion() {
        return version;
    }

    private int versionForCodec() {
        return version;
    }

    private static String normalize(String key) {
        return key.trim().toLowerCase(Locale.ROOT);
    }

    private static String normalizeId(String id) {
        return id.trim().toLowerCase(Locale.ROOT);
    }

    private static String npcStateKey(String npcKey, String tag) {
        return normalizeId(npcKey) + "#" + normalize(tag);
    }

    private static String narrativeStateKey(String kind, String id) {
        return normalize(kind) + ":" + normalizeId(id);
    }

    private static String conflictScoreKey(String kind, String id) {
        return normalize(kind) + ":" + normalizeId(id);
    }

    private void migrateLoadedData(int loadedVersion) {
        if (loadedVersion < 2) {
            migrateConflictPhasesFromLegacyStates();
        }
    }

    private void migrateConflictPhasesFromLegacyStates() {
        for (PlayerNarrativeState player : players.values()) {
            List<Map.Entry<String, String>> legacyConflictStates = player.narrativeStates().entrySet().stream()
                    .filter(entry -> entry.getKey().startsWith("conflict:"))
                    .filter(entry -> !entry.getKey().startsWith("conflict_phase:"))
                    .toList();
            for (Map.Entry<String, String> entry : legacyConflictStates) {
                String conflictId = entry.getKey().substring("conflict:".length());
                String phaseKey = narrativeStateKey("conflict_phase", conflictId);
                player.narrativeStates().putIfAbsent(phaseKey, inferConflictPhaseFromLegacyState(player, conflictId, entry.getValue()));
            }
        }
    }

    private static String inferConflictPhaseFromLegacyState(PlayerNarrativeState player, String conflictId, String conflictState) {
        String normalizedState = normalize(conflictState == null ? "" : conflictState);
        if (normalizedState.startsWith("failed")) {
            return "consequence";
        }
        if (normalizedState.startsWith("resolved")) {
            return "resolution";
        }
        if (player.conflictScores().getOrDefault(conflictScoreKey("resolve", conflictId), 0) > 0) {
            return "turn";
        }
        if (player.conflictScores().getOrDefault(conflictScoreKey("stress", conflictId), 0) > 0) {
            return "pressure";
        }
        return "setup";
    }

    private void setNarrativeState(UUID playerUuid, String key, String value) {
        if (value == null || value.isBlank() || "default".equalsIgnoreCase(value) || "not_started".equalsIgnoreCase(value)) {
            player(playerUuid).narrativeStates().remove(key);
        } else {
            player(playerUuid).narrativeStates().put(key, normalize(value));
        }
        setDirty();
    }

    private void setStoryValue(Map<String, String> variables, String key, String value) {
        String normalized = normalize(key);
        if (value == null || value.isBlank()) {
            variables.remove(normalized);
        } else {
            variables.put(normalized, StoryVarValue.ofString(value).raw());
        }
        setDirty();
    }

    private Map<String, String> worldStoryVariables(StoryVarLayer layer) {
        return switch (layer) {
            case BRANCH -> worldStoryBranchVariables;
            case MAJOR -> worldStoryMajorVariables;
            case MINOR -> worldStoryMinorVariables;
        };
    }

    private static JsonObject storyVarObject(Map<String, String> branch, Map<String, String> major, Map<String, String> minor) {
        JsonObject object = new JsonObject();
        object.add(StoryVarLayer.BRANCH.serializedName(), stringMapObject(branch));
        object.add(StoryVarLayer.MAJOR.serializedName(), stringMapObject(major));
        object.add(StoryVarLayer.MINOR.serializedName(), stringMapObject(minor));
        return object;
    }

    private static JsonObject stringMapObject(Map<String, String> values) {
        JsonObject object = new JsonObject();
        values.forEach(object::addProperty);
        return object;
    }

    private static void appendLayerLines(
            List<String> lines,
            String prefix,
            Map<String, String> branch,
            Map<String, String> major,
            Map<String, String> minor,
            int limit
    ) {
        appendMapLines(lines, prefix + ".branch", branch, limit);
        appendMapLines(lines, prefix + ".major", major, limit);
        appendMapLines(lines, prefix + ".minor", minor, limit);
    }

    private static void appendMapLines(List<String> lines, String prefix, Map<String, String> values, int limit) {
        values.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .limit(Math.max(0, limit))
                .forEach(entry -> lines.add(prefix + "." + entry.getKey() + " = " + entry.getValue()));
        if (values.size() > limit) {
            lines.add(prefix + " ... " + (values.size() - limit) + " more");
        }
    }

    private static void appendIntMapLines(List<String> lines, String prefix, Map<String, Integer> values, int limit) {
        values.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .limit(Math.max(0, limit))
                .forEach(entry -> lines.add(prefix + "." + entry.getKey() + " = " + entry.getValue()));
        if (values.size() > limit) {
            lines.add(prefix + " ... " + (values.size() - limit) + " more");
        }
        if (values.isEmpty()) {
            lines.add(prefix + ": none");
        }
    }

    private static void appendSetLines(List<String> lines, String prefix, Set<String> values, int limit) {
        values.stream()
                .sorted()
                .limit(Math.max(0, limit))
                .forEach(value -> lines.add(prefix + "." + value));
        if (values.size() > limit) {
            lines.add(prefix + " ... " + (values.size() - limit) + " more");
        }
        if (values.isEmpty()) {
            lines.add(prefix + ": none");
        }
    }

    public record SpendResult(boolean success, String reason, int score, int remainingPoints) {
    }

    public record FeatActivationResult(boolean success, String reason, List<String> activeFeats) {
    }
}
