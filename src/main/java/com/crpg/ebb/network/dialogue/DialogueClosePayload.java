package com.crpg.ebb.network.dialogue;

import com.crpg.ebb.EbbMod;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

import java.util.UUID;

public record DialogueClosePayload(
        UUID conversationId,
        String reason
) implements CustomPacketPayload {
    public static final Type<DialogueClosePayload> TYPE = new Type<>(EbbMod.id("dialogue/close"));
    public static final StreamCodec<RegistryFriendlyByteBuf, DialogueClosePayload> CODEC = StreamCodec.ofMember(
            DialogueClosePayload::write,
            DialogueClosePayload::read
    );

    private void write(RegistryFriendlyByteBuf buffer) {
        buffer.writeUUID(conversationId);
        buffer.writeUtf(reason, DialoguePayloadCodecs.MAX_REASON_LENGTH);
    }

    private static DialogueClosePayload read(RegistryFriendlyByteBuf buffer) {
        return new DialogueClosePayload(buffer.readUUID(), buffer.readUtf(DialoguePayloadCodecs.MAX_REASON_LENGTH));
    }

    @Override
    public Type<DialogueClosePayload> type() {
        return TYPE;
    }
}
