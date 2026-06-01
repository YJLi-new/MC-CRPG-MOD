package com.crpg.ebb.quest;

import com.crpg.ebb.feat.FeatDefinition;
import com.crpg.ebb.feat.FeatRegistry;
import com.crpg.ebb.state.NarrativeSavedData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

import java.util.ArrayList;
import java.util.List;

public final class QuestTreeService {
    private QuestTreeService() {
    }

    public static List<String> build(ServerPlayer player) {
        NarrativeSavedData state = NarrativeSavedData.get((ServerLevel) player.level());
        List<String> lines = new ArrayList<>();
        lines.add("Esoteric Ebb Quest Tree");
        lines.add("player=" + player.getName().getString() + " uuid=" + player.getUUID());
        lines.add(QuestBranchRegistry.summaryLine());
        lines.add(FeatRegistry.summaryLine());
        lines.add("active_feat_slots=" + state.activeFeatIds(player.getUUID()).size() + "/" + com.crpg.ebb.state.PlayerNarrativeState.MAX_ACTIVE_FEATS);
        lines.add("");
        lines.add("Quest branches:");
        if (QuestBranchRegistry.definitions().isEmpty()) {
            lines.add("- none");
        }
        for (QuestBranchDefinition definition : QuestBranchRegistry.orderedDefinitions()) {
            String questState = state.getQuestState(player.getUUID(), definition.id().toString());
            lines.add("- " + definition.id() + " [" + definition.kind().serializedName() + "] " + definition.title() + " state=" + questState);
            if (!definition.description().isBlank()) {
                lines.add("  " + definition.description());
            }
            if ("take_rooted".equals(questState) && !definition.takeRootText().isBlank()) {
                lines.add("  take-root: " + definition.takeRootText());
            }
            if (!definition.grantFeats().isEmpty()) {
                lines.add("  grants: " + definition.grantFeats());
            }
        }
        lines.add("");
        lines.add("Feats:");
        if (FeatRegistry.definitions().isEmpty()) {
            lines.add("- none");
        }
        for (FeatDefinition feat : FeatRegistry.orderedDefinitions()) {
            boolean unlocked = state.hasFeat(player.getUUID(), feat.id().toString());
            boolean active = state.isFeatActive(player.getUUID(), feat.id().toString());
            lines.add("- " + feat.id() + " " + feat.displayName() + " unlocked=" + unlocked + " active=" + active);
            if (!feat.description().isBlank()) {
                lines.add("  " + feat.description());
            }
            if (!feat.checkModifiers().isEmpty()) {
                lines.add("  check_modifiers=" + feat.checkModifiers());
            }
        }
        return List.copyOf(lines);
    }
}
