package com.crpg.ebb.dialogue;

import com.crpg.ebb.state.NarrativeSavedData;
import com.crpg.ebb.story.StoryVarLayer;
import com.crpg.ebb.story.StoryVarValue;
import com.google.gson.JsonObject;
import net.minecraft.util.GsonHelper;

import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;

public record DialogueCondition(
        ConditionType type,
        DialogueScope scope,
        StoryVarLayer storyLayer,
        String key,
        boolean expected,
        Optional<String> value,
        Optional<Integer> min,
        Optional<Integer> max
) {
    public DialogueCondition {
        value = value == null ? Optional.empty() : value;
        min = min == null ? Optional.empty() : min;
        max = max == null ? Optional.empty() : max;
        storyLayer = storyLayer == null ? StoryVarLayer.MINOR : storyLayer;
    }

    public enum ConditionType {
        FLAG,
        VARIABLE_EQUALS,
        ATTRIBUTE_AT_LEAST,
        STORY_VAR,
        QUEST_STATE,
        HAS_FEAT,
        FEAT_ACTIVE,
        HAS_JOURNAL_ENTRY,
        RELATION_AT_LEAST,
        NPC_STATE,
        TIME_WINDOW,
        CLUE_FOUND,
        CONFLICT_STATE,
        SCENE_PHASE;

        static Optional<ConditionType> parse(String value) {
            if (value == null || value.isBlank()) {
                return Optional.empty();
            }
            String normalized = value.trim().toUpperCase(Locale.ROOT).replace('-', '_');
            normalized = switch (normalized) {
                case "HAS_FLAG", "NOT_FLAG", "HAS_TRAIT", "HAS_THOUGHT", "TRAIT", "THOUGHT" -> "FLAG";
                case "VAR_EQUALS", "VARIABLE", "VARIABLE_EQUALS" -> "VARIABLE_EQUALS";
                case "SKILL_AT_LEAST", "ATTRIBUTE", "ATTRIBUTE_AT_LEAST" -> "ATTRIBUTE_AT_LEAST";
                case "STORY_VARIABLE", "STORY_VAR_EQUALS", "STORY_VARIABLE_EQUALS", "STORY_VAR_AT_LEAST" -> "STORY_VAR";
                case "QUEST", "QUEST_BRANCH", "QUEST_BRANCH_STATE" -> "QUEST_STATE";
                case "FEAT", "UNLOCKED_FEAT" -> "HAS_FEAT";
                case "HAS_ACTIVE_FEAT", "ACTIVE_FEAT", "FEAT_ACTIVE", "SLOTTED_FEAT", "EQUIPPED_FEAT" -> "FEAT_ACTIVE";
                case "JOURNAL", "JOURNAL_ENTRY" -> "HAS_JOURNAL_ENTRY";
                case "CLUE", "HAS_CLUE", "CLUE_FOUND" -> "CLUE_FOUND";
                case "RELATION", "RELATIONSHIP", "RELATIONSHIP_AT_LEAST" -> "RELATION_AT_LEAST";
                case "NPC_TAG", "NPC_STATE_TAG", "HAS_NPC_STATE", "HAS_NPC_TAG" -> "NPC_STATE";
                case "TIME", "TIME_OF_DAY", "DAY_TIME" -> "TIME_WINDOW";
                case "CONFLICT", "CONFLICT_STATUS" -> "CONFLICT_STATE";
                case "SCENE", "SCENE_STATUS" -> "SCENE_PHASE";
                default -> normalized;
            };
            try {
                return Optional.of(ConditionType.valueOf(normalized));
            } catch (IllegalArgumentException ex) {
                return Optional.empty();
            }
        }
    }

    static Optional<DialogueCondition> parse(JsonObject json, String path, List<String> messages) {
        String rawType = optionalString(json, "type").orElse("flag");
        Optional<ConditionType> type = ConditionType.parse(rawType);
        if (type.isEmpty()) {
            messages.add(path + ": unsupported condition type=" + rawType);
            return Optional.empty();
        }
        Optional<DialogueScope> scope = DialogueScope.parse(optionalString(json, "scope").orElse("player"));
        if (scope.isEmpty()) {
            messages.add(path + ": scope must be player or world");
            return Optional.empty();
        }
        StoryVarLayer storyLayer = StoryVarLayer.parse(optionalString(json, "layer").orElse("minor"))
                .orElse(StoryVarLayer.MINOR);

        boolean expected = type.get() == ConditionType.FLAG
                ? optionalBoolean(json, "expected")
                .or(() -> optionalBoolean(json, "value"))
                .orElse(!"not_flag".equalsIgnoreCase(rawType))
                : optionalBoolean(json, "expected").orElse(true);
        Optional<String> key = optionalString(json, "id")
                .or(() -> optionalString(json, "key"))
                .or(() -> optionalString(json, "flag"))
                .or(() -> optionalString(json, "trait"))
                .or(() -> optionalString(json, "thought"))
                .or(() -> optionalString(json, "story_var"))
                .or(() -> optionalString(json, "storyVar"))
                .or(() -> optionalString(json, "quest"))
                .or(() -> optionalString(json, "quest_branch"))
                .or(() -> optionalString(json, "questBranch"))
                .or(() -> optionalString(json, "feat"))
                .or(() -> optionalString(json, "journal"))
                .or(() -> optionalString(json, "journal_entry"))
                .or(() -> optionalString(json, "journalEntry"))
                .or(() -> optionalString(json, "clue"))
                .or(() -> optionalString(json, "relation"))
                .or(() -> optionalString(json, "relationship"))
                .or(() -> optionalString(json, "npc"))
                .or(() -> optionalString(json, "npc_id"))
                .or(() -> optionalString(json, "npcId"))
                .or(() -> optionalString(json, "time"))
                .or(() -> optionalString(json, "conflict"))
                .or(() -> optionalString(json, "scene"))
                .or(() -> optionalString(json, "attribute"));
        if ((key.isEmpty() || key.get().isBlank()) && type.get() == ConditionType.TIME_WINDOW) {
            key = Optional.of("_time");
        }
        if (key.isEmpty() || key.get().isBlank()) {
            messages.add(path + ": missing id/key/flag/trait/thought/attribute string");
            return Optional.empty();
        }

        String normalizedKey = normalizeKey(rawType, key.get());
        Optional<String> stringValue = optionalString(json, "equals")
                .or(() -> optionalString(json, "string_value"))
                .or(() -> optionalString(json, "tag"))
                .or(() -> optionalString(json, "state"))
                .or(() -> optionalString(json, "phase"))
                .or(() -> optionalString(json, "state_tag"))
                .or(() -> optionalString(json, "stateTag"))
                .or(() -> type.get() == ConditionType.VARIABLE_EQUALS || type.get() == ConditionType.STORY_VAR
                        ? optionalScalarString(json, "value") : Optional.empty());
        Optional<Integer> min = optionalInt(json, "min")
                .or(() -> optionalInt(json, "at_least"))
                .or(() -> optionalInt(json, "start"));
        Optional<Integer> max = optionalInt(json, "max").or(() -> optionalInt(json, "end"));
        if (type.get() == ConditionType.VARIABLE_EQUALS && stringValue.isEmpty()) {
            messages.add(path + ": variable condition requires equals/string_value/value");
            return Optional.empty();
        }
        if (type.get() == ConditionType.ATTRIBUTE_AT_LEAST && min.isEmpty()) {
            messages.add(path + ": attribute_at_least condition requires min/at_least");
            return Optional.empty();
        }
        if (type.get() == ConditionType.STORY_VAR && stringValue.isEmpty() && min.isEmpty()) {
            messages.add(path + ": story_var condition requires equals/string_value/value or min/at_least");
            return Optional.empty();
        }
        if (type.get() == ConditionType.QUEST_STATE && stringValue.isEmpty()) {
            stringValue = Optional.of("take_rooted");
        }
        if (type.get() == ConditionType.CONFLICT_STATE && stringValue.isEmpty()) {
            stringValue = Optional.of("active");
        }
        if (type.get() == ConditionType.SCENE_PHASE && stringValue.isEmpty()) {
            stringValue = Optional.of("default");
        }
        if (type.get() == ConditionType.RELATION_AT_LEAST && min.isEmpty()) {
            messages.add(path + ": relation_at_least condition requires min/at_least");
            return Optional.empty();
        }
        if (type.get() == ConditionType.NPC_STATE && stringValue.isEmpty()) {
            messages.add(path + ": npc_state condition requires tag/state/state_tag");
            return Optional.empty();
        }
        if (type.get() == ConditionType.TIME_WINDOW && (min.isEmpty() || max.isEmpty())) {
            messages.add(path + ": time_window condition requires start/min and end/max");
            return Optional.empty();
        }
        return Optional.of(new DialogueCondition(type.get(), scope.get(), storyLayer, normalizedKey, expected, stringValue, min, max));
    }

    public boolean matches(NarrativeSavedData state, UUID playerUuid) {
        return matches(state, playerUuid, -1L);
    }

    public boolean matches(NarrativeSavedData state, UUID playerUuid, long dayTime) {
        return switch (type) {
            case FLAG -> {
                boolean actual = switch (scope) {
                    case PLAYER -> state.hasPlayerFlag(playerUuid, key);
                    case WORLD -> state.hasWorldFlag(key);
                };
                yield actual == expected;
            }
            case VARIABLE_EQUALS -> {
                String actual = switch (scope) {
                    case PLAYER -> state.getPlayerVariable(playerUuid, key);
                    case WORLD -> state.getWorldVariable(key);
                };
                yield actual.equals(value.orElse("")) == expected;
            }
            case ATTRIBUTE_AT_LEAST -> {
                boolean actual = state.getAttribute(playerUuid, key) >= min.orElse(0);
                yield actual == expected;
            }
            case STORY_VAR -> {
                String actual = switch (scope) {
                    case PLAYER -> state.getPlayerStoryVariable(playerUuid, storyLayer, key);
                    case WORLD -> state.getWorldStoryVariable(storyLayer, key);
                };
                boolean matched = min
                        .map(threshold -> StoryVarValue.ofString(actual).asInt().stream().anyMatch(value -> value >= threshold))
                        .orElseGet(() -> StoryVarValue.ofString(actual).scalarEquals(StoryVarValue.ofString(value.orElse(""))));
                yield matched == expected;
            }
            case QUEST_STATE -> {
                boolean actual = state.getQuestState(playerUuid, normalizeIdentifier(key)).equals(value.orElse("take_rooted"));
                yield actual == expected;
            }
            case HAS_FEAT -> {
                boolean actual = state.hasFeat(playerUuid, normalizeIdentifier(key));
                yield actual == expected;
            }
            case FEAT_ACTIVE -> {
                boolean actual = state.isFeatActive(playerUuid, normalizeIdentifier(key));
                yield actual == expected;
            }
            case HAS_JOURNAL_ENTRY -> {
                boolean actual = state.hasJournalEntry(playerUuid, normalizeIdentifier(key));
                yield actual == expected;
            }
            case CLUE_FOUND -> {
                boolean actual = state.hasClue(playerUuid, normalizeIdentifier(key))
                        || state.hasJournalEntry(playerUuid, normalizeIdentifier(key));
                yield actual == expected;
            }
            case RELATION_AT_LEAST -> {
                boolean actual = state.getRelation(playerUuid, normalizeIdentifier(key)) >= min.orElse(0);
                yield actual == expected;
            }
            case NPC_STATE -> {
                boolean actual = switch (scope) {
                    case PLAYER -> state.hasPlayerNpcState(playerUuid, normalizeIdentifier(key), value.orElse(""));
                    case WORLD -> state.hasWorldNpcState(normalizeIdentifier(key), value.orElse(""));
                };
                yield actual == expected;
            }
            case TIME_WINDOW -> {
                long t = Math.floorMod(dayTime, 24000L);
                int start = min.orElse(0);
                int end = max.orElse(24000);
                boolean actual = dayTime >= 0 && (start <= end ? t >= start && t < end : t >= start || t < end);
                yield actual == expected;
            }
            case CONFLICT_STATE -> {
                boolean actual = state.getConflictState(playerUuid, normalizeIdentifier(key)).equals(value.orElse("active"));
                yield actual == expected;
            }
            case SCENE_PHASE -> {
                boolean actual = state.getScenePhase(playerUuid, normalizeIdentifier(key)).equals(value.orElse("default"));
                yield actual == expected;
            }
        };
    }

    public String debugSummary() {
        String prefix = scope.name().toLowerCase(Locale.ROOT) + " ";
        return switch (type) {
            case FLAG -> prefix + "flag " + key + " == " + expected;
            case VARIABLE_EQUALS -> prefix + "var " + key + " == " + value.orElse("") + " expected=" + expected;
            case ATTRIBUTE_AT_LEAST -> "attribute " + key + " >= " + min.orElse(0) + " expected=" + expected;
            case STORY_VAR -> prefix + "story " + storyLayer.serializedName() + " " + key
                    + min.map(integer -> " >= " + integer).orElse(" == " + value.orElse(""))
                    + " expected=" + expected;
            case QUEST_STATE -> "quest " + key + " == " + value.orElse("take_rooted") + " expected=" + expected;
            case HAS_FEAT -> "feat " + key + " unlocked == " + expected;
            case FEAT_ACTIVE -> "feat " + key + " active == " + expected;
            case HAS_JOURNAL_ENTRY -> "journal " + key + " unlocked == " + expected;
            case CLUE_FOUND -> "clue " + key + " found == " + expected;
            case RELATION_AT_LEAST -> "relation " + key + " >= " + min.orElse(0) + " expected=" + expected;
            case NPC_STATE -> prefix + "npc_state " + key + "#" + value.orElse("") + " == " + expected;
            case TIME_WINDOW -> "time_window [" + min.orElse(0) + "," + max.orElse(24000) + ") expected=" + expected;
            case CONFLICT_STATE -> "conflict " + key + " == " + value.orElse("active") + " expected=" + expected;
            case SCENE_PHASE -> "scene " + key + " == " + value.orElse("default") + " expected=" + expected;
        };
    }

    private static String normalizeKey(String rawType, String key) {
        if ("has_trait".equalsIgnoreCase(rawType) || "trait".equalsIgnoreCase(rawType)) {
            return key.startsWith("trait:") ? key : "trait:" + key;
        }
        if ("has_thought".equalsIgnoreCase(rawType) || "thought".equalsIgnoreCase(rawType)) {
            return key.startsWith("thought:") ? key : "thought:" + key;
        }
        return key;
    }

    private static String normalizeIdentifier(String key) {
        return key.contains(":") ? key : "ebb:" + key;
    }

    private static Optional<String> optionalString(JsonObject json, String key) {
        return json.has(key) && !json.get(key).isJsonNull()
                ? Optional.of(GsonHelper.getAsString(json, key))
                : Optional.empty();
    }

    private static Optional<String> optionalScalarString(JsonObject json, String key) {
        return json.has(key) && !json.get(key).isJsonNull() && json.get(key).isJsonPrimitive()
                ? Optional.of(StoryVarValue.fromJson(json.get(key)).raw())
                : Optional.empty();
    }

    private static Optional<Boolean> optionalBoolean(JsonObject json, String key) {
        return json.has(key) && !json.get(key).isJsonNull() && json.get(key).isJsonPrimitive() && json.get(key).getAsJsonPrimitive().isBoolean()
                ? Optional.of(GsonHelper.getAsBoolean(json, key))
                : Optional.empty();
    }

    private static Optional<Integer> optionalInt(JsonObject json, String key) {
        return json.has(key) && !json.get(key).isJsonNull()
                && json.get(key).isJsonPrimitive()
                && json.get(key).getAsJsonPrimitive().isNumber()
                ? Optional.of(GsonHelper.getAsInt(json, key))
                : Optional.empty();
    }
}
