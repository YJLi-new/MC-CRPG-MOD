package com.crpg.ebb.chime;

import com.crpg.ebb.dialogue.DialogueDefinition;
import com.crpg.ebb.dialogue.DialogueNode;
import com.crpg.ebb.state.NarrativeSavedData;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public final class ChimeResolver {
    private ChimeResolver() {
    }

    public static Optional<String> resolve(DialogueDefinition dialogue, DialogueNode node, NarrativeSavedData state, UUID playerUuid) {
        return resolve(dialogue, node, state, playerUuid, -1L);
    }

    public static Optional<String> resolve(DialogueDefinition dialogue, DialogueNode node, NarrativeSavedData state, UUID playerUuid, long dayTime) {
        if (node.chimeTags().isEmpty()) {
            return Optional.empty();
        }
        for (ChimeDefinition definition : ChimeRegistry.orderedDefinitions()) {
            if (definition.triggerTags().stream().noneMatch(node.chimeTags()::contains)) {
                continue;
            }
            int score = state.getAttribute(playerUuid, definition.sourceAttribute());
            if (score < definition.minScore()) {
                continue;
            }
            String globalOnceFlag = "chime_seen_global:" + definition.id();
            if (definition.oneShotGlobal() && state.hasPlayerFlag(playerUuid, globalOnceFlag)) {
                continue;
            }
            String nodeOnceFlag = "chime_seen:" + dialogue.id() + ":" + node.id() + ":" + definition.id();
            if (definition.oneShotPerNode() && state.hasPlayerFlag(playerUuid, nodeOnceFlag)) {
                continue;
            }
            String cooldownKey = "chime_last_tick:" + definition.id();
            if (cooldownRemainingTicks(definition, state, playerUuid, cooldownKey, dayTime) > 0L) {
                continue;
            }
            if (definition.oneShotPerNode()) {
                state.setPlayerFlag(playerUuid, nodeOnceFlag, true);
            }
            if (definition.oneShotGlobal()) {
                state.setPlayerFlag(playerUuid, globalOnceFlag, true);
            }
            if (definition.cooldownTicks() > 0 && dayTime >= 0L) {
                state.setPlayerVariable(playerUuid, cooldownKey, Long.toString(dayTime));
            }
            state.setPlayerFlag(playerUuid, "chime:" + definition.id(), true);
            String line = definition.lineForStableIndex((dialogue.id().toString() + node.id()).hashCode());
            return Optional.of("[Chime: " + definition.name() + "] " + line);
        }
        return Optional.empty();
    }

    public static List<String> explain(DialogueDefinition dialogue, DialogueNode node, NarrativeSavedData state, UUID playerUuid, long dayTime) {
        List<String> lines = new ArrayList<>();
        if (node.chimeTags().isEmpty()) {
            lines.add("node has no chime_tags; no passive insert can trigger");
            return lines;
        }
        for (ChimeDefinition definition : ChimeRegistry.orderedDefinitions()) {
            boolean tagMatch = definition.triggerTags().stream().anyMatch(node.chimeTags()::contains);
            int score = state.getAttribute(playerUuid, definition.sourceAttribute());
            String globalOnceFlag = "chime_seen_global:" + definition.id();
            String nodeOnceFlag = "chime_seen:" + dialogue.id() + ":" + node.id() + ":" + definition.id();
            String cooldownKey = "chime_last_tick:" + definition.id();
            long cooldownRemaining = cooldownRemainingTicks(definition, state, playerUuid, cooldownKey, dayTime);
            String reason;
            if (!tagMatch) {
                reason = "skip:no_matching_trigger_tag";
            } else if (score < definition.minScore()) {
                reason = "skip:score_" + score + "_below_min_" + definition.minScore();
            } else if (definition.oneShotGlobal() && state.hasPlayerFlag(playerUuid, globalOnceFlag)) {
                reason = "skip:one_shot_global_seen";
            } else if (definition.oneShotPerNode() && state.hasPlayerFlag(playerUuid, nodeOnceFlag)) {
                reason = "skip:one_shot_node_seen";
            } else if (cooldownRemaining > 0L) {
                reason = "skip:cooldown_remaining_" + cooldownRemaining;
            } else {
                reason = "ready";
            }
            lines.add(definition.id() + " [" + definition.name() + "] " + reason
                    + " attr=" + definition.sourceAttribute() + ":" + score + "/" + definition.minScore()
                    + " node_tags=" + node.chimeTags()
                    + " trigger_tags=" + definition.triggerTags()
                    + " cooldown=" + definition.cooldownTicks()
                    + " one_shot_per_node=" + definition.oneShotPerNode()
                    + " active_thoughts=" + definition.activeThoughtIds());
        }
        return List.copyOf(lines);
    }

    private static long cooldownRemainingTicks(
            ChimeDefinition definition,
            NarrativeSavedData state,
            UUID playerUuid,
            String cooldownKey,
            long dayTime
    ) {
        if (definition.cooldownTicks() <= 0 || dayTime < 0L) {
            return 0L;
        }
        String rawLastTick = state.getPlayerVariable(playerUuid, cooldownKey);
        if (rawLastTick.isBlank()) {
            return 0L;
        }
        try {
            long lastTick = Long.parseLong(rawLastTick);
            if (dayTime < lastTick) {
                return 0L;
            }
            long elapsed = dayTime - lastTick;
            return elapsed < definition.cooldownTicks() ? definition.cooldownTicks() - elapsed : 0L;
        } catch (NumberFormatException ignored) {
            return 0L;
        }
    }
}
