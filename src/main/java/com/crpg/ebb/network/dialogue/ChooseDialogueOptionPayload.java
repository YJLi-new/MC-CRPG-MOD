package com.crpg.ebb.network.dialogue;

import com.crpg.ebb.EbbMod;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

import java.util.UUID;

public record ChooseDialogueOptionPayload(
        UUID conversationId,
        String choiceId
) implements CustomPacketPayload {
    public static final Type<ChooseDialogueOptionPayload> TYPE = new Type<>(EbbMod.id("dialogue/choose"));
    public static final StreamCodec<RegistryFriendlyByteBuf, ChooseDialogueOptionPayload> CODEC = StreamCodec.ofMember(
            ChooseDialogueOptionPayload::write,
            ChooseDialogueOptionPayload::read
    );

    private void write(RegistryFriendlyByteBuf buffer) {
        buffer.writeUUID(conversationId);
        buffer.writeUtf(choiceId, VisibleDialogueChoice.MAX_ID_LENGTH);
    }

    private static ChooseDialogueOptionPayload read(RegistryFriendlyByteBuf buffer) {
        return new ChooseDialogueOptionPayload(buffer.readUUID(), buffer.readUtf(VisibleDialogueChoice.MAX_ID_LENGTH));
    }

    @Override
    public Type<ChooseDialogueOptionPayload> type() {
        return TYPE;
    }
}
