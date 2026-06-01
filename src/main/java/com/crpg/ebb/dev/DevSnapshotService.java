package com.crpg.ebb.dev;

import com.crpg.ebb.attribute.AttributeRegistry;
import com.crpg.ebb.chime.ChimeRegistry;
import com.crpg.ebb.data.JsonDataRegistry;
import com.crpg.ebb.data.NarrativeDataRegistries;
import com.crpg.ebb.dialogue.DialogueRegistry;
import com.crpg.ebb.dialogue.DialogueService;
import com.crpg.ebb.feat.FeatRegistry;
import com.crpg.ebb.conflict.ConflictRegistry;
import com.crpg.ebb.interaction.BlockGroupIndex;
import com.crpg.ebb.interaction.InteractionSettings;
import com.crpg.ebb.interaction.entity.EntityBindingRegistry;
import com.crpg.ebb.investigation.InvestigationRegistry;
import com.crpg.ebb.journal.JournalEntryRegistry;
import com.crpg.ebb.quest.QuestBranchRegistry;
import com.crpg.ebb.relationship.RelationshipRegistry;
import com.crpg.ebb.routine.NpcRoutineRegistry;
import com.crpg.ebb.state.NarrativeSavedData;
import net.minecraft.server.MinecraftServer;

import java.util.ArrayList;
import java.util.List;

public final class DevSnapshotService {
    private DevSnapshotService() {
    }

    public static List<String> build(MinecraftServer server) {
        List<String> lines = new ArrayList<>();
        lines.add("Esoteric Ebb CRPG developer tree browser");
        lines.add(NarrativeDataRegistries.summaryLine());
        lines.add("dialogue_sessions(active=" + DialogueService.activeSessionCount() + ")");
        NarrativeSavedData narrativeState = NarrativeSavedData.get(server);
        lines.add(narrativeState.summaryLine());
        lines.add("players_online=" + server.getPlayerList().getPlayerCount());
        lines.add("");
        lines.add("Raw registries:");
        for (JsonDataRegistry registry : NarrativeDataRegistries.all()) {
            lines.add("- " + registry.directory() + ": raw=" + registry.size()
                    + ", raw_messages=" + registry.validationMessages().size());
        }
        lines.add("");
        lines.add("Typed registries:");
        lines.add("- " + DialogueRegistry.summaryLine());
        lines.add("- " + AttributeRegistry.summaryLine());
        lines.add("- " + InteractionSettings.summaryLine());
        lines.add("- " + BlockGroupIndex.summaryLine());
        lines.add("- " + EntityBindingRegistry.summaryLine());
        lines.add("- " + NpcRoutineRegistry.summaryLine());
        lines.add("- " + QuestBranchRegistry.summaryLine());
        lines.add("- " + FeatRegistry.summaryLine());
        lines.add("- " + ChimeRegistry.summaryLine());
        lines.add("- " + JournalEntryRegistry.summaryLine());
        lines.add("- " + RelationshipRegistry.summaryLine());
        lines.add("- " + InvestigationRegistry.summaryLine());
        lines.add("- " + ConflictRegistry.summaryLine());
        lines.add("");
        lines.addAll(narrativeState.storyVariableDebugLines(96));
        lines.add("");
        lines.addAll(narrativeState.questFeatDebugLines(96));
        lines.add("");
        lines.addAll(narrativeState.relationshipDebugLines(96));
        lines.add("");
        lines.addAll(narrativeState.investigationDebugLines(96));
        DialogueDebugDumper.appendDialogueTrees(lines);
        DialogueDebugDumper.appendEntityBindings(lines);
        DialogueDebugDumper.appendRoutines(lines);
        appendMessages(lines, "Raw JSON messages", NarrativeDataRegistries.all().stream()
                .flatMap(registry -> registry.validationMessages().stream()
                        .map(message -> registry.directory() + ": " + message))
                .toList());
        appendMessages(lines, "Dialogue validation", DialogueRegistry.validationMessages());
        appendMessages(lines, "Attribute validation", AttributeRegistry.validationMessages());
        appendMessages(lines, "Interaction settings validation", InteractionSettings.validationMessages());
        appendMessages(lines, "Block group validation", BlockGroupIndex.messages());
        appendMessages(lines, "Entity binding validation", EntityBindingRegistry.validationMessages());
        appendMessages(lines, "NPC routine validation", NpcRoutineRegistry.validationMessages());
        appendMessages(lines, "Quest branch validation", QuestBranchRegistry.validationMessages());
        appendMessages(lines, "Feat validation", FeatRegistry.validationMessages());
        appendMessages(lines, "Chime validation", ChimeRegistry.validationMessages());
        appendMessages(lines, "Journal entry validation", JournalEntryRegistry.validationMessages());
        appendMessages(lines, "Relationship validation", RelationshipRegistry.validationMessages());
        appendMessages(lines, "Investigation validation", InvestigationRegistry.validationMessages());
        appendMessages(lines, "Conflict validation", ConflictRegistry.validationMessages());
        return lines;
    }

    private static void appendMessages(List<String> lines, String heading, List<String> messages) {
        lines.add("");
        lines.add(heading + " (" + messages.size() + "):");
        if (messages.isEmpty()) {
            lines.add("- none");
            return;
        }
        messages.stream().limit(96).forEach(message -> lines.add("- " + message));
        if (messages.size() > 96) {
            lines.add("- ... " + (messages.size() - 96) + " more");
        }
    }
}
