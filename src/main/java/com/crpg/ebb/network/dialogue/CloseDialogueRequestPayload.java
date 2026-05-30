package com.crpg.ebb.network.dialogue;

import com.crpg.ebb.EbbMod;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

import java.util.UUID;

public record CloseDialogueRequestPayload(
        UUID conversationId,
        String reason
) implements CustomPacketPayload {
    public static final Type<CloseDialogueRequestPayload> TYPE = new Type<>(EbbMod.id("dialogue/close_request"));
    public static final StreamCodec<RegistryFriendlyByteBuf, CloseDialogueRequestPayload> CODEC = StreamCodec.ofMember(
            CloseDialogueRequestPayload::write,
            CloseDialogueRequestPayload::read
    );

    private void write(RegistryFriendlyByteBuf buffer) {
        buffer.writeUUID(conversationId);
        buffer.writeUtf(reason, DialoguePayloadCodecs.MAX_REASON_LENGTH);
    }

    private static CloseDialogueRequestPayload read(RegistryFriendlyByteBuf buffer) {
        return new CloseDialogueRequestPayload(buffer.readUUID(), buffer.readUtf(DialoguePayloadCodecs.MAX_REASON_LENGTH));
    }

    @Override
    public Type<CloseDialogueRequestPayload> type() {
        return TYPE;
    }
}
