package com.crpg.ebb.dev;

import com.crpg.ebb.attribute.AttributeRegistry;
import com.crpg.ebb.data.JsonDataRegistry;
import com.crpg.ebb.data.NarrativeDataRegistries;
import com.crpg.ebb.dialogue.DialogueRegistry;
import com.crpg.ebb.dialogue.DialogueService;
import com.crpg.ebb.interaction.BlockGroupIndex;
import com.crpg.ebb.state.NarrativeSavedData;
import net.minecraft.server.MinecraftServer;

import java.util.ArrayList;
import java.util.List;

public final class DevSnapshotService {
    private DevSnapshotService() {
    }

    public static List<String> build(MinecraftServer server) {
        List<String> lines = new ArrayList<>();
        lines.add("Esoteric Ebb CRPG developer snapshot");
        lines.add(NarrativeDataRegistries.summaryLine());
        lines.add("dialogue_sessions(active=" + DialogueService.activeSessionCount() + ")");
        lines.add(NarrativeSavedData.get(server).summaryLine());
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
        lines.add("- " + BlockGroupIndex.summaryLine());
        appendMessages(lines, "Raw JSON messages", NarrativeDataRegistries.all().stream()
                .flatMap(registry -> registry.validationMessages().stream()
                        .map(message -> registry.directory() + ": " + message))
                .toList());
        appendMessages(lines, "Dialogue validation", DialogueRegistry.validationMessages());
        appendMessages(lines, "Attribute validation", AttributeRegistry.validationMessages());
        appendMessages(lines, "Block group validation", BlockGroupIndex.messages());
        return lines;
    }

    private static void appendMessages(List<String> lines, String heading, List<String> messages) {
        lines.add("");
        lines.add(heading + " (" + messages.size() + "):");
        if (messages.isEmpty()) {
            lines.add("- none");
            return;
        }
        messages.stream().limit(24).forEach(message -> lines.add("- " + message));
        if (messages.size() > 24) {
            lines.add("- ... " + (messages.size() - 24) + " more");
        }
    }
}
