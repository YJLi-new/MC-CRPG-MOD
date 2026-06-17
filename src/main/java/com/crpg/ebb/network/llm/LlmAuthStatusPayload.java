package com.crpg.ebb.network.llm;

import com.crpg.ebb.EbbMod;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

public record LlmAuthStatusPayload(
        String status,
        String redactedSummary,
        long intervalSeconds,
        String error
) implements CustomPacketPayload {
    public static final Type<LlmAuthStatusPayload> TYPE = new Type<>(EbbMod.id("llm/auth_status"));
    public static final StreamCodec<RegistryFriendlyByteBuf, LlmAuthStatusPayload> CODEC = StreamCodec.ofMember(
            LlmAuthStatusPayload::write,
            LlmAuthStatusPayload::read
    );

    public LlmAuthStatusPayload {
        status = status == null ? "" : status;
        redactedSummary = redactedSummary == null ? "" : redactedSummary;
        intervalSeconds = Math.max(0L, intervalSeconds);
        error = error == null ? "" : error;
    }

    private void write(RegistryFriendlyByteBuf buffer) {
        buffer.writeUtf(status, LlmPayloadCodecs.MAX_REASON_LENGTH);
        buffer.writeUtf(redactedSummary, 512);
        buffer.writeVarLong(intervalSeconds);
        buffer.writeUtf(error, LlmPayloadCodecs.MAX_REASON_LENGTH);
    }

    private static LlmAuthStatusPayload read(RegistryFriendlyByteBuf buffer) {
        return new LlmAuthStatusPayload(
                buffer.readUtf(LlmPayloadCodecs.MAX_REASON_LENGTH),
                buffer.readUtf(512),
                buffer.readVarLong(),
                buffer.readUtf(LlmPayloadCodecs.MAX_REASON_LENGTH)
        );
    }

    @Override
    public Type<LlmAuthStatusPayload> type() {
        return TYPE;
    }
}
