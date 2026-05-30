package com.crpg.ebb.network;

import com.crpg.ebb.EbbMod;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

public record InteractionDeniedPayload(
        Identifier targetId,
        String reason
) implements CustomPacketPayload {
    public static final int MAX_REASON_LENGTH = 96;
    public static final Type<InteractionDeniedPayload> TYPE = new Type<>(EbbMod.id("interaction/denied"));
    public static final StreamCodec<RegistryFriendlyByteBuf, InteractionDeniedPayload> CODEC = StreamCodec.ofMember(
            InteractionDeniedPayload::write,
            InteractionDeniedPayload::read
    );

    private void write(RegistryFriendlyByteBuf buffer) {
        buffer.writeIdentifier(targetId);
        buffer.writeUtf(reason, MAX_REASON_LENGTH);
    }

    private static InteractionDeniedPayload read(RegistryFriendlyByteBuf buffer) {
        return new InteractionDeniedPayload(buffer.readIdentifier(), buffer.readUtf(MAX_REASON_LENGTH));
    }

    @Override
    public Type<InteractionDeniedPayload> type() {
        return TYPE;
    }
}
