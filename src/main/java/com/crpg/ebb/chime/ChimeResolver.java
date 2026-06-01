package com.crpg.ebb.chime;

import com.crpg.ebb.dialogue.DialogueDefinition;
import com.crpg.ebb.dialogue.DialogueNode;
import com.crpg.ebb.state.NarrativeSavedData;

import java.util.Optional;
import java.util.UUID;

public final class ChimeResolver {
    private ChimeResolver() {
    }

    public static Optional<String> resolve(DialogueDefinition dialogue, DialogueNode node, NarrativeSavedData state, UUID playerUuid) {
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
            String onceFlag = "chime_seen:" + dialogue.id() + ":" + node.id() + ":" + definition.id();
            if (state.hasPlayerFlag(playerUuid, onceFlag)) {
                continue;
            }
            state.setPlayerFlag(playerUuid, onceFlag, true);
            state.setPlayerFlag(playerUuid, "chime:" + definition.id(), true);
            String line = definition.lineForStableIndex((dialogue.id().toString() + node.id()).hashCode());
            return Optional.of("[Chime: " + definition.name() + "] " + line);
        }
        return Optional.empty();
    }
}
