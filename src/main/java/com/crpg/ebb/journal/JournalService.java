package com.crpg.ebb.journal;

import com.crpg.ebb.state.NarrativeSavedData;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

public final class JournalService {
    private JournalService() {
    }

    public static Optional<String> addEntry(NarrativeSavedData state, java.util.UUID playerUuid, String rawId) {
        Identifier id = parseIdentifier(rawId);
        boolean unlocked = state.unlockJournalEntry(playerUuid, id.toString());
        JournalEntryDefinition definition = JournalEntryRegistry.byId(id).orElse(null);
        String prefix = definition != null && definition.category() == JournalEntryCategory.CLUE ? "clue_gained:" : "journal_entry_added:";
        return Optional.of((unlocked ? prefix : prefix.replace("_added", "_already_added")) + id);
    }

    public static List<String> build(ServerPlayer player) {
        NarrativeSavedData state = NarrativeSavedData.get((ServerLevel) player.level());
        List<String> lines = new ArrayList<>();
        lines.add("Esoteric Ebb Journal");
        lines.add("player=" + player.getName().getString() + " uuid=" + player.getUUID());
        lines.add(JournalEntryRegistry.summaryLine());
        lines.add("unlocked_entries=" + state.journalEntryIds(player.getUUID()).size());
        lines.add("");
        for (JournalEntryCategory category : JournalEntryCategory.values()) {
            lines.add(category.serializedName() + ":");
            List<JournalEntryDefinition> entries = JournalEntryRegistry.orderedDefinitions().stream()
                    .filter(definition -> definition.category() == category)
                    .filter(definition -> state.hasJournalEntry(player.getUUID(), definition.id().toString()))
                    .sorted(Comparator.comparing(definition -> definition.id().toString()))
                    .toList();
            if (entries.isEmpty()) {
                lines.add("- none");
                continue;
            }
            for (JournalEntryDefinition entry : entries) {
                lines.add("- [category=" + entry.category().serializedName() + "] " + entry.title() + " (" + entry.id() + ")");
                entry.quest().ifPresent(quest -> lines.add("  quest=" + quest));
                lines.add("  " + entry.text());
            }
            lines.add("");
        }
        return List.copyOf(lines);
    }

    public static Identifier parseIdentifier(String raw) {
        return raw.contains(":") ? Identifier.parse(raw) : Identifier.fromNamespaceAndPath("ebb", raw);
    }
}
