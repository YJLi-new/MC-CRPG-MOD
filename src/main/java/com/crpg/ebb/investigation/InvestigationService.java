package com.crpg.ebb.investigation;

import com.crpg.ebb.journal.JournalService;
import com.crpg.ebb.state.NarrativeSavedData;
import net.minecraft.resources.Identifier;

import java.util.Optional;
import java.util.UUID;

public final class InvestigationService {
    private InvestigationService() {
    }

    public static Optional<String> revealClue(NarrativeSavedData state, UUID playerUuid, String rawId) {
        Identifier id = parseIdentifier(rawId);
        boolean added = state.revealClue(playerUuid, id.toString());
        Optional<ClueDefinition> definition = InvestigationRegistry.clue(id);
        definition.flatMap(ClueDefinition::journalEntry)
                .ifPresent(journal -> JournalService.addEntry(state, playerUuid, journal.toString()));
        String prefix = added ? "clue_found:" : "clue_already_found:";
        return Optional.of(prefix + id + definition.map(clue -> " \"" + clue.title() + "\"").orElse(""));
    }

    private static Identifier parseIdentifier(String raw) {
        return raw.contains(":") ? Identifier.parse(raw) : Identifier.fromNamespaceAndPath("ebb", raw);
    }
}
