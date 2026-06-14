package com.crpg.ebb.network.llm;

import io.netty.handler.codec.DecoderException;
import net.minecraft.network.RegistryFriendlyByteBuf;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public final class LlmPayloadCodecs {
    public static final int MAX_REASON_LENGTH = 96;
    public static final int MAX_NPC_KEY_LENGTH = 160;
    public static final int MAX_DISPLAY_NAME_LENGTH = 96;
    public static final int MAX_TOPIC_LENGTH = 256;
    public static final int MAX_MESSAGE_LENGTH = 2048;
    public static final int MAX_REPLY_LENGTH = 4096;
    public static final int MAX_OPTION_LENGTH = 160;
    public static final int MAX_OPTIONS = 8;
    public static final int MAX_CITATION_LENGTH = 160;
    public static final int MAX_CITATIONS = 16;

    private LlmPayloadCodecs() {
    }

    public static void writeOptionalUtf(RegistryFriendlyByteBuf buffer, Optional<String> value, int maxLength) {
        buffer.writeBoolean(value != null && value.isPresent());
        if (value != null) {
            value.ifPresent(text -> buffer.writeUtf(text, maxLength));
        }
    }

    public static Optional<String> readOptionalUtf(RegistryFriendlyByteBuf buffer, int maxLength) {
        return buffer.readBoolean() ? Optional.of(buffer.readUtf(maxLength)) : Optional.empty();
    }

    public static void writeStringList(RegistryFriendlyByteBuf buffer, List<String> values, int maxCount, int maxLength) {
        List<String> safe = values == null ? List.of() : values;
        int count = Math.min(safe.size(), maxCount);
        buffer.writeVarInt(count);
        for (int i = 0; i < count; i++) {
            buffer.writeUtf(safe.get(i), maxLength);
        }
    }

    public static List<String> readStringList(RegistryFriendlyByteBuf buffer, int maxCount, int maxLength) {
        int count = buffer.readVarInt();
        if (count < 0 || count > maxCount) {
            throw new DecoderException("Too many LLM strings: " + count);
        }
        List<String> values = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            values.add(buffer.readUtf(maxLength));
        }
        return List.copyOf(values);
    }
}
