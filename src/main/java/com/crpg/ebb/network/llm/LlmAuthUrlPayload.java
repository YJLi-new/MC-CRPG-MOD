package com.crpg.ebb.network.llm;

import com.crpg.ebb.EbbMod;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

public record LlmAuthUrlPayload(
        String authSessionId,
        String verificationUrl,
        String userCode,
        String provider,
        long intervalSeconds
) implements CustomPacketPayload {
    public static final Type<LlmAuthUrlPayload> TYPE = new Type<>(EbbMod.id("llm/auth_url"));
    public static final StreamCodec<RegistryFriendlyByteBuf, LlmAuthUrlPayload> CODEC = StreamCodec.ofMember(
            LlmAuthUrlPayload::write,
            LlmAuthUrlPayload::read
    );

    public LlmAuthUrlPayload {
        authSessionId = authSessionId == null ? "" : authSessionId;
        verificationUrl = verificationUrl == null ? "" : verificationUrl;
        userCode = userCode == null ? "" : userCode;
        provider = provider == null ? "" : provider;
        intervalSeconds = Math.max(1L, intervalSeconds);
    }

    private void write(RegistryFriendlyByteBuf buffer) {
        buffer.writeUtf(authSessionId, LlmPayloadCodecs.MAX_REASON_LENGTH);
        buffer.writeUtf(verificationUrl, 1024);
        buffer.writeUtf(userCode, 32);
        buffer.writeUtf(provider, 64);
        buffer.writeVarLong(intervalSeconds);
    }

    private static LlmAuthUrlPayload read(RegistryFriendlyByteBuf buffer) {
        return new LlmAuthUrlPayload(
                buffer.readUtf(LlmPayloadCodecs.MAX_REASON_LENGTH),
                buffer.readUtf(1024),
                buffer.readUtf(32),
                buffer.readUtf(64),
                buffer.readVarLong()
        );
    }

    @Override
    public Type<LlmAuthUrlPayload> type() {
        return TYPE;
    }
}
