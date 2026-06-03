package com.crpg.ebb.network.dialogue;

import com.crpg.ebb.dialogue.ChoiceType;
import com.crpg.ebb.dialogue.DialogueChoice;
import net.minecraft.network.RegistryFriendlyByteBuf;

import java.util.Optional;

public record VisibleDialogueChoice(
        String id,
        ChoiceType type,
        String text,
        Optional<String> textKey,
        Optional<String> checkSummary
) {
    public static final int MAX_ID_LENGTH = 64;
    public static final int MAX_TEXT_LENGTH = 512;
    public static final int MAX_CHECK_SUMMARY_LENGTH = 96;

    public VisibleDialogueChoice {
        textKey = textKey == null ? Optional.empty() : textKey;
        checkSummary = checkSummary == null ? Optional.empty() : checkSummary;
    }

    public static VisibleDialogueChoice fromChoice(DialogueChoice choice) {
        Optional<String> check = choice.check().map(c -> {
            String dc = c.showDc() ? "DC " + c.dc() : "DC ?";
            String die = c.showRoll() ? c.die() : "hidden roll";
            return c.attribute() + " " + dc + " " + die;
        });
        return new VisibleDialogueChoice(choice.id(), choice.type(), choice.text(), choice.textKey(), check);
    }

    public void write(RegistryFriendlyByteBuf buffer) {
        buffer.writeUtf(id, MAX_ID_LENGTH);
        buffer.writeEnum(type);
        buffer.writeUtf(text, MAX_TEXT_LENGTH);
        DialoguePayloadCodecs.writeOptionalUtf(buffer, textKey, DialoguePayloadCodecs.MAX_TEXT_KEY_LENGTH);
        buffer.writeBoolean(checkSummary.isPresent());
        checkSummary.ifPresent(summary -> buffer.writeUtf(summary, MAX_CHECK_SUMMARY_LENGTH));
    }

    public static VisibleDialogueChoice read(RegistryFriendlyByteBuf buffer) {
        String id = buffer.readUtf(MAX_ID_LENGTH);
        ChoiceType type = buffer.readEnum(ChoiceType.class);
        String text = buffer.readUtf(MAX_TEXT_LENGTH);
        Optional<String> textKey = DialoguePayloadCodecs.readOptionalUtf(buffer, DialoguePayloadCodecs.MAX_TEXT_KEY_LENGTH);
        Optional<String> checkSummary = buffer.readBoolean()
                ? Optional.of(buffer.readUtf(MAX_CHECK_SUMMARY_LENGTH))
                : Optional.empty();
        return new VisibleDialogueChoice(id, type, text, textKey, checkSummary);
    }
}
