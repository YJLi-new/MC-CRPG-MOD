package com.crpg.ebb.quest;

import com.crpg.ebb.feat.FeatDefinition;
import com.crpg.ebb.feat.FeatRegistry;
import com.crpg.ebb.state.NarrativeSavedData;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

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
        lines.add("Branch Map:");
        if (QuestBranchRegistry.definitions().isEmpty()) {
            lines.add("- none");
        }
        for (QuestBranchDefinition definition : QuestBranchRegistry.orderedDefinitions()) {
            String questState = state.getQuestState(player.getUUID(), definition.id().toString());
            String marker = definition.kind() == QuestBranchKind.MAJOR ? "◆ MAJOR" : "◇ MINOR";
            lines.add(marker + " " + definition.title() + " (" + definition.id() + ") state=" + questState);
            if (!definition.description().isBlank()) {
                lines.add("  ├─ " + definition.description());
            }
            if ("take_rooted".equals(questState) && !definition.takeRootText().isBlank()) {
                lines.add("  ★ TAKE ROOT: " + definition.takeRootText());
            }
            if (!definition.grantFeats().isEmpty()) {
                lines.add("  └─ grants: " + definition.grantFeats());
            }
        }
        lines.add("");
        lines.add("Feat Loadout:");
        lines.add("active_slots=" + state.activeFeatIds(player.getUUID()).size() + "/" + com.crpg.ebb.state.PlayerNarrativeState.MAX_ACTIVE_FEATS);
        if (FeatRegistry.definitions().isEmpty()) {
            lines.add("- none");
        }
        for (FeatDefinition feat : FeatRegistry.orderedDefinitions()) {
            boolean unlocked = state.hasFeat(player.getUUID(), feat.id().toString());
            boolean active = state.isFeatActive(player.getUUID(), feat.id().toString());
            String status = active ? "ACTIVE" : unlocked ? "UNLOCKED" : "LOCKED";
            String source = sourceQuests(feat.id());
            lines.add((active ? "▶ " : "- ") + status + " " + feat.displayName() + " (" + feat.id() + ")"
                    + " passive=" + feat.permanentPassive()
                    + " slot_candidate=" + feat.activeSlotCandidate()
                    + (source.isBlank() ? "" : " source=" + source));
            if (!feat.description().isBlank()) {
                lines.add("  " + feat.description());
            }
            if (!feat.checkModifiers().isEmpty()) {
                lines.add("  modifiers=" + feat.checkModifiers());
            }
        }
        return List.copyOf(lines);
    }

    private static String sourceQuests(Identifier featId) {
        return QuestBranchRegistry.orderedDefinitions().stream()
                .filter(definition -> definition.grantFeats().contains(featId))
                .map(definition -> definition.kind().serializedName() + ":" + definition.id())
                .collect(Collectors.joining(","));
    }
}
