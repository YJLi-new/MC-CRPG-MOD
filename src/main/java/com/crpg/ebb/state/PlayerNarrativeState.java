package com.crpg.ebb.state;

import com.crpg.ebb.story.StoryVarLayer;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class PlayerNarrativeState {
    public static final int DEFAULT_ATTRIBUTE_POINTS = 8;
    public static final int MAX_ACTIVE_FEATS = 4;

    public static final Codec<PlayerNarrativeState> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.unboundedMap(Codec.STRING, Codec.INT).optionalFieldOf("attributes", Map.of()).forGetter(PlayerNarrativeState::attributesForCodec),
            Codec.STRING.listOf().optionalFieldOf("flags", List.of()).forGetter(PlayerNarrativeState::flagsForCodec),
            Codec.unboundedMap(Codec.STRING, Codec.STRING).optionalFieldOf("variables", Map.of()).forGetter(PlayerNarrativeState::variablesForCodec),
            Codec.unboundedMap(Codec.STRING, Codec.STRING).optionalFieldOf("story_branch_vars", Map.of()).forGetter(PlayerNarrativeState::storyBranchVariablesForCodec),
            Codec.unboundedMap(Codec.STRING, Codec.STRING).optionalFieldOf("story_major_vars", Map.of()).forGetter(PlayerNarrativeState::storyMajorVariablesForCodec),
            Codec.unboundedMap(Codec.STRING, Codec.STRING).optionalFieldOf("story_minor_vars", Map.of()).forGetter(PlayerNarrativeState::storyMinorVariablesForCodec),
            Codec.unboundedMap(Codec.STRING, Codec.STRING).optionalFieldOf("quest_states", Map.of()).forGetter(PlayerNarrativeState::questStatesForCodec),
            Codec.STRING.listOf().optionalFieldOf("unlocked_feats", List.of()).forGetter(PlayerNarrativeState::unlockedFeatsForCodec),
            Codec.STRING.listOf().optionalFieldOf("active_feats", List.of()).forGetter(PlayerNarrativeState::activeFeatsForCodec),
            Codec.unboundedMap(Codec.STRING, Codec.INT).optionalFieldOf("relationships", Map.of()).forGetter(PlayerNarrativeState::relationshipsForCodec),
            Codec.STRING.listOf().optionalFieldOf("npc_state_tags", List.of()).forGetter(PlayerNarrativeState::npcStateTagsForCodec),
            Codec.STRING.listOf().optionalFieldOf("journal_entries", List.of()).forGetter(PlayerNarrativeState::journalEntriesForCodec),
            Codec.STRING.listOf().optionalFieldOf("discovered_clues", List.of()).forGetter(PlayerNarrativeState::discoveredCluesForCodec),
            Codec.unboundedMap(Codec.STRING, Codec.STRING).optionalFieldOf("narrative_states", Map.of()).forGetter(PlayerNarrativeState::narrativeStatesForCodec),
            Codec.unboundedMap(Codec.STRING, Codec.INT).optionalFieldOf("conflict_scores", Map.of()).forGetter(PlayerNarrativeState::conflictScoresForCodec),
            Codec.INT.optionalFieldOf("attribute_points", DEFAULT_ATTRIBUTE_POINTS).forGetter(PlayerNarrativeState::attributePoints)
    ).apply(instance, PlayerNarrativeState::new));

    private final Map<String, Integer> attributes = new LinkedHashMap<>();
    private final Set<String> flags = new LinkedHashSet<>();
    private final Map<String, String> variables = new LinkedHashMap<>();
    private final Map<String, String> storyBranchVariables = new LinkedHashMap<>();
    private final Map<String, String> storyMajorVariables = new LinkedHashMap<>();
    private final Map<String, String> storyMinorVariables = new LinkedHashMap<>();
    private final Map<String, String> questStates = new LinkedHashMap<>();
    private final Set<String> unlockedFeats = new LinkedHashSet<>();
    private final List<String> activeFeats = new java.util.ArrayList<>();
    private final Map<String, Integer> relationships = new LinkedHashMap<>();
    private final Set<String> npcStateTags = new LinkedHashSet<>();
    private final Set<String> journalEntries = new LinkedHashSet<>();
    private final Set<String> discoveredClues = new LinkedHashSet<>();
    private final Map<String, String> narrativeStates = new LinkedHashMap<>();
    private final Map<String, Integer> conflictScores = new LinkedHashMap<>();
    private int attributePoints = DEFAULT_ATTRIBUTE_POINTS;

    public PlayerNarrativeState() {
    }

    private PlayerNarrativeState(
            Map<String, Integer> attributes,
            List<String> flags,
            Map<String, String> variables,
            Map<String, String> storyBranchVariables,
            Map<String, String> storyMajorVariables,
            Map<String, String> storyMinorVariables,
            Map<String, String> questStates,
            List<String> unlockedFeats,
            List<String> activeFeats,
            Map<String, Integer> relationships,
            List<String> npcStateTags,
            List<String> journalEntries,
            List<String> discoveredClues,
            Map<String, String> narrativeStates,
            Map<String, Integer> conflictScores,
            int attributePoints
    ) {
        this.attributes.putAll(attributes);
        this.flags.addAll(flags);
        this.variables.putAll(variables);
        this.storyBranchVariables.putAll(storyBranchVariables);
        this.storyMajorVariables.putAll(storyMajorVariables);
        this.storyMinorVariables.putAll(storyMinorVariables);
        this.questStates.putAll(questStates);
        this.unlockedFeats.addAll(unlockedFeats);
        activeFeats.stream().distinct().limit(MAX_ACTIVE_FEATS).forEach(this.activeFeats::add);
        this.relationships.putAll(relationships);
        this.npcStateTags.addAll(npcStateTags);
        this.journalEntries.addAll(journalEntries);
        this.discoveredClues.addAll(discoveredClues);
        this.narrativeStates.putAll(narrativeStates);
        this.conflictScores.putAll(conflictScores);
        this.attributePoints = Math.max(0, attributePoints);
    }

    public Map<String, Integer> attributes() {
        return attributes;
    }

    public Set<String> flags() {
        return flags;
    }

    public Map<String, String> variables() {
        return variables;
    }

    public Map<String, String> storyVariables(StoryVarLayer layer) {
        return switch (layer) {
            case BRANCH -> storyBranchVariables;
            case MAJOR -> storyMajorVariables;
            case MINOR -> storyMinorVariables;
        };
    }

    public Map<String, String> questStates() {
        return questStates;
    }

    public Set<String> unlockedFeats() {
        return unlockedFeats;
    }

    public List<String> activeFeats() {
        return activeFeats;
    }

    public Map<String, Integer> relationships() {
        return relationships;
    }

    public Set<String> npcStateTags() {
        return npcStateTags;
    }

    public Set<String> journalEntries() {
        return journalEntries;
    }

    public Set<String> discoveredClues() {
        return discoveredClues;
    }

    public Map<String, String> narrativeStates() {
        return narrativeStates;
    }

    public Map<String, Integer> conflictScores() {
        return conflictScores;
    }

    public int attributePoints() {
        return attributePoints;
    }

    public void setAttributePoints(int attributePoints) {
        this.attributePoints = Math.max(0, attributePoints);
    }

    private Map<String, Integer> attributesForCodec() {
        return Map.copyOf(attributes);
    }

    private List<String> flagsForCodec() {
        return List.copyOf(flags);
    }

    private Map<String, String> variablesForCodec() {
        return Map.copyOf(variables);
    }

    private Map<String, String> storyBranchVariablesForCodec() {
        return Map.copyOf(storyBranchVariables);
    }

    private Map<String, String> storyMajorVariablesForCodec() {
        return Map.copyOf(storyMajorVariables);
    }

    private Map<String, String> storyMinorVariablesForCodec() {
        return Map.copyOf(storyMinorVariables);
    }

    private Map<String, String> questStatesForCodec() {
        return Map.copyOf(questStates);
    }

    private List<String> unlockedFeatsForCodec() {
        return List.copyOf(unlockedFeats);
    }

    private List<String> activeFeatsForCodec() {
        return List.copyOf(activeFeats);
    }

    private Map<String, Integer> relationshipsForCodec() {
        return Map.copyOf(relationships);
    }

    private List<String> npcStateTagsForCodec() {
        return List.copyOf(npcStateTags);
    }

    private List<String> journalEntriesForCodec() {
        return List.copyOf(journalEntries);
    }

    private List<String> discoveredCluesForCodec() {
        return List.copyOf(discoveredClues);
    }

    private Map<String, String> narrativeStatesForCodec() {
        return Map.copyOf(narrativeStates);
    }

    private Map<String, Integer> conflictScoresForCodec() {
        return Map.copyOf(conflictScores);
    }
}
