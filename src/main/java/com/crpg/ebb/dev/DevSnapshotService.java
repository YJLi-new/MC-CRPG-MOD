package com.crpg.ebb.dev;

import com.crpg.ebb.attribute.AttributeRegistry;
import com.crpg.ebb.chime.ChimeRegistry;
import com.crpg.ebb.chime.ChimeResolver;
import com.crpg.ebb.data.JsonDataRegistry;
import com.crpg.ebb.data.NarrativeDataRegistries;
import com.crpg.ebb.dialogue.DialogueDefinition;
import com.crpg.ebb.dialogue.DialogueNode;
import com.crpg.ebb.dialogue.DialogueRegistry;
import com.crpg.ebb.dialogue.DialogueSession;
import com.crpg.ebb.dialogue.DialogueService;
import com.crpg.ebb.conflict.ConflictDefinition;
import com.crpg.ebb.feat.FeatRegistry;
import com.crpg.ebb.conflict.ConflictRegistry;
import com.crpg.ebb.conflict.ConflictService;
import com.crpg.ebb.interaction.BlockGroupIndex;
import com.crpg.ebb.interaction.InteractionSettings;
import com.crpg.ebb.interaction.entity.EntityBindingRegistry;
import com.crpg.ebb.investigation.InvestigationRegistry;
import com.crpg.ebb.journal.JournalEntryRegistry;
import com.crpg.ebb.network.sync.InteractionSyncService;
import com.crpg.ebb.quest.QuestBranchRegistry;
import com.crpg.ebb.relationship.RelationshipRegistry;
import com.crpg.ebb.routine.NpcRoutineRegistry;
import com.crpg.ebb.npc.profile.NpcProfileRegistry;
import com.crpg.ebb.state.NarrativeSavedData;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

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
        lines.add("- " + NpcProfileRegistry.summaryLine());
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
        lines.addAll(NpcProfileRegistry.debugLines(96));
        lines.add("");
        lines.addAll(narrativeState.promotedNpcProfileDebugLines(96));
        lines.add("");
        lines.addAll(narrativeState.investigationDebugLines(96));
        appendConflictCatalog(lines, server, narrativeState);
        appendSecurityDiagnostics(lines);
        appendChimeTriggerDebug(lines, server, narrativeState);
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
        appendMessages(lines, "NPC profile validation", NpcProfileRegistry.validationMessages());
        appendMessages(lines, "Quest branch validation", QuestBranchRegistry.validationMessages());
        appendMessages(lines, "Feat validation", FeatRegistry.validationMessages());
        appendMessages(lines, "Chime validation", ChimeRegistry.validationMessages());
        appendMessages(lines, "Journal entry validation", JournalEntryRegistry.validationMessages());
        appendMessages(lines, "Relationship validation", RelationshipRegistry.validationMessages());
        appendMessages(lines, "Investigation validation", InvestigationRegistry.validationMessages());
        appendMessages(lines, "Conflict validation", ConflictRegistry.validationMessages());
        return lines;
    }

    private static void appendSecurityDiagnostics(List<String> lines) {
        lines.add("");
        lines.add("P29 multiplayer/packet diagnostics:");
        lines.add("- dialogue_security_events=" + DialogueService.securityEventSnapshot());
        lines.add("- missing_client_mod_diagnostics:");
        InteractionSyncService.missingClientModDiagnosticLines().forEach(line -> lines.add("  " + line));
    }

    private static void appendConflictCatalog(List<String> lines, MinecraftServer server, NarrativeSavedData narrativeState) {
        lines.add("");
        lines.add("Conflict phase/status catalog:");
        List<ConflictDefinition> definitions = ConflictRegistry.orderedDefinitions();
        if (definitions.isEmpty()) {
            lines.add("- none");
            return;
        }
        List<ServerPlayer> players = server.getPlayerList().getPlayers();
        for (ConflictDefinition definition : definitions) {
            lines.add("- " + definition.debugSummary());
            lines.add("  phases=" + definition.phases());
            if (!definition.phaseDescriptions().isEmpty()) {
                definition.phaseDescriptions().forEach((phase, description) -> lines.add("  phase." + phase + "=" + description));
            }
            if (!definition.leverageClues().isEmpty()) {
                lines.add("  leverage_clues=" + definition.leverageClues());
            }
            if (!definition.outcomes().isEmpty()) {
                lines.add("  outcomes=" + definition.outcomes().stream()
                        .map(outcome -> outcome.debugSummary())
                        .toList());
            }
            if (players.isEmpty()) {
                lines.add("  status=no_online_players");
            } else {
                for (ServerPlayer player : players) {
                    lines.add("  status." + player.getName().getString() + "="
                            + ConflictService.statusLine(narrativeState, player.getUUID(), definition.id()));
                }
            }
        }
    }

    private static void appendChimeTriggerDebug(List<String> lines, MinecraftServer server, NarrativeSavedData narrativeState) {
        lines.add("");
        lines.add("Chime trigger debug:");
        List<ServerPlayer> players = server.getPlayerList().getPlayers();
        if (players.isEmpty()) {
            lines.add("- no online players");
            return;
        }
        for (ServerPlayer player : players) {
            String playerName = player.getName().getString();
            Optional<DialogueSession> maybeSession = DialogueService.currentSessionForPlayer(player.getUUID());
            if (maybeSession.isEmpty()) {
                lines.add("- " + playerName + ": no active dialogue; Chime reasons require a current dialogue node");
                continue;
            }
            DialogueSession session = maybeSession.get();
            Optional<DialogueDefinition> maybeDefinition = DialogueRegistry.byId(session.dialogueId());
            if (maybeDefinition.isEmpty()) {
                lines.add("- " + playerName + ": active dialogue " + session.dialogueId() + " is no longer loaded");
                continue;
            }
            DialogueDefinition definition = maybeDefinition.get();
            Optional<DialogueNode> maybeNode = definition.node(session.nodeId());
            if (maybeNode.isEmpty()) {
                lines.add("- " + playerName + ": active node " + session.nodeId() + " is missing in " + session.dialogueId());
                continue;
            }
            DialogueNode node = maybeNode.get();
            lines.add("- " + playerName + ": dialogue=" + session.dialogueId() + " node=" + node.id()
                    + " chime_tags=" + node.chimeTags());
            ChimeResolver.explain(definition, node, narrativeState, player.getUUID(), player.level().getOverworldClockTime())
                    .forEach(reason -> lines.add("  - " + reason));
        }
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
