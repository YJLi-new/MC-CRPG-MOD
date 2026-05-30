package com.crpg.ebb.network.dialogue;

import com.crpg.ebb.EbbMod;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public record DialogueUpdatePayload(
        UUID conversationId,
        Identifier dialogueId,
        String nodeId,
        String speaker,
        String text,
        Optional<String> textKey,
        List<VisibleDialogueChoice> choices,
        Optional<RollResultPayload> rollResult,
        Optional<String> statusMessage
) implements CustomPacketPayload {
    public static final Type<DialogueUpdatePayload> TYPE = new Type<>(EbbMod.id("dialogue/update"));
    public static final StreamCodec<RegistryFriendlyByteBuf, DialogueUpdatePayload> CODEC = StreamCodec.ofMember(
            DialogueUpdatePayload::write,
            DialogueUpdatePayload::read
    );

    public DialogueUpdatePayload {
        textKey = textKey == null ? Optional.empty() : textKey;
        choices = List.copyOf(choices);
        rollResult = rollResult == null ? Optional.empty() : rollResult;
        statusMessage = statusMessage == null ? Optional.empty() : statusMessage;
    }

    private void write(RegistryFriendlyByteBuf buffer) {
        buffer.writeUUID(conversationId);
        buffer.writeIdentifier(dialogueId);
        buffer.writeUtf(nodeId, DialoguePayloadCodecs.MAX_NODE_ID_LENGTH);
        buffer.writeUtf(speaker, DialoguePayloadCodecs.MAX_SPEAKER_LENGTH);
        buffer.writeUtf(text, DialoguePayloadCodecs.MAX_TEXT_LENGTH);
        DialoguePayloadCodecs.writeOptionalUtf(buffer, textKey, DialoguePayloadCodecs.MAX_TEXT_KEY_LENGTH);
        DialoguePayloadCodecs.writeChoices(buffer, choices);
        buffer.writeBoolean(rollResult.isPresent());
        rollResult.ifPresent(result -> result.write(buffer));
        DialoguePayloadCodecs.writeOptionalUtf(buffer, statusMessage, DialoguePayloadCodecs.MAX_TEXT_LENGTH);
    }

    private static DialogueUpdatePayload read(RegistryFriendlyByteBuf buffer) {
        UUID conversationId = buffer.readUUID();
        Identifier dialogueId = buffer.readIdentifier();
        String nodeId = buffer.readUtf(DialoguePayloadCodecs.MAX_NODE_ID_LENGTH);
        String speaker = buffer.readUtf(DialoguePayloadCodecs.MAX_SPEAKER_LENGTH);
        String text = buffer.readUtf(DialoguePayloadCodecs.MAX_TEXT_LENGTH);
        Optional<String> textKey = DialoguePayloadCodecs.readOptionalUtf(buffer, DialoguePayloadCodecs.MAX_TEXT_KEY_LENGTH);
        List<VisibleDialogueChoice> choices = DialoguePayloadCodecs.readChoices(buffer);
        Optional<RollResultPayload> rollResult = buffer.readBoolean()
                ? Optional.of(RollResultPayload.read(buffer))
                : Optional.empty();
        Optional<String> statusMessage = DialoguePayloadCodecs.readOptionalUtf(buffer, DialoguePayloadCodecs.MAX_TEXT_LENGTH);
        return new DialogueUpdatePayload(conversationId, dialogueId, nodeId, speaker, text, textKey, choices, rollResult, statusMessage);
    }

    @Override
    public Type<DialogueUpdatePayload> type() {
        return TYPE;
    }
}
