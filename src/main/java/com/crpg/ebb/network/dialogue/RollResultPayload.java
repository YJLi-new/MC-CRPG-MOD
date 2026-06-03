package com.crpg.ebb.network.dialogue;

import net.minecraft.network.RegistryFriendlyByteBuf;

public record RollResultPayload(
        String attribute,
        int dc,
        int dieRoll,
        int attributeScore,
        int total,
        boolean success,
        boolean critical,
        String outcome,
        boolean showDc,
        boolean showRoll
) {
    public static final int MAX_ATTRIBUTE_LENGTH = 64;
    public static final int MAX_OUTCOME_LENGTH = 64;

    public void write(RegistryFriendlyByteBuf buffer) {
        buffer.writeUtf(attribute, MAX_ATTRIBUTE_LENGTH);
        buffer.writeVarInt(dc);
        buffer.writeVarInt(dieRoll);
        buffer.writeVarInt(attributeScore);
        buffer.writeVarInt(total);
        buffer.writeBoolean(success);
        buffer.writeBoolean(critical);
        buffer.writeUtf(outcome, MAX_OUTCOME_LENGTH);
        buffer.writeBoolean(showDc);
        buffer.writeBoolean(showRoll);
    }

    public static RollResultPayload read(RegistryFriendlyByteBuf buffer) {
        return new RollResultPayload(
                buffer.readUtf(MAX_ATTRIBUTE_LENGTH),
                buffer.readVarInt(),
                buffer.readVarInt(),
                buffer.readVarInt(),
                buffer.readVarInt(),
                buffer.readBoolean(),
                buffer.readBoolean(),
                buffer.readUtf(MAX_OUTCOME_LENGTH),
                buffer.readBoolean(),
                buffer.readBoolean()
        );
    }

    public String summary() {
        String roll = showRoll
                ? "d20=" + dieRoll + " + " + attributeScore + " => " + total
                : "hidden roll";
        String difficulty = showDc ? "DC " + dc : "hidden DC";
        return attribute + " " + roll + " vs " + difficulty + " (" + outcome + ")";
    }
}
