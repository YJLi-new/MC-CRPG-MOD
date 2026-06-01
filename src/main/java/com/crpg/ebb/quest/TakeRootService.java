package com.crpg.ebb.quest;

import com.crpg.ebb.dialogue.DialogueEffect;
import com.crpg.ebb.feat.FeatDefinition;
import com.crpg.ebb.feat.FeatRegistry;
import com.crpg.ebb.state.NarrativeSavedData;
import net.minecraft.resources.Identifier;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public final class TakeRootService {
    private TakeRootService() {
    }

    public static Optional<String> startBranch(NarrativeSavedData state, UUID playerUuid, String rawQuestId) {
        Identifier questId = parseIdentifier(rawQuestId);
        String current = state.getQuestState(playerUuid, questId.toString());
        if ("not_started".equals(current)) {
            state.setQuestState(playerUuid, questId.toString(), "active");
            return Optional.of("quest_started:" + questId);
        }
        return Optional.of("quest_already_" + current + ":" + questId);
    }

    public static Optional<String> completeBranch(NarrativeSavedData state, UUID playerUuid, String rawQuestId) {
        Identifier questId = parseIdentifier(rawQuestId);
        QuestBranchDefinition definition = QuestBranchRegistry.byId(questId).orElse(null);
        if (definition == null) {
            state.setQuestState(playerUuid, questId.toString(), "completed");
            return Optional.of("quest_completed_unknown:" + questId);
        }

        String current = state.getQuestState(playerUuid, questId.toString());
        if ("take_rooted".equals(current)) {
            return Optional.of("quest_already_take_rooted:" + questId);
        }

        List<String> messages = new ArrayList<>();
        state.setQuestState(playerUuid, questId.toString(), "completed");
        messages.add("quest_completed:" + questId);

        if (definition.kind() == QuestBranchKind.MAJOR) {
            for (DialogueEffect effect : definition.takeRootEffects()) {
                effect.apply(state, playerUuid).ifPresent(messages::add);
            }
            for (Identifier featId : definition.grantFeats()) {
                boolean unlocked = state.unlockFeat(playerUuid, featId.toString());
                FeatDefinition feat = FeatRegistry.byId(featId).orElse(null);
                if (feat != null && feat.activeSlotCandidate()) {
                    state.activateFeat(playerUuid, featId.toString());
                }
                messages.add((unlocked ? "feat_unlocked:" : "feat_already_unlocked:") + featId);
            }
            state.setQuestState(playerUuid, questId.toString(), "take_rooted");
            String text = definition.takeRootText().isBlank()
                    ? "Take-root complete: " + definition.title()
                    : definition.takeRootText();
            messages.add("take_root: " + text);
        }
        return Optional.of(String.join(", ", messages));
    }

    public static Optional<String> unlockFeat(NarrativeSavedData state, UUID playerUuid, String rawFeatId) {
        Identifier featId = parseIdentifier(rawFeatId);
        boolean unlocked = state.unlockFeat(playerUuid, featId.toString());
        return Optional.of((unlocked ? "feat_unlocked:" : "feat_already_unlocked:") + featId);
    }

    public static Optional<String> activateFeat(NarrativeSavedData state, UUID playerUuid, String rawFeatId) {
        Identifier featId = parseIdentifier(rawFeatId);
        NarrativeSavedData.FeatActivationResult result = state.activateFeat(playerUuid, featId.toString());
        return Optional.of("feat_activate:" + featId + ":" + result.reason());
    }

    public static Identifier parseIdentifier(String raw) {
        return raw.contains(":") ? Identifier.parse(raw) : Identifier.fromNamespaceAndPath("ebb", raw);
    }
}
