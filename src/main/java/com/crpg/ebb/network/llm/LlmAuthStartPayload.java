package com.crpg.ebb.network.llm;

import com.crpg.ebb.EbbMod;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

import java.util.UUID;

public record LlmAuthStartPayload(UUID requestId) implements CustomPacketPayload {
    public static final Type<LlmAuthStartPayload> TYPE = new Type<>(EbbMod.id("llm/auth_start"));
    public static final StreamCodec<RegistryFriendlyByteBuf, LlmAuthStartPayload> CODEC = StreamCodec.ofMember(
            LlmAuthStartPayload::write,
            LlmAuthStartPayload::read
    );

    public LlmAuthStartPayload {
        requestId = requestId == null ? UUID.randomUUID() : requestId;
    }

    private void write(RegistryFriendlyByteBuf buffer) {
        buffer.writeUUID(requestId);
    }

    private static LlmAuthStartPayload read(RegistryFriendlyByteBuf buffer) {
        return new LlmAuthStartPayload(buffer.readUUID());
    }

    @Override
    public Type<LlmAuthStartPayload> type() {
        return TYPE;
    }
}
