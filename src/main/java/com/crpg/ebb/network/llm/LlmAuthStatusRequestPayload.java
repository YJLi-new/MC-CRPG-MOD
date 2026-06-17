package com.crpg.ebb.network.llm;

import com.crpg.ebb.EbbMod;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

public record LlmAuthStatusRequestPayload(String authSessionId) implements CustomPacketPayload {
    public static final Type<LlmAuthStatusRequestPayload> TYPE = new Type<>(EbbMod.id("llm/auth_status_request"));
    public static final StreamCodec<RegistryFriendlyByteBuf, LlmAuthStatusRequestPayload> CODEC = StreamCodec.ofMember(
            LlmAuthStatusRequestPayload::write,
            LlmAuthStatusRequestPayload::read
    );

    public LlmAuthStatusRequestPayload {
        authSessionId = authSessionId == null ? "" : authSessionId;
    }

    private void write(RegistryFriendlyByteBuf buffer) {
        buffer.writeUtf(authSessionId, LlmPayloadCodecs.MAX_REASON_LENGTH);
    }

    private static LlmAuthStatusRequestPayload read(RegistryFriendlyByteBuf buffer) {
        return new LlmAuthStatusRequestPayload(buffer.readUtf(LlmPayloadCodecs.MAX_REASON_LENGTH));
    }

    @Override
    public Type<LlmAuthStatusRequestPayload> type() {
        return TYPE;
    }
}
