package com.crpg.ebb.dialogue;

import com.crpg.ebb.state.NarrativeSavedData;
import com.crpg.ebb.story.StoryVarLayer;
import com.crpg.ebb.story.StoryVarValue;
import com.crpg.ebb.quest.TakeRootService;
import com.crpg.ebb.journal.JournalService;
import com.crpg.ebb.investigation.InvestigationService;
import com.crpg.ebb.conflict.ConflictService;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.minecraft.util.GsonHelper;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;

public record DialogueEffect(
        EffectType type,
        DialogueScope scope,
        StoryVarLayer storyLayer,
        String id,
        Optional<Integer> attributeValue,
        Optional<String> stringValue
) {
    public DialogueEffect {
        attributeValue = attributeValue == null ? Optional.empty() : attributeValue;
        stringValue = stringValue == null ? Optional.empty() : stringValue;
        storyLayer = storyLayer == null ? StoryVarLayer.MINOR : storyLayer;
    }

    public enum EffectType {
        SET_FLAG,
        CLEAR_FLAG,
        SET_ATTRIBUTE,
        SET_VARIABLE,
        CLEAR_VARIABLE,
        SET_STORY_VAR,
        CLEAR_STORY_VAR,
        ADD_STORY_INT,
        START_QUEST_BRANCH,
        COMPLETE_QUEST_BRANCH,
        UNLOCK_FEAT,
        ACTIVATE_FEAT,
        ADD_JOURNAL_ENTRY,
        SET_RELATION,
        ADD_RELATION,
        SET_NPC_STATE,
        CLEAR_NPC_STATE,
        SET_NPC_ROUTINE,
        REVEAL_CLUE,
        START_CONFLICT,
        ADD_CONFLICT_STRESS,
        ADD_CONFLICT_RESOLVE,
        SET_CONFLICT_STATE,
        APPLY_CONFLICT_OUTCOME,
        SET_SCENE_PHASE,
        ADD_TRAIT,
        REMOVE_TRAIT,
        ADD_THOUGHT,
        REMOVE_THOUGHT,
        UNLOCK_RETRY,
        GIVE_ITEM_PLACEHOLDER,
        TAKE_ITEM_PLACEHOLDER,
        ROUTINE_PLACEHOLDER;

        static Optional<EffectType> parse(String value) {
            if (value == null || value.isBlank()) {
                return Optional.empty();
            }
            String normalized = value.trim().toUpperCase(Locale.ROOT).replace('-', '_');
            if ("GIVE_ITEM".equals(normalized)) {
                normalized = "GIVE_ITEM_PLACEHOLDER";
            } else if ("TAKE_ITEM".equals(normalized)) {
                normalized = "TAKE_ITEM_PLACEHOLDER";
            } else if ("ROUTINE".equals(normalized) || "SET_ROUTINE".equals(normalized)
                    || "NPC_ROUTINE".equals(normalized) || "SET_NPC_ROUTINE".equals(normalized)) {
                normalized = "SET_NPC_ROUTINE";
            } else if ("SET_VAR".equals(normalized) || "VARIABLE".equals(normalized)) {
                normalized = "SET_VARIABLE";
            } else if ("CLEAR_VAR".equals(normalized)) {
                normalized = "CLEAR_VARIABLE";
            } else if ("STORY_VAR".equals(normalized) || "SET_STORY_VARIABLE".equals(normalized)) {
                normalized = "SET_STORY_VAR";
            } else if ("CLEAR_STORY_VARIABLE".equals(normalized) || "REMOVE_STORY_VAR".equals(normalized)
                    || "REMOVE_STORY_VARIABLE".equals(normalized)) {
                normalized = "CLEAR_STORY_VAR";
            } else if ("ADD_STORY_VAR".equals(normalized) || "INCREMENT_STORY_VAR".equals(normalized)
                    || "INCREMENT_STORY_VARIABLE".equals(normalized)) {
                normalized = "ADD_STORY_INT";
            } else if ("START_QUEST".equals(normalized) || "START_QUEST_BRANCH".equals(normalized)) {
                normalized = "START_QUEST_BRANCH";
            } else if ("COMPLETE_QUEST".equals(normalized) || "COMPLETE_BRANCH".equals(normalized)
                    || "TAKE_ROOT".equals(normalized)) {
                normalized = "COMPLETE_QUEST_BRANCH";
            } else if ("GRANT_FEAT".equals(normalized)) {
                normalized = "UNLOCK_FEAT";
            } else if ("EQUIP_FEAT".equals(normalized) || "SLOT_FEAT".equals(normalized)) {
                normalized = "ACTIVATE_FEAT";
            } else if ("JOURNAL".equals(normalized) || "ADD_JOURNAL".equals(normalized)
                    || "ADD_JOURNAL_ENTRY".equals(normalized)) {
                normalized = "ADD_JOURNAL_ENTRY";
            } else if ("CLUE".equals(normalized) || "REVEAL_CLUE".equals(normalized)
                    || "ADD_CLUE".equals(normalized) || "CLUE_FOUND".equals(normalized)) {
                normalized = "REVEAL_CLUE";
            } else if ("CONFLICT".equals(normalized) || "START_CONFLICT".equals(normalized)) {
                normalized = "START_CONFLICT";
            } else if ("ADD_STRESS".equals(normalized) || "CONFLICT_STRESS".equals(normalized)) {
                normalized = "ADD_CONFLICT_STRESS";
            } else if ("ADD_RESOLVE".equals(normalized) || "CONFLICT_RESOLVE".equals(normalized)) {
                normalized = "ADD_CONFLICT_RESOLVE";
            } else if ("CONFLICT_OUTCOME".equals(normalized) || "APPLY_OUTCOME".equals(normalized)
                    || "APPLY_CONFLICT_OUTCOME".equals(normalized) || "RESOLVE_CONFLICT".equals(normalized)) {
                normalized = "APPLY_CONFLICT_OUTCOME";
            } else if ("SCENE_PHASE".equals(normalized)) {
                normalized = "SET_SCENE_PHASE";
            } else if ("RELATION".equals(normalized) || "SET_RELATIONSHIP".equals(normalized)) {
                normalized = "SET_RELATION";
            } else if ("ADD_RELATIONSHIP".equals(normalized) || "MODIFY_RELATION".equals(normalized)
                    || "MODIFY_RELATIONSHIP".equals(normalized)) {
                normalized = "ADD_RELATION";
            } else if ("NPC_STATE".equals(normalized) || "ADD_NPC_STATE".equals(normalized)
                    || "SET_NPC_TAG".equals(normalized) || "ADD_NPC_TAG".equals(normalized)
                    || "SET_STATE_TAG".equals(normalized)) {
                normalized = "SET_NPC_STATE";
            } else if ("REMOVE_NPC_STATE".equals(normalized) || "CLEAR_NPC_TAG".equals(normalized)
                    || "REMOVE_NPC_TAG".equals(normalized) || "CLEAR_STATE_TAG".equals(normalized)) {
                normalized = "CLEAR_NPC_STATE";
            } else if ("UNLOCK".equals(normalized) || "UNLOCK_RETRYABLE".equals(normalized)) {
                normalized = "UNLOCK_RETRY";
            }
            try {
                return Optional.of(EffectType.valueOf(normalized));
            } catch (IllegalArgumentException ex) {
                return Optional.empty();
            }
        }
    }

    public static List<DialogueEffect> parseList(JsonObject json, String key, String path, List<String> messages) {
        if (!json.has(key)) {
            return List.of();
        }
        if (!json.get(key).isJsonArray()) {
            messages.add(path + ": " + key + " must be an array when present");
            return List.of();
        }
        List<DialogueEffect> effects = new ArrayList<>();
        JsonArray array = json.getAsJsonArray(key);
        for (int i = 0; i < array.size(); i++) {
            JsonElement element = array.get(i);
            if (!element.isJsonObject()) {
                messages.add(path + "." + key + "[" + i + "]: effect must be an object");
                continue;
            }
            parse(element.getAsJsonObject(), path + "." + key + "[" + i + "]", messages).ifPresent(effects::add);
        }
        return List.copyOf(effects);
    }

    public static Optional<DialogueEffect> parse(JsonObject json, String path, List<String> messages) {
        Optional<EffectType> type = EffectType.parse(optionalString(json, "type")
                .or(() -> inferTypeFromShortcut(json))
                .orElse("set_flag"));
        if (type.isEmpty()) {
            messages.add(path + ": unknown effect type");
            return Optional.empty();
        }
        Optional<DialogueScope> scope = DialogueScope.parse(optionalString(json, "scope").orElse("player"));
        if (scope.isEmpty()) {
            messages.add(path + ": scope must be player or world");
            return Optional.empty();
        }
        StoryVarLayer storyLayer = StoryVarLayer.parse(optionalString(json, "layer").orElse("minor"))
                .orElse(StoryVarLayer.MINOR);
        Optional<String> id = optionalString(json, "id")
                .or(() -> optionalString(json, "key"))
                .or(() -> optionalString(json, "flag"))
                .or(() -> optionalString(json, "story_var"))
                .or(() -> optionalString(json, "storyVar"))
                .or(() -> optionalString(json, "setStoryVar"))
                .or(() -> optionalString(json, "clearStoryVar"))
                .or(() -> optionalString(json, "addStoryInt"))
                .or(() -> optionalString(json, "quest"))
                .or(() -> optionalString(json, "quest_branch"))
                .or(() -> optionalString(json, "questBranch"))
                .or(() -> optionalString(json, "feat"))
                .or(() -> optionalString(json, "journal"))
                .or(() -> optionalString(json, "journal_entry"))
                .or(() -> optionalString(json, "journalEntry"))
                .or(() -> optionalString(json, "clue"))
                .or(() -> optionalString(json, "conflict"))
                .or(() -> optionalString(json, "scene"))
                .or(() -> optionalString(json, "relation"))
                .or(() -> optionalString(json, "relationship"))
                .or(() -> optionalString(json, "npc"))
                .or(() -> optionalString(json, "npc_id"))
                .or(() -> optionalString(json, "npcId"))
                .or(() -> optionalString(json, "setFlag"))
                .or(() -> optionalString(json, "clearFlag"))
                .or(() -> optionalString(json, "attribute"))
                .or(() -> optionalString(json, "routine"))
                .or(() -> optionalString(json, "trait"))
                .or(() -> optionalString(json, "addTrait"))
                .or(() -> optionalString(json, "removeTrait"))
                .or(() -> optionalString(json, "thought"))
                .or(() -> optionalString(json, "addThought"))
                .or(() -> optionalString(json, "removeThought"))
                .or(() -> optionalString(json, "var"))
                .or(() -> optionalString(json, "setVar"))
                .or(() -> optionalString(json, "unlock"));
        if (id.isEmpty() || id.get().isBlank()) {
            messages.add(path + ": missing id/key/flag/attribute/routine/trait/thought/var string");
            return Optional.empty();
        }
        Optional<Integer> value = optionalInt(json, "value");
        Optional<Integer> amount = optionalInt(json, "amount").or(() -> optionalInt(json, "delta")).or(() -> value);
        if (type.get() == EffectType.SET_ATTRIBUTE && value.isEmpty()) {
            messages.add(path + ": set_attribute requires integer value");
            return Optional.empty();
        }
        Optional<String> stringValue = optionalString(json, "string_value")
                .or(() -> optionalString(json, "text"))
                .or(() -> optionalString(json, "equals"))
                .or(() -> optionalString(json, "tag"))
                .or(() -> optionalString(json, "state"))
                .or(() -> optionalString(json, "phase"))
                .or(() -> optionalString(json, "outcome"))
                .or(() -> optionalString(json, "outcome_id"))
                .or(() -> optionalString(json, "outcomeId"))
                .or(() -> optionalString(json, "state_tag"))
                .or(() -> optionalString(json, "stateTag"))
                .or(() -> type.get() == EffectType.SET_VARIABLE || type.get() == EffectType.SET_STORY_VAR
                        || type.get() == EffectType.APPLY_CONFLICT_OUTCOME
                        ? optionalScalarString(json, "value") : Optional.empty());
        if (type.get() == EffectType.SET_VARIABLE && stringValue.isEmpty()) {
            messages.add(path + ": set_variable requires string_value/text/value");
            return Optional.empty();
        }
        if (type.get() == EffectType.SET_STORY_VAR && stringValue.isEmpty()) {
            messages.add(path + ": set_story_var requires string_value/text/value");
            return Optional.empty();
        }
        if (type.get() == EffectType.ADD_STORY_INT && amount.isEmpty()) {
            messages.add(path + ": add_story_int requires integer amount/delta/value");
            return Optional.empty();
        }
        if ((type.get() == EffectType.SET_RELATION || type.get() == EffectType.ADD_RELATION) && amount.isEmpty()) {
            messages.add(path + ": relation effects require integer value/amount/delta");
            return Optional.empty();
        }
        if ((type.get() == EffectType.SET_NPC_STATE || type.get() == EffectType.CLEAR_NPC_STATE) && stringValue.isEmpty()) {
            messages.add(path + ": npc state effects require tag/state/state_tag");
            return Optional.empty();
        }
        if ((type.get() == EffectType.ADD_CONFLICT_STRESS || type.get() == EffectType.ADD_CONFLICT_RESOLVE) && amount.isEmpty()) {
            messages.add(path + ": conflict score effects require integer amount/delta/value");
            return Optional.empty();
        }
        if ((type.get() == EffectType.SET_CONFLICT_STATE || type.get() == EffectType.SET_SCENE_PHASE) && stringValue.isEmpty()) {
            messages.add(path + ": set_conflict_state/set_scene_phase require state/phase/value");
            return Optional.empty();
        }
        if (type.get() == EffectType.APPLY_CONFLICT_OUTCOME && stringValue.isEmpty()) {
            messages.add(path + ": apply_conflict_outcome requires outcome/outcome_id/value");
            return Optional.empty();
        }
        Optional<Integer> integerValue = type.get() == EffectType.ADD_STORY_INT
                || type.get() == EffectType.ADD_RELATION
                || type.get() == EffectType.SET_RELATION
                || type.get() == EffectType.ADD_CONFLICT_STRESS
                || type.get() == EffectType.ADD_CONFLICT_RESOLVE ? amount : value;
        return Optional.of(new DialogueEffect(type.get(), scope.get(), storyLayer, normalizeId(type.get(), id.get()), integerValue, stringValue));
    }

    public Optional<String> apply(NarrativeSavedData state, UUID playerUuid) {
        switch (type) {
            case SET_FLAG -> setFlag(state, playerUuid, true);
            case CLEAR_FLAG -> setFlag(state, playerUuid, false);
            case SET_ATTRIBUTE -> {
                if (scope == DialogueScope.WORLD) {
                    return Optional.of("world_attribute_effect_ignored:" + id);
                }
                state.setAttribute(playerUuid, id, attributeValue.orElse(0));
            }
            case SET_VARIABLE -> setVariable(state, playerUuid, stringValue.orElse(""));
            case CLEAR_VARIABLE -> setVariable(state, playerUuid, "");
            case SET_STORY_VAR -> {
                setStoryVariable(state, playerUuid, stringValue.orElse(""));
                return Optional.of("story_var_set:" + scopeName() + "." + storyLayer.serializedName() + "." + id + "=" + stringValue.orElse(""));
            }
            case CLEAR_STORY_VAR -> {
                setStoryVariable(state, playerUuid, "");
                return Optional.of("story_var_clear:" + scopeName() + "." + storyLayer.serializedName() + "." + id);
            }
            case ADD_STORY_INT -> {
                int next = addStoryInt(state, playerUuid, attributeValue.orElse(0));
                return Optional.of("story_var_add:" + scopeName() + "." + storyLayer.serializedName() + "." + id + "=" + next);
            }
            case START_QUEST_BRANCH -> {
                return TakeRootService.startBranch(state, playerUuid, id);
            }
            case COMPLETE_QUEST_BRANCH -> {
                return TakeRootService.completeBranch(state, playerUuid, id);
            }
            case UNLOCK_FEAT -> {
                return TakeRootService.unlockFeat(state, playerUuid, id);
            }
            case ACTIVATE_FEAT -> {
                return TakeRootService.activateFeat(state, playerUuid, id);
            }
            case ADD_JOURNAL_ENTRY -> {
                return JournalService.addEntry(state, playerUuid, id);
            }
            case REVEAL_CLUE -> {
                return InvestigationService.revealClue(state, playerUuid, id);
            }
            case SET_RELATION -> {
                if (scope == DialogueScope.WORLD) {
                    return Optional.of("world_relation_effect_ignored:" + id);
                }
                state.setRelation(playerUuid, id, attributeValue.orElse(0));
                return Optional.of("relation_changed:" + id + "=" + state.getRelation(playerUuid, id));
            }
            case ADD_RELATION -> {
                if (scope == DialogueScope.WORLD) {
                    return Optional.of("world_relation_effect_ignored:" + id);
                }
                int next = state.addRelation(playerUuid, id, attributeValue.orElse(0));
                return Optional.of("relation_changed:" + id + "=" + next);
            }
            case SET_NPC_STATE -> {
                setNpcState(state, playerUuid, true);
                return Optional.of("npc_state_set:" + scopeName() + "." + id + "#" + stringValue.orElse(""));
            }
            case CLEAR_NPC_STATE -> {
                setNpcState(state, playerUuid, false);
                return Optional.of("npc_state_clear:" + scopeName() + "." + id + "#" + stringValue.orElse(""));
            }
            case SET_NPC_ROUTINE -> {
                return Optional.empty();
            }
            case START_CONFLICT -> {
                return ConflictService.start(state, playerUuid, id);
            }
            case ADD_CONFLICT_STRESS -> {
                return ConflictService.addStress(state, playerUuid, id, attributeValue.orElse(0));
            }
            case ADD_CONFLICT_RESOLVE -> {
                return ConflictService.addResolve(state, playerUuid, id, attributeValue.orElse(0));
            }
            case SET_CONFLICT_STATE -> {
                state.setConflictState(playerUuid, id, stringValue.orElse(""));
                return Optional.of("conflict_state:" + id + "=" + state.getConflictState(playerUuid, id));
            }
            case APPLY_CONFLICT_OUTCOME -> {
                return ConflictService.applyOutcome(state, playerUuid, id, stringValue.orElse(""));
            }
            case SET_SCENE_PHASE -> {
                state.setScenePhase(playerUuid, id, stringValue.orElse(""));
                return Optional.of("scene_phase:" + id + "=" + state.getScenePhase(playerUuid, id));
            }
            case ADD_TRAIT, ADD_THOUGHT, UNLOCK_RETRY -> setFlag(state, playerUuid, true);
            case REMOVE_TRAIT, REMOVE_THOUGHT -> setFlag(state, playerUuid, false);
            case GIVE_ITEM_PLACEHOLDER -> {
                state.setPlayerFlag(playerUuid, "item:" + id, true);
                return Optional.of("item_placeholder_give:" + id);
            }
            case TAKE_ITEM_PLACEHOLDER -> {
                state.setPlayerFlag(playerUuid, "item:" + id, false);
                return Optional.of("item_placeholder_take:" + id);
            }
            case ROUTINE_PLACEHOLDER -> {
                setFlag(state, playerUuid, true);
                return Optional.of("routine_placeholder:" + id);
            }
        }
        return Optional.empty();
    }

    public String debugSummary() {
        String value = attributeValue.map(v -> "=" + v).orElse("");
        String text = stringValue.map(v -> "=\"" + v + "\"").orElse("");
        String layer = switch (type) {
            case SET_STORY_VAR, CLEAR_STORY_VAR, ADD_STORY_INT -> "," + storyLayer.serializedName();
            default -> "";
        };
        String tag = switch (type) {
            case SET_NPC_STATE, CLEAR_NPC_STATE -> ",tag=" + stringValue.orElse("");
            default -> "";
        };
        return type.name().toLowerCase(Locale.ROOT) + "(" + scope.name().toLowerCase(Locale.ROOT) + layer + "," + id + value + text + tag + ")";
    }

    private void setFlag(NarrativeSavedData state, UUID playerUuid, boolean value) {
        switch (scope) {
            case PLAYER -> state.setPlayerFlag(playerUuid, id, value);
            case WORLD -> state.setWorldFlag(id, value);
        }
    }

    private void setVariable(NarrativeSavedData state, UUID playerUuid, String value) {
        switch (scope) {
            case PLAYER -> state.setPlayerVariable(playerUuid, id, value);
            case WORLD -> state.setWorldVariable(id, value);
        }
    }

    private void setStoryVariable(NarrativeSavedData state, UUID playerUuid, String value) {
        StoryVarValue normalizedValue = StoryVarValue.ofString(value);
        switch (scope) {
            case PLAYER -> state.setPlayerStoryVariable(playerUuid, storyLayer, id, normalizedValue.raw());
            case WORLD -> state.setWorldStoryVariable(storyLayer, id, normalizedValue.raw());
        }
    }

    private int addStoryInt(NarrativeSavedData state, UUID playerUuid, int amount) {
        return switch (scope) {
            case PLAYER -> state.addPlayerStoryInt(playerUuid, storyLayer, id, amount);
            case WORLD -> state.addWorldStoryInt(storyLayer, id, amount);
        };
    }

    private void setNpcState(NarrativeSavedData state, UUID playerUuid, boolean value) {
        switch (scope) {
            case PLAYER -> state.setPlayerNpcState(playerUuid, id, stringValue.orElse(""), value);
            case WORLD -> state.setWorldNpcState(id, stringValue.orElse(""), value);
        }
    }

    private String scopeName() {
        return scope.name().toLowerCase(Locale.ROOT);
    }

    private static Optional<String> inferTypeFromShortcut(JsonObject json) {
        if (json.has("setFlag")) return Optional.of("set_flag");
        if (json.has("clearFlag")) return Optional.of("clear_flag");
        if (json.has("setVar")) return Optional.of("set_variable");
        if (json.has("setStoryVar")) return Optional.of("set_story_var");
        if (json.has("clearStoryVar")) return Optional.of("clear_story_var");
        if (json.has("addStoryInt")) return Optional.of("add_story_int");
        if (json.has("quest") || json.has("questBranch")) return Optional.of("complete_quest_branch");
        if (json.has("feat")) return Optional.of("unlock_feat");
        if (json.has("journal") || json.has("journalEntry")) return Optional.of("add_journal_entry");
        if (json.has("clue")) return Optional.of("reveal_clue");
        if (json.has("relation") || json.has("relationship")) return Optional.of("add_relation");
        if (json.has("conflict") && (json.has("outcome") || json.has("outcome_id") || json.has("outcomeId"))) return Optional.of("apply_conflict_outcome");
        if (json.has("conflict")) return Optional.of("start_conflict");
        if (json.has("scene") && (json.has("phase") || json.has("state"))) return Optional.of("set_scene_phase");
        if (json.has("npc") && (json.has("tag") || json.has("state") || json.has("state_tag") || json.has("stateTag"))) return Optional.of("set_npc_state");
        if (json.has("routine")) return Optional.of("set_npc_routine");
        if (json.has("addTrait")) return Optional.of("add_trait");
        if (json.has("removeTrait")) return Optional.of("remove_trait");
        if (json.has("addThought")) return Optional.of("add_thought");
        if (json.has("removeThought")) return Optional.of("remove_thought");
        if (json.has("unlock")) return Optional.of("unlock_retry");
        return Optional.empty();
    }

    private static String normalizeId(EffectType type, String id) {
        return switch (type) {
            case ADD_TRAIT, REMOVE_TRAIT -> id.startsWith("trait:") ? id : "trait:" + id;
            case ADD_THOUGHT, REMOVE_THOUGHT -> id.startsWith("thought:") ? id : "thought:" + id;
            case UNLOCK_RETRY -> id.startsWith("unlock:") ? id : "unlock:" + id;
            default -> id;
        };
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

    private static Optional<Integer> optionalInt(JsonObject json, String key) {
        return json.has(key) && !json.get(key).isJsonNull()
                && json.get(key).isJsonPrimitive()
                && json.get(key).getAsJsonPrimitive().isNumber()
                ? Optional.of(GsonHelper.getAsInt(json, key))
                : Optional.empty();
    }
}
