package com.crpg.ebb.network;

import com.crpg.ebb.EbbMod;
import com.crpg.ebb.network.dialogue.DialoguePayloadCodecs;
import com.crpg.ebb.network.dialogue.VisibleDialogueChoice;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public record OpenDialoguePayload(
        UUID conversationId,
        Identifier dialogueId,
        String nodeId,
        String speaker,
        String text,
        Optional<String> textKey,
        List<VisibleDialogueChoice> choices,
        Optional<String> statusMessage
) implements CustomPacketPayload {
    public static final Type<OpenDialoguePayload> TYPE = new Type<>(EbbMod.id("interaction/open_dialogue"));
    public static final StreamCodec<RegistryFriendlyByteBuf, OpenDialoguePayload> CODEC = StreamCodec.ofMember(
            OpenDialoguePayload::write,
            OpenDialoguePayload::read
    );

    public OpenDialoguePayload {
        textKey = textKey == null ? Optional.empty() : textKey;
        choices = List.copyOf(choices);
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
        DialoguePayloadCodecs.writeOptionalUtf(buffer, statusMessage, DialoguePayloadCodecs.MAX_TEXT_LENGTH);
    }

    private static OpenDialoguePayload read(RegistryFriendlyByteBuf buffer) {
        return new OpenDialoguePayload(
                buffer.readUUID(),
                buffer.readIdentifier(),
                buffer.readUtf(DialoguePayloadCodecs.MAX_NODE_ID_LENGTH),
                buffer.readUtf(DialoguePayloadCodecs.MAX_SPEAKER_LENGTH),
                buffer.readUtf(DialoguePayloadCodecs.MAX_TEXT_LENGTH),
                DialoguePayloadCodecs.readOptionalUtf(buffer, DialoguePayloadCodecs.MAX_TEXT_KEY_LENGTH),
                DialoguePayloadCodecs.readChoices(buffer),
                DialoguePayloadCodecs.readOptionalUtf(buffer, DialoguePayloadCodecs.MAX_TEXT_LENGTH)
        );
    }

    @Override
    public Type<OpenDialoguePayload> type() {
        return TYPE;
    }
}
